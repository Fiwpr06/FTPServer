package com.syncvault.server.core.command.system;

import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;

// Xử lý lệnh FEAT (Feature negotiation)
public class FeatCommand implements FtpCommand {
    @Override
    public void execute(ClientSession session, String argument) {
        session.sendReply(211, "Features:\r\n UTF8\r\n PASV\r\n SIZE\r\n MLSD\r\n211 End");
    }
}
