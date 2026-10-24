package com.syncvault.server.core.channel;

import com.syncvault.shared.constant.SystemConfig;
import com.syncvault.shared.util.NetworkUtil;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

// Quản lý kênh truyền dữ liệu thụ động (Passive Mode Data Channel)
public class PassiveDataChannel implements Closeable {
    private static final Logger LOGGER = Logger.getLogger(PassiveDataChannel.class.getName());

    private ServerSocket serverSocket;
    private Socket dataSocket;
    private final String boundIp;
    private int boundPort;

    public PassiveDataChannel() throws IOException {
        this.boundIp = NetworkUtil.getLocalIpAddress();
        this.serverSocket = openServerSocket();
        this.boundPort = serverSocket.getLocalPort();
        this.serverSocket.setSoTimeout(20000); // 20s timeout chờ Client kết nối vào kênh Data
    }

    private ServerSocket openServerSocket() {
        for (int p = SystemConfig.PASV_PORT_MIN; p <= SystemConfig.PASV_PORT_MAX; p++) {
            try {
                return new ServerSocket(p);
            } catch (IOException ignored) {}
        }
        try {
            return new ServerSocket(0); // Cổng ngẫu nhiên nếu dải cố định bị bận
        } catch (IOException e) {
            throw new RuntimeException("Không thể mở ServerSocket cho Passive Mode", e);
        }
    }

    public String getPasvReplyString() {
        return NetworkUtil.formatPasvAddress(boundIp, boundPort);
    }

    public synchronized Socket acceptDataSocket() throws IOException {
        if (dataSocket != null && !dataSocket.isClosed()) {
            return dataSocket;
        }
        if (serverSocket == null || serverSocket.isClosed()) {
            throw new IOException("Data ServerSocket đã đóng");
        }
        this.dataSocket = serverSocket.accept();
        return this.dataSocket;
    }

    @Override
    public synchronized void close() {
        try {
            if (dataSocket != null && !dataSocket.isClosed()) {
                dataSocket.close();
            }
        } catch (Exception ignored) {}

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}
    }
}
