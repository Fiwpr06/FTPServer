package com.syncvault.server.core.command;

import com.syncvault.server.core.session.ClientSession;

// Interface chuẩn cho tất cả các lệnh FTP theo mô hình Command Pattern
public interface FtpCommand {
    void execute(ClientSession session, String argument);
}
