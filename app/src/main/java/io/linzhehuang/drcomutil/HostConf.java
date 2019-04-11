package io.linzhehuang.drcomutil;

public class HostConf {
	private String user;
	private String password;
	private final String HOST_NAME = "FOO";
	private final String OS_NAME = "WINDOWS";
	private byte[] mac = new byte[6];
	
	public HostConf(String user, String password, String mac) {
		this.user = user;
		this.password = password;
		this.mac = parseMac(mac);
	}
	
	public String getUser() {
		return user;
	}
	public String getPassword() {
		return password;
	}
	public byte[] getMac() {
		return mac;
	}
	public String getHostName() {
		return HOST_NAME;
	}
	public String getOsName() {
		return OS_NAME;
	}
	
	private byte[] parseMac(String mac) {
		// remove '-' or ':'
		mac = (mac.contains("-")) ? mac.replaceAll("-", ""): mac;
		mac = (mac.contains(":")) ? mac.replaceAll(":", ""): mac;
		// how this work ?
		byte[] ret = new byte[6];
		StringBuilder sb = new StringBuilder(18);
        for (int i = 0; i < 12; i++) {
            sb.append(mac.charAt(i++)).append(mac.charAt(i)).append("-");
        }
        String macHexDash = sb.substring(0, 17);

        String[] split = macHexDash.split("-");
        for (int i = 0; i < split.length; i++) {
            ret[i] = (byte) Integer.parseInt(split[i], 16);
        }
        return ret;
	}
}
