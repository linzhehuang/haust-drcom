package io.linzhehuang.drcomutil;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class DRCOMUtil {
	
	public enum DRCOMState {
		SEND_CHALLENGE_ERROR,
		RECV_CHALLENGE_ERROR,
		RECV_CHALLENGE_TIMEOUT_ERROR,
		
		SEND_LOGIN_ERROR,
		RECV_LOGIN_ERROR,
		
		LOGGED,
		MAC_ERROR,
		AUTH_ERROR,
		UNKNOWN_ERROR
	}
	
	private static DatagramSocket client = null;
	private static byte[] tail1 = new byte[16];
	private static byte[] salt = new byte[4];
	
	public static void init() {
        try {
			client = new DatagramSocket();
	        client.setSoTimeout(DRCOMConf.TIMEOUT);
		} catch (SocketException e) {
			e.printStackTrace();
		}
    }
	
	public static DRCOMState login(HostConf conf) {
		DatagramPacket packet = null;
		// send challenge packet
		try {
			client.send(
					PacketUtil.generateChallengePack(DRCOMConf.AUTH_SERVER, DRCOMConf.PORT, 0));
		} catch (IOException e) {
			e.printStackTrace();
			return DRCOMState.SEND_CHALLENGE_ERROR;
		}

		// receive challenge response packet
		packet = PacketUtil.generateChallengeResponsePack();
		try {
			client.receive(packet);
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			return DRCOMState.RECV_CHALLENGE_TIMEOUT_ERROR;
		} catch (IOException e) {
			e.printStackTrace();
			return DRCOMState.RECV_CHALLENGE_ERROR;
		}
		byte[] clientHost = new byte[4];
		PacketUtil.getChallengeResponsePackInfo(packet, salt, clientHost);
		// send login packet
		byte[] md5a = new byte[16];
		try {
			client.send(
					PacketUtil.generateLoginPacket(conf, DRCOMConf.AUTH_SERVER, DRCOMConf.PORT ,salt, clientHost, md5a));
		} catch (IOException e) {
			e.printStackTrace();
			return DRCOMState.SEND_LOGIN_ERROR;
		}
		// receive login response packet
		packet = PacketUtil.generateLoginResponsePack();
		try {
			client.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
			return DRCOMState.RECV_LOGIN_ERROR;
		}
		// parse login response packet
		if (PacketUtil.isLogged(packet)) {
			PacketUtil.getLoginResponsePackInfo(packet, tail1); // save tail1 for logout
			return DRCOMState.LOGGED;
		}
		int errorCode = PacketUtil.getLoginErrorCode(packet);
		if (errorCode == 1) {
			return DRCOMState.AUTH_ERROR;
		} else if (errorCode == 2) {
			return DRCOMState.MAC_ERROR;
		}
		return DRCOMState.UNKNOWN_ERROR;
	}

	// logout will cause unknown error, so don't use it
	public static boolean logout(HostConf conf) {
		try {
			client.send(PacketUtil.generateLogoutPack(conf, DRCOMConf.AUTH_SERVER, DRCOMConf.PORT, salt, tail1));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		DatagramPacket packet = PacketUtil.generateLogoutResponsePack();
		try {
			client.receive(packet);
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return PacketUtil.isLogout(packet);
	}
}
