package com.syncvault.server.core.command.directory;

import com.syncvault.server.ServerContext;
import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.server.rmi.dto.WriteOperationRequest;
import com.syncvault.server.rmi.dto.WriteOperationResponse;
import com.syncvault.server.security.PathValidator;
import com.syncvault.shared.constant.FtpReplyCode;
import com.syncvault.shared.model.SyncAction;

import java.io.File;

// Xử lý lệnh RMD (Remove Directory)
public class RmdCommand implements FtpCommand {
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

        if (!context.isPrimary()) {
            if (context.getPeerWriteForwarder() != null) {
                try {
                    WriteOperationRequest req = new WriteOperationRequest(username, SyncAction.DELETE, targetVirtual);
                    WriteOperationResponse resp = context.getPeerWriteForwarder().executeForwardedWrite(req);
                    if (resp.isSuccess()) {
                        session.sendReply(FtpReplyCode.REQUESTED_FILE_ACTION_OK, "Directory removed via primary delegation");
                    } else {
                        session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "Cannot remove directory: " + resp.getMessage());
                    }
                    return;
                } catch (Exception e) {
                    session.sendReply(FtpReplyCode.ACTION_ABORTED_LOCAL_ERROR, "RMI Write-Forwarding failed: " + e.getMessage());
                    return;
                }
            }
        }

        File userRootDir = new File(context.getStorageRoot(), username);
        File physicalTarget = PathValidator.resolvePhysicalFile(userRootDir, targetVirtual);

        if (physicalTarget == null || !physicalTarget.exists() || !physicalTarget.isDirectory()) {
            session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "Directory not found");
            return;
        }

        if (physicalTarget.delete()) {
            context.getDataStore().deleteFileMetadata(username, targetVirtual);
            session.sendReply(FtpReplyCode.REQUESTED_FILE_ACTION_OK, "Directory removed");
        } else {
            session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "Cannot remove directory (may not be empty)");
        }
    }
}
