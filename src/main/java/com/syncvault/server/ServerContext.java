package com.syncvault.server;

import com.syncvault.server.db.store.IDataStore;
import com.syncvault.server.rmi.common.IReplicationService;
import com.syncvault.server.rmi.common.ISessionCoordinator;
import com.syncvault.server.rmi.common.IWriteForwarder;
import com.syncvault.shared.model.ServerMode;

import java.io.File;

// Ngữ cảnh hoạt động tập trung của Server (Singleton / Context Container)
public class ServerContext {
    private final String serverId;
    private final ServerMode mode;
    private final int ftpPort;
    private final int rmiPort;
    private final File storageRoot;
    private final IDataStore dataStore;

    private String peerHost = "127.0.0.1";
    private int peerRmiPort;
    private String peerServerId;

    // RMI references tới server đối tác
    private ISessionCoordinator peerSessionCoordinator;
    private IWriteForwarder peerWriteForwarder;
    private IReplicationService peerReplicationService;

    public ServerContext(String serverId, ServerMode mode, int ftpPort, int rmiPort, File storageRoot, IDataStore dataStore) {
        this.serverId = serverId;
        this.mode = mode;
        this.ftpPort = ftpPort;
        this.rmiPort = rmiPort;
        this.storageRoot = storageRoot;
        this.dataStore = dataStore;

        if (!storageRoot.exists()) {
            storageRoot.mkdirs();
        }
    }

    public String getServerId() { return serverId; }
    public ServerMode getMode() { return mode; }
    public int getFtpPort() { return ftpPort; }
    public int getRmiPort() { return rmiPort; }
    public File getStorageRoot() { return storageRoot; }
    public IDataStore getDataStore() { return dataStore; }

    public String getPeerHost() { return peerHost; }
    public void setPeerHost(String peerHost) { this.peerHost = peerHost; }

    public int getPeerRmiPort() { return peerRmiPort; }
    public void setPeerRmiPort(int peerRmiPort) { this.peerRmiPort = peerRmiPort; }

    public String getPeerServerId() { return peerServerId; }
    public void setPeerServerId(String peerServerId) { this.peerServerId = peerServerId; }

    public ISessionCoordinator getPeerSessionCoordinator() { return peerSessionCoordinator; }
    public void setPeerSessionCoordinator(ISessionCoordinator peerSessionCoordinator) { this.peerSessionCoordinator = peerSessionCoordinator; }

    public IWriteForwarder getPeerWriteForwarder() { return peerWriteForwarder; }
    public void setPeerWriteForwarder(IWriteForwarder peerWriteForwarder) { this.peerWriteForwarder = peerWriteForwarder; }

    public IReplicationService getPeerReplicationService() { return peerReplicationService; }
    public void setPeerReplicationService(IReplicationService peerReplicationService) { this.peerReplicationService = peerReplicationService; }

    public boolean isPrimary() {
        return mode == ServerMode.PRIMARY;
    }
}
