package com.syncvault.server.core.command.transfer;

import com.syncvault.server.core.channel.PassiveDataChannel;
import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.shared.constant.FtpReplyCode;

import java.io.IOException;

// Xử lý lệnh PASV (Passive Mode)
public class PasvCommand implements FtpCommand {
    @Override
    public void execute(ClientSession session, String argument) {
        if (!session.isAuthenticated()) {
            session.sendReply(FtpReplyCode.NOT_LOGGED_IN, "Please login with USER and PASS");
            return;
        }

        try {
            PassiveDataChannel channel = new PassiveDataChannel();
            session.setPassiveDataChannel(channel);
            session.sendReply(FtpReplyCode.ENTERING_PASSIVE_MODE, "Entering Passive Mode " + channel.getPasvReplyString());
        } catch (IOException e) {
            session.sendReply(FtpReplyCode.CANT_OPEN_DATA_CONNECTION, "Cannot open passive data connection");
        }
    }
}
