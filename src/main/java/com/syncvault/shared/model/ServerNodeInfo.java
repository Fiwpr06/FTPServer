package com.syncvault.shared.model;

import java.io.Serializable;

// Thông tin định danh và địa chỉ của một nút Server trong mạng
public class ServerNodeInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String serverId;
    private String host;
    private int ftpPort;
    private int rmiPort;
    private ServerMode mode;
    private boolean online;

    public ServerNodeInfo() {}

    public ServerNodeInfo(String serverId, String host, int ftpPort, int rmiPort, ServerMode mode) {
        this.serverId = serverId;
        this.host = host;
        this.ftpPort = ftpPort;
        this.rmiPort = rmiPort;
        this.mode = mode;
        this.online = false;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getFtpPort() {
        return ftpPort;
    }

    public void setFtpPort(int ftpPort) {
        this.ftpPort = ftpPort;
    }

    public int getRmiPort() {
        return rmiPort;
    }

    public void setRmiPort(int rmiPort) {
        this.rmiPort = rmiPort;
    }

    public ServerMode getMode() {
        return mode;
    }

    public void setMode(ServerMode mode) {
        this.mode = mode;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public String toString() {
        return String.format("%s (%s:%d | RMI:%d)", serverId, host, ftpPort, rmiPort);
    }
}
