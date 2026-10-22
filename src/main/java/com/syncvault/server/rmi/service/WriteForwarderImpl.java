package com.syncvault.server.rmi.service;

import com.syncvault.server.ServerContext;
import com.syncvault.server.db.model.FileMetadataRecord;
import com.syncvault.server.db.model.SyncLogRecord;
import com.syncvault.server.db.model.UserRecord;
import com.syncvault.server.rmi.common.IReplicationService;
import com.syncvault.server.rmi.common.IWriteForwarder;
import com.syncvault.server.rmi.dto.ReplicationPayload;
import com.syncvault.server.rmi.dto.WriteOperationRequest;
import com.syncvault.server.rmi.dto.WriteOperationResponse;
import com.syncvault.server.security.PathValidator;
import com.syncvault.shared.model.SyncAction;
import com.syncvault.shared.util.ChecksumUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

// Dịch vụ tiếp nhận ủy quyền thao tác ghi từ Server B sang Server A
public class WriteForwarderImpl extends UnicastRemoteObject implements IWriteForwarder {
    private static final Logger LOGGER = Logger.getLogger(WriteForwarderImpl.class.getName());
    private final ServerContext context;

    public WriteForwarderImpl(ServerContext context) throws RemoteException {
        super(context.getRmiPort());
        this.context = context;
    }

    @Override
    public synchronized WriteOperationResponse executeForwardedWrite(WriteOperationRequest request) throws RemoteException {
        LOGGER.info(String.format("[WRITE_FORWARD] Server A nhận ủy quyền: Action=%s, User=%s, Path=%s",
                request.getAction(), request.getUsername(), request.getVirtualPath()));

        UserRecord user = context.getDataStore().getUserByUsername(request.getUsername());
        if (user == null) {
            return WriteOperationResponse.fail("User not found: " + request.getUsername());
        }

        File userFolder = new File(context.getStorageRoot(), request.getUsername());
        if (!userFolder.exists()) {
            userFolder.mkdirs();
        }

        File physicalTarget = PathValidator.resolvePhysicalFile(userFolder, request.getVirtualPath());
        if (physicalTarget == null) {
            return WriteOperationResponse.fail("Access Denied: Path outside virtual root");
        }

        try {
            switch (request.getAction()) {
                case UPLOAD:
                    if (request.getFileBytes() == null) {
                        return WriteOperationResponse.fail("No file bytes received for upload");
                    }
                    if (physicalTarget.getParentFile() != null && !physicalTarget.getParentFile().exists()) {
                        physicalTarget.getParentFile().mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(physicalTarget)) {
                        fos.write(request.getFileBytes());
                        fos.flush();
                    }
                    String actualSha256 = ChecksumUtil.calculateSHA256(physicalTarget);

                    // Lưu metadata vào CSDL Server A
                    FileMetadataRecord meta = new FileMetadataRecord(
                            request.getUsername(),
                            request.getVirtualPath(),
                            physicalTarget.length(),
                            actualSha256,
                            false
                    );
                    context.getDataStore().saveOrUpdateFileMetadata(meta);

                    // Đồng bộ tức thời ngược lại sang Server B
                    replicateToSecondary(request.getUsername(), SyncAction.UPLOAD, request.getVirtualPath(),
                            request.getFileBytes(), actualSha256, false, null);

                    context.getDataStore().recordSyncLog(new SyncLogRecord(
                            SyncAction.UPLOAD,
                            context.getServerId(),
                            context.getPeerServerId(),
                            request.getVirtualPath(),
                            actualSha256,
                            "COMPLETED",
                            "Forwarded upload & instant replication completed"
                    ));

                    return WriteOperationResponse.ok(actualSha256);

                case DELETE:
                    boolean deleted = false;
                    if (physicalTarget.exists()) {
                        deleted = physicalTarget.delete();
                    }
                    context.getDataStore().deleteFileMetadata(request.getUsername(), request.getVirtualPath());

                    // Đẩy sự kiện xóa tức thời sang Server B
                    replicateToSecondary(request.getUsername(), SyncAction.DELETE, request.getVirtualPath(),
                            null, null, false, null);

                    return deleted ? WriteOperationResponse.ok("") : WriteOperationResponse.fail("File does not exist on Server A");

                case MKDIR:
                    boolean created = physicalTarget.mkdirs();
                    FileMetadataRecord dirMeta = new FileMetadataRecord(
                            request.getUsername(),
                            request.getVirtualPath(),
                            0,
                            "",
                            true
                    );
                    context.getDataStore().saveOrUpdateFileMetadata(dirMeta);

                    replicateToSecondary(request.getUsername(), SyncAction.MKDIR, request.getVirtualPath(),
                            null, null, true, null);

                    return created ? WriteOperationResponse.ok("") : WriteOperationResponse.fail("Could not create directory");

                case RENAME:
                    if (request.getNewVirtualPath() == null) {
                        return WriteOperationResponse.fail("Missing new virtual path for rename");
                    }
                    File newPhysicalTarget = PathValidator.resolvePhysicalFile(userFolder, request.getNewVirtualPath());
                    if (newPhysicalTarget == null) {
                        return WriteOperationResponse.fail("Invalid new target path");
                    }
                    boolean renamed = physicalTarget.renameTo(newPhysicalTarget);
                    if (renamed) {
                        context.getDataStore().renameFileMetadata(request.getUsername(), request.getVirtualPath(), request.getNewVirtualPath());
                        replicateToSecondary(request.getUsername(), SyncAction.RENAME, request.getVirtualPath(),
                                null, null, false, request.getNewVirtualPath());
                    }
                    return renamed ? WriteOperationResponse.ok("") : WriteOperationResponse.fail("Rename failed on Server A");

                default:
                    return WriteOperationResponse.fail("Unsupported write action");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[WRITE_FORWARD_ERR] " + e.getMessage(), e);
            return WriteOperationResponse.fail("Internal Server Error: " + e.getMessage());
        }
    }

    private void replicateToSecondary(String username, SyncAction action, String virtualPath,
                                      byte[] data, String sha256, boolean isDir, String newPath) {
        IReplicationService peerReplication = context.getPeerReplicationService();
        if (peerReplication != null) {
            try {
                ReplicationPayload payload = new ReplicationPayload(username, action, virtualPath);
                payload.setData(data);
                payload.setSha256(sha256);
                payload.setDirectory(isDir);
                payload.setNewVirtualPath(newPath);
                if (data != null) {
                    payload.setFileSize(data.length);
                }
                peerReplication.replicateImmediately(payload);
                LOGGER.info("[INSTANT_SYNC] Đã đồng bộ tức thời sang Server B: " + virtualPath);
            } catch (Exception e) {
                LOGGER.warning("[INSTANT_SYNC_WARN] Không thể đồng bộ tức thời sang Server B: " + e.getMessage());
            }
        }
    }
}
