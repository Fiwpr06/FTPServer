package com.syncvault.shared.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

// Tiện ích mạng và định dạng địa chỉ IP cho giao thức FTP
public final class NetworkUtil {
    private NetworkUtil() {}

    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress() && addr.getHostAddress().indexOf(':') == -1) {
                        return addr.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    // Định dạng chuỗi PASV (h1,h2,h3,h4,p1,p2)
    // Ví dụ: IP 192.168.1.15, Port 30005 -> 192,168,1,15,117,53 (vì 117 * 256 + 53 = 30005)
    public static String formatPasvAddress(String ip, int port) {
        String formattedIp = ip.replace('.', ',');
        int p1 = (port >> 8) & 0xFF;
        int p2 = port & 0xFF;
        return String.format("(%s,%d,%d)", formattedIp, p1, p2);
    }
}
