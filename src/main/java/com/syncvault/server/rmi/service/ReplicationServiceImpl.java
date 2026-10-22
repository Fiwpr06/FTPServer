package com.syncvault.server.rmi.service;

import com.syncvault.server.ServerContext;
import com.syncvault.server.db.model.FileMetadataRecord;
import com.syncvault.server.db.model.SyncLogRecord;
import com.syncvault.server.rmi.common.IReplicationService;
import com.syncvault.server.rmi.dto.ReplicationPayload;
import com.syncvault.server.security.PathValidator;
import com.syncvault.shared.model.SyncAction;
import com.syncvault.shared.util.ChecksumUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

// Dịch vụ nhận dữ liệu đồng bộ tức thời từ Server A sang Server B
public class ReplicationServiceImpl extends UnicastRemoteObject implements IReplicationService {
    private static final Logger LOGGER = Logger.getLogger(ReplicationServiceImpl.class.getName());
    private final ServerContext context;

    public ReplicationServiceImpl(ServerContext context) throws RemoteException {
        super(context.getRmiPort());
        this.context = context;
    }

    @Override
    public synchronized boolean replicateImmediately(ReplicationPayload payload) throws RemoteException {
        LOGGER.info(String.format("[REPLICATION_RECV] Server B nhận đồng bộ tức thời: Action=%s, User=%s, Path=%s",
                payload.getAction(), payload.getOwnerUsername(), payload.getVirtualPath()));

        File userFolder = new File(context.getStorageRoot(), payload.getOwnerUsername());
        if (!userFolder.exists()) {
            userFolder.mkdirs();
        }

        File physicalTarget = PathValidator.resolvePhysicalFile(userFolder, payload.getVirtualPath());
        if (physicalTarget == null) {
            LOGGER.severe("[REPLICATION_ERR] Vi phạm bảo mật đường dẫn trên Server B");
            return false;
        }

        try {
            switch (payload.getAction()) {
                case UPLOAD:
                    if (physicalTarget.getParentFile() != null && !physicalTarget.getParentFile().exists()) {
                        physicalTarget.getParentFile().mkdirs();
                    }
                    if (payload.getData() != null) {
                        try (FileOutputStream fos = new FileOutputStream(physicalTarget)) {
                            fos.write(payload.getData());
                            fos.flush();
                        }
                    }
                    // Kiểm tra SHA-256 xác thực toàn vẹn
                    String receivedSha256 = ChecksumUtil.calculateSHA256(physicalTarget);
                    boolean checksumMatches = (payload.getSha256() == null || payload.getSha256().isEmpty() ||
                            payload.getSha256().equalsIgnoreCase(receivedSha256));

                    if (!checksumMatches) {
                        LOGGER.severe(String.format("[CHECKSUM_MISMATCH] Kỳ vọng: %s, Thực tế: %s",
                                payload.getSha256(), receivedSha256));
                        context.getDataStore().recordSyncLog(new SyncLogRecord(
                                SyncAction.UPLOAD,
                                "SERVER_A",
                                context.getServerId(),
                                payload.getVirtualPath(),
                                receivedSha256,
                                "FAILED",
                                "Checksum mismatch after instant replication"
                        ));
                        return false;
                    }

                    // Lưu metadata vào DB Server B
                    FileMetadataRecord meta = new FileMetadataRecord(
                            payload.getOwnerUsername(),
                            payload.getVirtualPath(),
                            physicalTarget.length(),
                            receivedSha256,
                            false
                    );
                    context.getDataStore().saveOrUpdateFileMetadata(meta);
                    context.getDataStore().recordSyncLog(new SyncLogRecord(
                            SyncAction.UPLOAD,
                            "SERVER_A",
                            context.getServerId(),
                            payload.getVirtualPath(),
                            receivedSha256,
                            "COMPLETED",
                            "Instant replication verified successfully"
                    ));
                    return true;

                case DELETE:
                    if (physicalTarget.exists()) {
                        physicalTarget.delete();
                    }
                    context.getDataStore().deleteFileMetadata(payload.getOwnerUsername(), payload.getVirtualPath());
                    context.getDataStore().recordSyncLog(new SyncLogRecord(
                            SyncAction.DELETE,
                            "SERVER_A",
                            context.getServerId(),
                            payload.getVirtualPath(),
                            "",
                            "COMPLETED",
                            "Deleted replica file"
                    ));
                    return true;

                case MKDIR:
                    physicalTarget.mkdirs();
                    FileMetadataRecord dirMeta = new FileMetadataRecord(
                            payload.getOwnerUsername(),
                            payload.getVirtualPath(),
                            0,
                            "",
                            true
                    );
                    context.getDataStore().saveOrUpdateFileMetadata(dirMeta);
                    return true;

                case RENAME:
                    if (payload.getNewVirtualPath() != null) {
                        File newTarget = PathValidator.resolvePhysicalFile(userFolder, payload.getNewVirtualPath());
                        if (newTarget != null && physicalTarget.exists()) {
                            physicalTarget.renameTo(newTarget);
                            context.getDataStore().renameFileMetadata(payload.getOwnerUsername(), payload.getVirtualPath(), payload.getNewVirtualPath());
                        }
                    }
                    return true;

                default:
                    return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[REPLICATION_ERR] Lỗi đồng bộ tại Server B: " + e.getMessage(), e);
            return false;
        }
    }
}
