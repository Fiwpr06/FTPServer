package com.syncvault.server.core.command.transfer;

import com.syncvault.server.ServerContext;
import com.syncvault.server.core.channel.PassiveDataChannel;
import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.server.security.PathValidator;
import com.syncvault.shared.constant.FtpReplyCode;
import com.syncvault.shared.constant.SystemConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.Socket;

// Xử lý lệnh RETR (Tải file từ máy chủ về máy khách)
public class RetrCommand implements FtpCommand {
    @Override
    public void execute(ClientSession session, String argument) {
        if (!session.isAuthenticated()) {
            session.sendReply(FtpReplyCode.NOT_LOGGED_IN, "Please login with USER and PASS");
            return;
        }

        if (argument == null || argument.trim().isEmpty()) {
            session.sendReply(FtpReplyCode.SYNTAX_ERROR_PARAMETERS, "Filename required");
            return;
        }

        PassiveDataChannel dataChannel = session.getPassiveDataChannel();
        if (dataChannel == null) {
            session.sendReply(FtpReplyCode.CANT_OPEN_DATA_CONNECTION, "Use PASV first");
            return;
        }

        ServerContext context = session.getContext();
        String username = session.getAuthenticatedUser();
        String targetVirtual = PathValidator.normalizeVirtualPath(session.getCurrentVirtualDir(), argument.trim());
        File userRootDir = new File(context.getStorageRoot(), username);
        File physicalFile = PathValidator.resolvePhysicalFile(userRootDir, targetVirtual);

        if (physicalFile == null || !physicalFile.exists() || physicalFile.isDirectory()) {
            session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "File not found or is a directory");
            session.setPassiveDataChannel(null);
            return;
        }

        session.sendReply(FtpReplyCode.FILE_STATUS_OK_OPEN_DATA, String.format("Opening BINARY mode data connection for %s (%d bytes)",
                physicalFile.getName(), physicalFile.length()));

        try (Socket dataSocket = dataChannel.acceptDataSocket();
             FileInputStream fis = new FileInputStream(physicalFile);
             OutputStream os = dataSocket.getOutputStream()) {

            byte[] buffer = new byte[SystemConfig.BUFFER_SIZE];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
            session.sendReply(FtpReplyCode.CLOSING_DATA_CONNECTION, "Transfer complete");
        } catch (Exception e) {
            session.sendReply(FtpReplyCode.CONNECTION_CLOSED_TRANSFER_ABORTED, "Download aborted: " + e.getMessage());
        } finally {
            session.setPassiveDataChannel(null);
        }
    }
}
