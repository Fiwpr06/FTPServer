package com.syncvault.server.core.command.system;

import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.shared.constant.FtpReplyCode;

// Xử lý lệnh SYST
public class SystCommand implements FtpCommand {
    @Override
    public void execute(ClientSession session, String argument) {
        session.sendReply(FtpReplyCode.NAME_SYSTEM_TYPE, "UNIX Type: L8");
    }
}
