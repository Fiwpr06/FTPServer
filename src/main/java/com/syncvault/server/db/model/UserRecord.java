package com.syncvault.server.db.model;

import java.time.LocalDateTime;

// Bản ghi tài khoản người dùng
public class UserRecord {
    private int id;
    private String username;
    private String passwordHash;
    private String salt;
    private String homeDir;
    private long maxQuotaBytes;
    private boolean canUpload;
    private boolean canDownload;
    private boolean canDelete;
    private LocalDateTime createdAt;

    public UserRecord() {
        this.canUpload = true;
        this.canDownload = true;
        this.canDelete = true;
        this.maxQuotaBytes = 104857600L; // 100 MB
        this.homeDir = "/";
    }

    public UserRecord(String username, String passwordHash, String salt) {
        this();
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getHomeDir() { return homeDir; }
    public void setHomeDir(String homeDir) { this.homeDir = homeDir; }

    public long getMaxQuotaBytes() { return maxQuotaBytes; }
    public void setMaxQuotaBytes(long maxQuotaBytes) { this.maxQuotaBytes = maxQuotaBytes; }

    public boolean isCanUpload() { return canUpload; }
    public void setCanUpload(boolean canUpload) { this.canUpload = canUpload; }

    public boolean isCanDownload() { return canDownload; }
    public void setCanDownload(boolean canDownload) { this.canDownload = canDownload; }

    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
