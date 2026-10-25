package com.syncvault.server.core.command.transfer;

import com.syncvault.server.ServerContext;
import com.syncvault.server.core.channel.PassiveDataChannel;
import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.server.db.model.FileMetadataRecord;
import com.syncvault.server.db.model.SyncLogRecord;
import com.syncvault.server.rmi.common.IReplicationService;
import com.syncvault.server.rmi.common.IWriteForwarder;
import com.syncvault.server.rmi.dto.ReplicationPayload;
import com.syncvault.server.rmi.dto.WriteOperationRequest;
import com.syncvault.server.rmi.dto.WriteOperationResponse;
import com.syncvault.server.security.PathValidator;
import com.syncvault.server.security.QuotaValidator;
import com.syncvault.shared.constant.FtpReplyCode;
import com.syncvault.shared.constant.SystemConfig;
import com.syncvault.shared.model.SyncAction;
import com.syncvault.shared.util.ChecksumUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.logging.Logger;

// Xử lý lệnh STOR (Upload tệp tin) kèm ủy quyền ghi từ B sang A và đồng bộ tức thời từ A sang B
public class StorCommand implements FtpCommand {
    private static final Logger LOGGER = Logger.getLogger(StorCommand.class.getName());

    @Override
    public void execute(ClientSession session, String argument) {
        if (!session.isAuthenticated()) {
            session.sendReply(FtpReplyCode.NOT_LOGGED_IN, "Please login with USER and PASS");
            return;
        }

        if (argument == null || argument.trim().isEmpty()) {
            session.sendReply(FtpReplyCode.SYNTAX_ERROR_PARAMETERS, "Filename required");
            return;
        }

        PassiveDataChannel dataChannel = session.getPassiveDataChannel();
        if (dataChannel == null) {
            session.sendReply(FtpReplyCode.CANT_OPEN_DATA_CONNECTION, "Use PASV first");
            return;
        }

        ServerContext context = session.getContext();
        String username = session.getAuthenticatedUser();
        String targetVirtual = PathValidator.normalizeVirtualPath(session.getCurrentVirtualDir(), argument.trim());
        File userRootDir = new File(context.getStorageRoot(), username);

        session.sendReply(FtpReplyCode.FILE_STATUS_OK_OPEN_DATA, "Ready to receive file data in BINARY mode");

        try (Socket dataSocket = dataChannel.acceptDataSocket();
             InputStream is = dataSocket.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[SystemConfig.BUFFER_SIZE];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            byte[] fileBytes = baos.toByteArray();

            // Kiểm tra dung lượng Quota của User
            long maxQuota = (session.getUserRecord() != null) ? session.getUserRecord().getMaxQuotaBytes() : 104857600L;
            if (!QuotaValidator.checkQuota(userRootDir, fileBytes.length, maxQuota)) {
                session.sendReply(FtpReplyCode.EXCEEDED_STORAGE_ALLOCATION, "Upload rejected: Storage quota exceeded");
                return;
            }

            // TRƯỜNG HỢP 1: Client kết nối tới Server B (Secondary) -> Ủy quyền ghi sang Server A qua RMI
            if (!context.isPrimary()) {
                LOGGER.info("[WRITE_FORWARD] Client upload tại Server B -> Đang ủy quyền ghi sang Server A...");
                IWriteForwarder forwarder = context.getPeerWriteForwarder();
                if (forwarder == null) {
                    session.sendReply(FtpReplyCode.ACTION_ABORTED_LOCAL_ERROR, "Cannot reach Primary Server A for write operation");
                    return;
                }

                WriteOperationRequest req = new WriteOperationRequest(username, SyncAction.UPLOAD, targetVirtual);
                req.setFileBytes(fileBytes);
                WriteOperationResponse resp = forwarder.executeForwardedWrite(req);

                if (resp.isSuccess()) {
                    session.sendReply(FtpReplyCode.CLOSING_DATA_CONNECTION,
                            String.format("Transfer complete (Forwarded & Replicated, SHA-256: %s)", resp.getVerifiedSha256()));
                } else {
                    session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "Primary Server rejected upload: " + resp.getMessage());
                }
                return;
            }

            // TRƯỜNG HỢP 2: Client kết nối trực tiếp Server A (Primary)
            File physicalTarget = PathValidator.resolvePhysicalFile(userRootDir, targetVirtual);
            if (physicalTarget == null) {
                session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "Access denied: Invalid virtual path");
                return;
            }

            if (physicalTarget.getParentFile() != null && !physicalTarget.getParentFile().exists()) {
                physicalTarget.getParentFile().mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(physicalTarget)) {
                fos.write(fileBytes);
                fos.flush();
            }

            String sha256 = ChecksumUtil.calculateSHA256(physicalTarget);

            // Ghi metadata vào CSDL Server A
            FileMetadataRecord meta = new FileMetadataRecord(username, targetVirtual, physicalTarget.length(), sha256, false);
            context.getDataStore().saveOrUpdateFileMetadata(meta);

            // ĐỒNG BỘ TỨC THỜI SANG SERVER B (Secondary) NGAY LẬP TỨC
            IReplicationService peerReplication = context.getPeerReplicationService();
            if (peerReplication != null) {
                try {
                    ReplicationPayload payload = new ReplicationPayload(username, SyncAction.UPLOAD, targetVirtual);
                    payload.setData(fileBytes);
                    payload.setFileSize(fileBytes.length);
                    payload.setSha256(sha256);
                    payload.setDirectory(false);
                    boolean syncOk = peerReplication.replicateImmediately(payload);
                    LOGGER.info(String.format("[INSTANT_SYNC_RESULT] Server B đồng bộ tức thời: %s (SHA-256: %s)",
                            syncOk ? "THÀNH CÔNG" : "THẤT BẠI", sha256));
                } catch (Exception ex) {
                    LOGGER.warning("[INSTANT_SYNC_WARN] Server B offline hoặc lỗi đồng bộ: " + ex.getMessage());
                }
            }

            context.getDataStore().recordSyncLog(new SyncLogRecord(
                    SyncAction.UPLOAD,
                    context.getServerId(),
                    context.getPeerServerId(),
                    targetVirtual,
                    sha256,
                    "COMPLETED",
                    "Uploaded on Primary and replicated immediately to Secondary"
            ));

            session.sendReply(FtpReplyCode.CLOSING_DATA_CONNECTION,
                    String.format("Transfer complete (SHA-256: %s)", sha256));

        } catch (Exception e) {
            LOGGER.severe("[STOR_ERR] Lỗi trong quá trình upload: " + e.getMessage());
            session.sendReply(FtpReplyCode.CONNECTION_CLOSED_TRANSFER_ABORTED, "Upload failed: " + e.getMessage());
        } finally {
            session.setPassiveDataChannel(null);
        }
    }
}
