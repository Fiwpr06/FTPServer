package com.syncvault.server.rmi.dto;

import com.syncvault.shared.model.SyncAction;
import java.io.Serializable;

// Yêu cầu ủy quyền thao tác ghi từ Server B sang Server A
public class WriteOperationRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private SyncAction action;
    private String virtualPath;
    private String newVirtualPath; // Cho lệnh RENAME
    private byte[] fileBytes;      // Cho lệnh UPLOAD
    private String sha256;

    public WriteOperationRequest() {}

    public WriteOperationRequest(String username, SyncAction action, String virtualPath) {
        this.username = username;
        this.action = action;
        this.virtualPath = virtualPath;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public SyncAction getAction() { return action; }
    public void setAction(SyncAction action) { this.action = action; }

    public String getVirtualPath() { return virtualPath; }
    public void setVirtualPath(String virtualPath) { this.virtualPath = virtualPath; }

    public String getNewVirtualPath() { return newVirtualPath; }
    public void setNewVirtualPath(String newVirtualPath) { this.newVirtualPath = newVirtualPath; }

    public byte[] getFileBytes() { return fileBytes; }
    public void setFileBytes(byte[] fileBytes) { this.fileBytes = fileBytes; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
}
