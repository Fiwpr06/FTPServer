package com.syncvault.server.rmi.dto;

import com.syncvault.shared.model.SyncAction;
import java.io.Serializable;

// Gói dữ liệu đồng bộ tức thời từ Server A (Primary) đẩy sang Server B (Secondary)
public class ReplicationPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ownerUsername;
    private SyncAction action;
    private String virtualPath;
    private String newVirtualPath;
    private byte[] data;
    private long fileSize;
    private String sha256;
    private boolean isDirectory;

    public ReplicationPayload() {}

    public ReplicationPayload(String ownerUsername, SyncAction action, String virtualPath) {
        this.ownerUsername = ownerUsername;
        this.action = action;
        this.virtualPath = virtualPath;
    }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public SyncAction getAction() { return action; }
    public void setAction(SyncAction action) { this.action = action; }

    public String getVirtualPath() { return virtualPath; }
    public void setVirtualPath(String virtualPath) { this.virtualPath = virtualPath; }

    public String getNewVirtualPath() { return newVirtualPath; }
    public void setNewVirtualPath(String newVirtualPath) { this.newVirtualPath = newVirtualPath; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public boolean isDirectory() { return isDirectory; }
    public void setDirectory(boolean directory) { isDirectory = directory; }
}
