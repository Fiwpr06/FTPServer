package com.syncvault.server.core.command.auth;

import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.server.core.session.SessionState;
import com.syncvault.shared.constant.FtpReplyCode;

// Xử lý lệnh USER trong giao thức FTP
public class UserCommand implements FtpCommand {
    @Override
    public void execute(ClientSession session, String argument) {
        if (argument == null || argument.trim().isEmpty()) {
            session.sendReply(FtpReplyCode.SYNTAX_ERROR_PARAMETERS, "Username required");
            return;
        }

        String username = argument.trim();
        session.setTempUsername(username);
        session.setState(SessionState.NEED_PASSWORD);
        session.sendReply(FtpReplyCode.USER_NAME_OK_NEED_PASS, "User name okay, need password for " + username);
    }
}
