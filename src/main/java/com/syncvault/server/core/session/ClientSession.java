package com.syncvault.server.core.session;

import com.syncvault.server.ServerContext;
import com.syncvault.server.core.channel.PassiveDataChannel;
import com.syncvault.server.db.model.UserRecord;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

// Quản lý trạng thái và kênh giao tiếp của một kết nối Client cụ thể
public class ClientSession implements Closeable {
    private static final Logger LOGGER = Logger.getLogger(ClientSession.class.getName());

    private final Socket controlSocket;
    private final ServerContext context;
    private BufferedReader reader;
    private BufferedWriter writer;

    private SessionState state = SessionState.CONNECTED;
    private String tempUsername;
    private String authenticatedUser;
    private UserRecord userRecord;
    private String currentVirtualDir = "/";
    private String transferType = "I"; // Mặc định Binary Type I

    private PassiveDataChannel passiveDataChannel;
    private volatile boolean closed = false;

    public ClientSession(Socket controlSocket, ServerContext context) {
        this.controlSocket = controlSocket;
        this.context = context;
        try {
            this.reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            close();
        }
    }

    public synchronized void sendReply(int code, String message) {
        if (closed || writer == null) return;
        try {
            String line = code + " " + message + "\r\n";
            writer.write(line);
            writer.flush();
            LOGGER.fine(String.format("[%s >>> %s:%d] %s", context.getServerId(), getClientIp(), getClientPort(), line.trim()));
        } catch (IOException e) {
            close();
        }
    }

    public String readCommand() throws IOException {
        if (closed || reader == null) return null;
        return reader.readLine();
    }

    public String getClientIp() {
        if (controlSocket != null && controlSocket.getInetAddress() != null) {
            return controlSocket.getInetAddress().getHostAddress();
        }
        return "unknown";
    }

    public int getClientPort() {
        return (controlSocket != null) ? controlSocket.getPort() : 0;
    }

    public Socket getControlSocket() { return controlSocket; }
    public ServerContext getContext() { return context; }
    public SessionState getState() { return state; }
    public void setState(SessionState state) { this.state = state; }

    public String getTempUsername() { return tempUsername; }
    public void setTempUsername(String tempUsername) { this.tempUsername = tempUsername; }

    public String getAuthenticatedUser() { return authenticatedUser; }
    public void setAuthenticatedUser(String authenticatedUser) { this.authenticatedUser = authenticatedUser; }

    public UserRecord getUserRecord() { return userRecord; }
    public void setUserRecord(UserRecord userRecord) { this.userRecord = userRecord; }

    public String getCurrentVirtualDir() { return currentVirtualDir; }
    public void setCurrentVirtualDir(String currentVirtualDir) { this.currentVirtualDir = currentVirtualDir; }

    public String getTransferType() { return transferType; }
    public void setTransferType(String transferType) { this.transferType = transferType; }

    public PassiveDataChannel getPassiveDataChannel() { return passiveDataChannel; }
    public void setPassiveDataChannel(PassiveDataChannel passiveDataChannel) {
        if (this.passiveDataChannel != null) {
            this.passiveDataChannel.close();
        }
        this.passiveDataChannel = passiveDataChannel;
    }

    public boolean isAuthenticated() {
        return state == SessionState.AUTHENTICATED || state == SessionState.TRANSFERRING;
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        state = SessionState.CLOSED;

        if (passiveDataChannel != null) {
            passiveDataChannel.close();
            passiveDataChannel = null;
        }

        // Tự động thu hồi phiên trong CSDL khi ngắt kết nối
        if (authenticatedUser != null) {
            context.getDataStore().removeSession(authenticatedUser);
            // Bắn tín hiệu RMI thông báo giải phóng phiên cho server đối tác
            if (context.getPeerSessionCoordinator() != null) {
                try {
                    context.getPeerSessionCoordinator().notifySessionReleased(authenticatedUser, context.getServerId());
                } catch (Exception ignored) {}
            }
        }

        try {
            if (reader != null) reader.close();
        } catch (Exception ignored) {}

        try {
            if (writer != null) writer.close();
        } catch (Exception ignored) {}

        try {
            if (controlSocket != null && !controlSocket.isClosed()) {
                controlSocket.close();
            }
        } catch (Exception ignored) {}

        LOGGER.info(String.format("[SESSION_CLOSED] Đã đóng phiên kết nối từ %s:%d (%s)",
                getClientIp(), getClientPort(), authenticatedUser != null ? authenticatedUser : "anonymous"));
    }
}
