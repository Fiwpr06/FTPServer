package com.syncvault.server.core.command.system;

import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.shared.constant.FtpReplyCode;

// Xử lý lệnh NOOP (Heartbeat giữ kết nối và cập nhật thời gian hoạt động vào CSDL)
public class NoopCommand implements FtpCommand {
    @Override
    public void execute(ClientSession session, String argument) {
        if (session.isAuthenticated()) {
            session.getContext().getDataStore().updateSessionHeartbeat(session.getAuthenticatedUser());
        }
        session.sendReply(FtpReplyCode.COMMAND_OK, "NOOP ok");
    }
}
