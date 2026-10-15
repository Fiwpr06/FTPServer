package com.syncvault.server.db.model;

import com.syncvault.shared.model.SyncAction;
import java.time.LocalDateTime;

// Bản ghi lịch sử đồng bộ giữa hai Server
public class SyncLogRecord {
    private long id;
    private SyncAction action;
    private String sourceServer;
    private String targetServer;
    private String virtualPath;
    private String checksum;
    private String status; // PENDING, SYNCING, COMPLETED, FAILED
    private String message;
    private LocalDateTime timestamp;

    public SyncLogRecord() {
        this.timestamp = LocalDateTime.now();
        this.status = "PENDING";
    }

    public SyncLogRecord(SyncAction action, String sourceServer, String targetServer, String virtualPath, String checksum, String status, String message) {
        this();
        this.action = action;
        this.sourceServer = sourceServer;
        this.targetServer = targetServer;
        this.virtualPath = virtualPath;
        this.checksum = checksum;
        this.status = status;
        this.message = message;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public SyncAction getAction() { return action; }
    public void setAction(SyncAction action) { this.action = action; }

    public String getSourceServer() { return sourceServer; }
    public void setSourceServer(String sourceServer) { this.sourceServer = sourceServer; }

    public String getTargetServer() { return targetServer; }
    public void setTargetServer(String targetServer) { this.targetServer = targetServer; }

    public String getVirtualPath() { return virtualPath; }
    public void setVirtualPath(String virtualPath) { this.virtualPath = virtualPath; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
