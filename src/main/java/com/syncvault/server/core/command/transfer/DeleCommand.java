package com.syncvault.server.core.command.transfer;

import com.syncvault.server.ServerContext;
import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.server.db.model.SyncLogRecord;
import com.syncvault.server.rmi.common.IReplicationService;
import com.syncvault.server.rmi.common.IWriteForwarder;
import com.syncvault.server.rmi.dto.ReplicationPayload;
import com.syncvault.server.rmi.dto.WriteOperationRequest;
import com.syncvault.server.rmi.dto.WriteOperationResponse;
import com.syncvault.server.security.PathValidator;
import com.syncvault.shared.constant.FtpReplyCode;
import com.syncvault.shared.model.SyncAction;

import java.io.File;

// Xử lý lệnh DELE (Xóa tệp tin) kèm ủy quyền ghi sang Server A và đồng bộ xóa tức thời sang Server B
public class DeleCommand implements FtpCommand {
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

        ServerContext context = session.getContext();
        String username = session.getAuthenticatedUser();
        String targetVirtual = PathValidator.normalizeVirtualPath(session.getCurrentVirtualDir(), argument.trim());

        // Nếu ở Server B -> Ủy quyền sang Server A
        if (!context.isPrimary()) {
            IWriteForwarder forwarder = context.getPeerWriteForwarder();
            if (forwarder != null) {
                try {
                    WriteOperationRequest req = new WriteOperationRequest(username, SyncAction.DELETE, targetVirtual);
                    WriteOperationResponse resp = forwarder.executeForwardedWrite(req);
                    if (resp.isSuccess()) {
                        session.sendReply(FtpReplyCode.REQUESTED_FILE_ACTION_OK, "File deleted via primary delegation");
                    } else {
                        session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "Delete rejected: " + resp.getMessage());
                    }
                    return;
                } catch (Exception e) {
                    session.sendReply(FtpReplyCode.ACTION_ABORTED_LOCAL_ERROR, "RMI Write-Forwarding failed: " + e.getMessage());
                    return;
                }
            }
        }

        // Xóa trên Server A
        File userRootDir = new File(context.getStorageRoot(), username);
        File physicalFile = PathValidator.resolvePhysicalFile(userRootDir, targetVirtual);

        if (physicalFile == null || !physicalFile.exists() || physicalFile.isDirectory()) {
            session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "File not found or is a directory");
            return;
        }

        if (physicalFile.delete()) {
            context.getDataStore().deleteFileMetadata(username, targetVirtual);

            // Đồng bộ xóa tức thời sang Server B
            IReplicationService peerReplication = context.getPeerReplicationService();
            if (peerReplication != null) {
                try {
                    ReplicationPayload payload = new ReplicationPayload(username, SyncAction.DELETE, targetVirtual);
                    peerReplication.replicateImmediately(payload);
                } catch (Exception ignored) {}
            }

            context.getDataStore().recordSyncLog(new SyncLogRecord(
                    SyncAction.DELETE,
                    context.getServerId(),
                    context.getPeerServerId(),
                    targetVirtual,
                    "",
                    "COMPLETED",
                    "Deleted on Primary and replicated delete to Secondary"
            ));

            session.sendReply(FtpReplyCode.REQUESTED_FILE_ACTION_OK, "File deleted successfully");
        } else {
            session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "Could not delete file");
        }
    }
}
