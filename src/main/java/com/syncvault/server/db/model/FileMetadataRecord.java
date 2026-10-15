package com.syncvault.server.db.model;

import java.time.LocalDateTime;

// Bản ghi metadata của tệp tin trong CSDL
public class FileMetadataRecord {
    private long id;
    private String ownerUsername;
    private String virtualPath;
    private long fileSize;
    private String sha256Checksum;
    private boolean isDirectory;
    private LocalDateTime lastModified;
    private boolean isDeleted;

    public FileMetadataRecord() {
        this.lastModified = LocalDateTime.now();
        this.isDeleted = false;
    }

    public FileMetadataRecord(String ownerUsername, String virtualPath, long fileSize, String sha256Checksum, boolean isDirectory) {
        this();
        this.ownerUsername = ownerUsername;
        this.virtualPath = virtualPath;
        this.fileSize = fileSize;
        this.sha256Checksum = sha256Checksum;
        this.isDirectory = isDirectory;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getVirtualPath() { return virtualPath; }
    public void setVirtualPath(String virtualPath) { this.virtualPath = virtualPath; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getSha256Checksum() { return sha256Checksum; }
    public void setSha256Checksum(String sha256Checksum) { this.sha256Checksum = sha256Checksum; }

    public boolean isDirectory() { return isDirectory; }
    public void setDirectory(boolean directory) { isDirectory = directory; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}
