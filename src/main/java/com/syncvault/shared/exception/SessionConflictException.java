package com.syncvault.shared.exception;

// Ngoại lệ khi phát hiện tài khoản đã đăng nhập tại một máy chủ khác trong cụm
public class SessionConflictException extends Exception {
    private final String username;
    private final String existingServerId;
    private final String existingClientIp;
    private final String loginTime;

    public SessionConflictException(String username, String existingServerId, String existingClientIp, String loginTime) {
        super(String.format("Tài khoản '%s' đang hoạt động tại %s (IP: %s) từ lúc %s!", username, existingServerId, existingClientIp, loginTime));
        this.username = username;
        this.existingServerId = existingServerId;
        this.existingClientIp = existingClientIp;
        this.loginTime = loginTime;
    }

    public String getUsername() {
        return username;
    }

    public String getExistingServerId() {
        return existingServerId;
    }

    public String getExistingClientIp() {
        return existingClientIp;
    }

    public String getLoginTime() {
        return loginTime;
    }
}
