package com.syncvault.server.core.command.directory;

import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.shared.constant.FtpReplyCode;

// Xử lý lệnh PWD (Print Working Directory)
public class PwdCommand implements FtpCommand {
    @Override
    public void execute(ClientSession session, String argument) {
        if (!session.isAuthenticated()) {
            session.sendReply(FtpReplyCode.NOT_LOGGED_IN, "Please login with USER and PASS");
            return;
        }

        String virtualDir = session.getCurrentVirtualDir();
        session.sendReply(FtpReplyCode.PATHNAME_CREATED, "\"" + virtualDir + "\" is current directory");
    }
}
