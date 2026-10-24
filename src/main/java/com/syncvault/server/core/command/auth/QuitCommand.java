package com.syncvault.server.core.command.auth;

import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.shared.constant.FtpReplyCode;

// Xử lý lệnh QUIT
public class QuitCommand implements FtpCommand {
    @Override
    public void execute(ClientSession session, String argument) {
        session.sendReply(FtpReplyCode.SERVICE_CLOSING_CONTROL, "Goodbye from SyncVault FTP Service");
        session.close();
    }
}
