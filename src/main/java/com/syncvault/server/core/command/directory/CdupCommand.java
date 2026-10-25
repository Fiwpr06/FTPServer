package com.syncvault.server.core.command.directory;

import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;

// Xử lý lệnh CDUP (Change to Parent Directory)
public class CdupCommand implements FtpCommand {
    private final CwdCommand cwdCommand = new CwdCommand();

    @Override
    public void execute(ClientSession session, String argument) {
        cwdCommand.execute(session, "..");
    }
}
