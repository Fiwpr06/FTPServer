package com.syncvault.server.core.command.directory;

import com.syncvault.server.ServerContext;
import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.server.db.model.FileMetadataRecord;
import com.syncvault.server.rmi.dto.WriteOperationRequest;
import com.syncvault.server.rmi.dto.WriteOperationResponse;
import com.syncvault.server.security.PathValidator;
import com.syncvault.shared.constant.FtpReplyCode;
import com.syncvault.shared.model.SyncAction;

import java.io.File;

// Xử lý lệnh MKD (Make Directory) kèm cơ chế ủy quyền ghi sang Server A nếu đang ở Server B
public class MkdCommand implements FtpCommand {
    @Override
    public void execute(ClientSession session, String argument) {
        if (!session.isAuthenticated()) {
            session.sendReply(FtpReplyCode.NOT_LOGGED_IN, "Please login with USER and PASS");
            return;
        }

        if (argument == null || argument.trim().isEmpty()) {
            session.sendReply(FtpReplyCode.SYNTAX_ERROR_PARAMETERS, "Directory name required");
            return;
        }

        ServerContext context = session.getContext();
        String username = session.getAuthenticatedUser();
        String targetVirtual = PathValidator.normalizeVirtualPath(session.getCurrentVirtualDir(), argument.trim());

        // Nếu ở Server B (Secondary) -> Ủy quyền sang Server A qua RMI
        if (!context.isPrimary()) {
            if (context.getPeerWriteForwarder() != null) {
                try {
                    WriteOperationRequest req = new WriteOperationRequest(username, SyncAction.MKDIR, targetVirtual);
                    WriteOperationResponse resp = context.getPeerWriteForwarder().executeForwardedWrite(req);
                    if (resp.isSuccess()) {
                        session.sendReply(FtpReplyCode.PATHNAME_CREATED, "\"" + targetVirtual + "\" created via primary delegation");
                    } else {
                        session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "Cannot create directory: " + resp.getMessage());
                    }
                    return;
                } catch (Exception e) {
                    session.sendReply(FtpReplyCode.ACTION_ABORTED_LOCAL_ERROR, "RMI Write-Forwarding failed: " + e.getMessage());
                    return;
                }
            }
        }

        // Thực hiện cục bộ trên Server A
        File userRootDir = new File(context.getStorageRoot(), username);
        File physicalTarget = PathValidator.resolvePhysicalFile(userRootDir, targetVirtual);

        if (physicalTarget == null) {
            session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "Access denied");
            return;
        }

        if (physicalTarget.exists()) {
            session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "Directory already exists");
            return;
        }

        if (physicalTarget.mkdirs()) {
            FileMetadataRecord dirMeta = new FileMetadataRecord(username, targetVirtual, 0, "", true);
            context.getDataStore().saveOrUpdateFileMetadata(dirMeta);
            session.sendReply(FtpReplyCode.PATHNAME_CREATED, "\"" + targetVirtual + "\" created successfully");
        } else {
            session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "Cannot create directory");
        }
    }
}
