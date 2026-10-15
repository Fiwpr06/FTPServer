package com.syncvault.server.db.model;

import java.time.LocalDateTime;

// Bản ghi phiên đăng nhập đang hoạt động
public class SessionRecord {
    private int id;
    private String username;
    private String serverId;
    private String clientIp;
    private int controlPort;
    private LocalDateTime loginTime;
    private LocalDateTime lastHeartbeat;
    private String sessionToken;

    public SessionRecord() {}

    public SessionRecord(String username, String serverId, String clientIp, int controlPort, String sessionToken) {
        this.username = username;
        this.serverId = serverId;
        this.clientIp = clientIp;
        this.controlPort = controlPort;
        this.sessionToken = sessionToken;
        this.loginTime = LocalDateTime.now();
        this.lastHeartbeat = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public int getControlPort() { return controlPort; }
    public void setControlPort(int controlPort) { this.controlPort = controlPort; }

    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}
