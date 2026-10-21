package com.syncvault.server.rmi.dto;

import java.io.Serializable;

// Phản hồi kiểm tra phiên từ Server đối tác
public class SessionCheckResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean allowed;
    private String existingServerId;
    private String existingClientIp;
    private String loginTime;
    private String message;

    public SessionCheckResponse() {}

    public static SessionCheckResponse success() {
        SessionCheckResponse resp = new SessionCheckResponse();
        resp.setAllowed(true);
        resp.setMessage("OK");
        return resp;
    }

    public static SessionCheckResponse conflict(String existingServerId, String existingClientIp, String loginTime) {
        SessionCheckResponse resp = new SessionCheckResponse();
        resp.setAllowed(false);
        resp.setExistingServerId(existingServerId);
        resp.setExistingClientIp(existingClientIp);
        resp.setLoginTime(loginTime);
        resp.setMessage(String.format("Tài khoản đang đăng nhập tại %s (IP: %s) từ lúc %s!", existingServerId, existingClientIp, loginTime));
        return resp;
    }

    public boolean isAllowed() { return allowed; }
    public void setAllowed(boolean allowed) { this.allowed = allowed; }

    public String getExistingServerId() { return existingServerId; }
    public void setExistingServerId(String existingServerId) { this.existingServerId = existingServerId; }

    public String getExistingClientIp() { return existingClientIp; }
    public void setExistingClientIp(String existingClientIp) { this.existingClientIp = existingClientIp; }

    public String getLoginTime() { return loginTime; }
    public void setLoginTime(String loginTime) { this.loginTime = loginTime; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
