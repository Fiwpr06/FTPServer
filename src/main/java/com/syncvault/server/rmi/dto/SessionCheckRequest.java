package com.syncvault.server.rmi.dto;

import java.io.Serializable;

// Yêu cầu kiểm tra và cấp quyền phiên đăng nhập qua RMI
public class SessionCheckRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String requestingServerId;
    private String clientIp;
    private int controlPort;
    private String sessionToken;

    public SessionCheckRequest() {}

    public SessionCheckRequest(String username, String requestingServerId, String clientIp, int controlPort, String sessionToken) {
        this.username = username;
        this.requestingServerId = requestingServerId;
        this.clientIp = clientIp;
        this.controlPort = controlPort;
        this.sessionToken = sessionToken;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRequestingServerId() { return requestingServerId; }
    public void setRequestingServerId(String requestingServerId) { this.requestingServerId = requestingServerId; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public int getControlPort() { return controlPort; }
    public void setControlPort(int controlPort) { this.controlPort = controlPort; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}
