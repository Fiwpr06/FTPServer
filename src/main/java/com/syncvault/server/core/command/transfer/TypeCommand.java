package com.syncvault.server.core.command.transfer;

import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.shared.constant.FtpReplyCode;

// Xử lý lệnh TYPE (A: ASCII, I: Binary/Image)
public class TypeCommand implements FtpCommand {
    @Override
    public void execute(ClientSession session, String argument) {
        if (!session.isAuthenticated()) {
            session.sendReply(FtpReplyCode.NOT_LOGGED_IN, "Please login with USER and PASS");
            return;
        }

        if (argument == null || argument.trim().isEmpty()) {
            session.sendReply(FtpReplyCode.SYNTAX_ERROR_PARAMETERS, "Type code required");
            return;
        }

        String type = argument.trim().toUpperCase();
        if (type.startsWith("A")) {
            session.setTransferType("A");
            session.sendReply(FtpReplyCode.COMMAND_OK, "Type set to ASCII");
        } else if (type.startsWith("I") || type.startsWith("L")) {
            session.setTransferType("I");
            session.sendReply(FtpReplyCode.COMMAND_OK, "Type set to BINARY (Image)");
        } else {
            session.sendReply(FtpReplyCode.PARAMETER_NOT_IMPLEMENTED, "Type not supported, using BINARY");
        }
    }
}
