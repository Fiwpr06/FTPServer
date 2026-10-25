package com.syncvault.server.core.command.directory;

import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.server.security.PathValidator;
import com.syncvault.shared.constant.FtpReplyCode;

import java.io.File;

// Xử lý lệnh CWD (Change Working Directory)
public class CwdCommand implements FtpCommand {
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

        String targetVirtual = PathValidator.normalizeVirtualPath(session.getCurrentVirtualDir(), argument.trim());
        File userRootDir = new File(session.getContext().getStorageRoot(), session.getAuthenticatedUser());
        File physicalTarget = PathValidator.resolvePhysicalFile(userRootDir, targetVirtual);

        if (physicalTarget == null || (!physicalTarget.exists() && !targetVirtual.equals("/"))) {
            session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "Directory not found");
            return;
        }

        session.setCurrentVirtualDir(targetVirtual);
        session.sendReply(FtpReplyCode.REQUESTED_FILE_ACTION_OK, "Directory changed to " + targetVirtual);
    }
}
