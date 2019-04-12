package io.linzhehuang.drcomutil;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

public class PacketUtil {
	/**
	 * 
	 * @param host IP address of authentication server
	 * @param port port of authentication server
	 * @param time time of sending challenge packet
	 * @return
	 */
	public static DatagramPacket generateChallengePack(String host, int port, int time) {
		byte[] buf = {0x01, (byte)(0x02 + time), getRandomByte(), getRandomByte(), 0x22,
                0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00};
		DatagramPacket ret = null;
		try {
			ret =  new DatagramPacket(buf, buf.length, InetAddress.getByName(host), port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public static DatagramPacket generateChallengeResponsePack() {
		byte[] buf = new byte[76];
		return new DatagramPacket(buf, buf.length);
	}
	
	/**
	 * 
	 * @param packet challenge response packet
	 * @param salt 4 bytes output argument 
	 * @param clientHost 4 bytes output argument
	 * @return
	 */
	public static boolean getChallengeResponsePackInfo(DatagramPacket packet, byte[] salt, byte[] clientHost) {
		byte[] buf = packet.getData();
		if (buf[0] != 0x02) return false; // not a challenge response packet
		else {
			System.arraycopy(buf, 4, salt, 0, 4);  // [4:8]
            System.arraycopy(buf, 20, clientHost, 0, 4);  // [20:24]
		}
		return true;
	}
	
	/**
	 *  The login packet is complex, there is not a standard format. Follow format works worse than
     *  than python version. But I don't know the reason.
	 * @param conf
	 * @param host
	 * @param port
	 * @param salt
	 * @param clientHost
	 * @param md5a 16 bytes output argument
	 * @return
	 */
	public static DatagramPacket generateLoginPacket(HostConf conf, String host, int port, byte[] salt, byte[] clientHost, byte[] md5a) {
		byte code = 0x03;
        byte type = 0x01;
        byte EOF = 0x00;
        byte controlCheck = 0x20;
        byte adapterNum = 0x04; // Windows :07 gdufe 04
        byte ipDog = 0x01;
        byte[] primaryDNS = {(byte) 0xca, 0x60, (byte) 0x80, (byte) 0xa6};   //每台机子对应的dns和dhcp服务器，可以填0
        byte[] dhcp = {(byte) 0xde, (byte) 0xca, (byte) 0xab, 0x21};//同上
        byte[] md5b;

        int dataLen = 330;
        byte[] data = new byte[dataLen];

        data[0] = code;
        data[1] = type;
        data[2] = EOF;
        data[3] = (byte) (conf.getUser().length() + 20);

        System.arraycopy(MD5Util.md5(new byte[]{code, type}, salt, conf.getPassword().getBytes()),
                0, md5a, 0, 16);//md5a保存起来
        System.arraycopy(md5a, 0, data, 4, md5a.length);//md5a 4+16=20

        byte[] user = ljust(conf.getUser().getBytes(), 36);
        System.arraycopy(user, 0, data, 20, user.length);//username 20+36=56

        data[56] = controlCheck;//0x20
        data[57] = adapterNum;

        //md5a[0:6] xor mac
        System.arraycopy(md5a, 0, data, 58, 6);
        byte[] macBytes = conf.getMac();
        for (int i = 0; i < macBytes.length; i++) { //macBytes.length=6
            data[i + 58] ^= macBytes[i];//md5a oxr mac
        }// xor 58+6=64
        md5b = MD5Util.md5(new byte[]{0x01}, conf.getPassword().getBytes(), salt, new byte[]{0x00, 0x00, 0x00, 0x00});
        System.arraycopy(md5b, 0, data, 64, md5b.length);//md5b 64+16=80

        data[80] = 0x01;//网卡数量，直接填1后面就有12个byte（3个网卡ip）可以直接填0处理了
        System.arraycopy(clientHost, 0, data, 81, clientHost.length);//ip1 81+4=85
        System.arraycopy(new byte[]{
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
        }, 0, data, 85, 12);//ip2/3/4 85+12=97

        data[97] = 0x14;//临时放，97 ~ 97+8 是 md5c[0:8]
        data[98] = 0x00;
        data[99] = 0x07;
        data[100] = 0x0b;
        byte[] tmp = new byte[101];
        System.arraycopy(data, 0, tmp, 0, 101);//前 97 位 和 0x14_00_07_0b
        byte[] md5c = MD5Util.md5(tmp);
        System.arraycopy(md5c, 0, data, 97, 8);//md5c 97+8=105

        data[105] = ipDog;//0x01
        //fill four zero.  106+4=110
        byte[] hostname = ljust(conf.getHostName().getBytes(), 32);
        System.arraycopy(hostname, 0, data, 110, 32);//hostname 110+32=142
        System.arraycopy(primaryDNS, 0, data, 142, 4);//primaryDNS 142+4=146
        System.arraycopy(dhcp, 0, data, 146, 4);//dhcp 146+4=150
        dhcp[3] = 0x1f;
        System.arraycopy(dhcp, 0, data, 150, 4);//dhcp 150+4=154 //second dns 150+4=154 填0就不写了

        //delimiter 154+8=162 填0就不写了

        data[162] = (byte) 0x94;//unknown 162+4=166
        data[166] = 0x06;       //os major 166+4=170
        data[170] = 0x02;       //os minor 170+4=174
        data[174] = (byte) 0xf0;//os build
        data[175] = 0x23;       //os build 174+4=178
        data[178] = 0x02;       //os unknown 178+4=182

        byte[] byteHostOs = ljust(conf.getOsName().getBytes(),32);
        System.arraycopy(byteHostOs, 0, data, 182, byteHostOs.length);//byteHostOs 182+32=214
        //214+96=310 byte zero，实际不全是0
        data[310] = DRCOMConf.AUTH_VERSION[0];
        data[311] = DRCOMConf.AUTH_VERSION[1];
        //这附近相对原本的吉林大学版修改较
        data[312] = 0x02;
        data[313] = 0x0c;

        //这部分开始是 checksum(data+'\x01\x26\x07\x11\x00\x00'+dump(mac)) 占4个byte
        byte[] temp = {0x01,0x26,0x07,0x11,0x00,0x00};
        tmp = new byte[326];//data+'\x01\x26\x07\x11\x00\x00'+dump(mac)
        System.arraycopy(data, 0, tmp, 0, 314);
        System.arraycopy(temp, 0, tmp, 314, temp.length);            //len=6
        System.arraycopy(macBytes, 0, tmp, 320, macBytes.length);    //len(macbyte)=6
        tmp = checksum(tmp);
        System.arraycopy(tmp, 0, data, 314, 4); //取checksum前4个
        //checksum结束，4个byte

        data[318] = 0x00;//0分割，占2个
        data[319] = 0x00;

        //macBytes占6个
        System.arraycopy(macBytes, 0, data, 320, macBytes.length);//320+6=326
        data[326] = 0x00;             //Auto_Logout
        data[327] = 0x00;            //broadcast mode
        data[328] = (byte) 0xc2;    //unknown 可填0
        data[329] = 0x66;           //unknown 可填0
        DatagramPacket ret = null;
        try {
			ret = new DatagramPacket(data, data.length, InetAddress.getByName(host), port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
        return ret;
	}
	/**
	 * 
	 * @param packet login response packet
	 * @param tail1 16 bytes output argument
	 * @return
	 */
	public static boolean getLoginResponsePackInfo(DatagramPacket packet, byte[] tail1) {
		byte[] buf = packet.getData();
		System.arraycopy(buf, 23, tail1, 0, 16);
		return true;
	}
	
	public static DatagramPacket generateLoginResponsePack() {
		byte[] buf = new byte[45];
		return new DatagramPacket(buf, buf.length);
	}
	
	public static boolean isLogged(DatagramPacket packet) {
		byte[] buf = packet.getData();
		return (buf[0] == 0x04);
	}
	
	/**
	 * 
	 * @param packet login response packet
	 * @return 0 - unknown error, 1 - authentication error, 2 - MAC error
	 */
	public static int getLoginErrorCode(DatagramPacket packet) {
		byte[] buf = packet.getData();
		switch (buf[4]) {
			case 0x03:  // sometimes the network is down , it will also get 0x03 code
				return 1;
			case 0x0b:
				return 2;
		}
		return 0;
	}
	
	public static DatagramPacket generateLogoutPack(HostConf conf, String host, int port, byte[] salt, byte[] tail1) {
		byte[] data = new byte[80];
		data[0] = 0x06;//code
		data[1] = 0x01;//type
		data[2] = 0x00;//EOF
		data[3] = (byte) (conf.getUser().length() + 20);
		byte[] md5 = MD5Util.md5(new byte[]{0x06, 0x01}, salt, conf.getPassword().getBytes());
		System.arraycopy(md5, 0, data, 4, md5.length);//md5 4+16=20
		System.arraycopy(ljust(conf.getUser().getBytes(), 36),
		          0, data, 20, 36);//username 20+36=56
		data[56] = 0x20;
		data[57] = 0x05;
		byte[] macBytes = conf.getMac();
		for (int i = 0; i < 6; i++) {
		    data[58 + i] = (byte) (data[4 + i] ^ macBytes[i]);
		}// mac xor md5 58+6=64
		System.arraycopy(tail1, 0, data, 64, tail1.length);//64+16=80
		try {
			return new DatagramPacket(data, data.length, InetAddress.getByName(host), port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static DatagramPacket generateLogoutResponsePack() {
		byte[] buf = new byte[512];
		return new DatagramPacket(buf, buf.length);
	}
	
	public static boolean isLogout(DatagramPacket packet) {
		byte[] buf = packet.getData();
		return (buf[0] == 0x04);
	}
	
	private static byte getRandomByte() {
		Random random = new Random(System.currentTimeMillis());
		return (byte)random.nextInt();
	}
	
	private static byte[] checksum(byte[] data) {
        // 1234 = 0x_00_00_04_d2
        byte[] sum = new byte[]{0x00, 0x00, 0x04, (byte) 0xd2};
        int len = data.length;
        int i = 0;
        //0123_4567_8901_23
        for (; i + 3 < len; i = i + 4) {
            //abcd ^ 3210
            //abcd ^ 7654
            //abcd ^ 1098
            sum[0] ^= data[i + 3];
            sum[1] ^= data[i + 2];
            sum[2] ^= data[i + 1];
            sum[3] ^= data[i];
        }
        if (i < len) {
            //剩下_23
            //i=12,len=14
            byte[] tmp = new byte[4];
            for (int j = 3; j >= 0 && i < len; j--) {
                //j=3 tmp = 0 0 0 2  i=12  13
                //j=2 tmp = 0 0 3 2  i=13  14
                tmp[j] = data[i++];
            }
            for (int j = 0; j < 4; j++) {
                sum[j] ^= tmp[j];
            }
        }
        BigInteger bigInteger = new BigInteger(1, sum);//无符号数即正数
        bigInteger = bigInteger.multiply(BigInteger.valueOf(1968));
        bigInteger = bigInteger.and(BigInteger.valueOf(0xff_ff_ff_ffL));
        byte[] bytes = bigInteger.toByteArray();
        len = bytes.length;
        i = 0;
        byte[] ret = new byte[4];
        for (int j = len - 1; j >= 0 && i < 4; j--) {
            ret[i++] = bytes[j];
        }
        return ret;
    }
	
	private static byte[] ljust(byte[] src, int count) {
        return ljust(src, count, (byte) 0x00);
    }

    private static byte[] ljust(byte[] src, int count, byte fill) {
        int srcLen = src.length;
        byte[] ret = new byte[count];
        if (srcLen >= count) {//只返回前 count 位
            System.arraycopy(src, 0, ret, 0, count);
            return ret;
        }
        System.arraycopy(src, 0, ret, 0, srcLen);
        for (int i = srcLen; i < count; i++) {
            ret[i] = fill;
        }
        return ret;
    }
}
