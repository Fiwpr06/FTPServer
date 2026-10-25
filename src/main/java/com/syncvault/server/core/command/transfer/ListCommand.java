package com.syncvault.server.core.command.transfer;

import com.syncvault.server.ServerContext;
import com.syncvault.server.core.channel.PassiveDataChannel;
import com.syncvault.server.core.command.FtpCommand;
import com.syncvault.server.core.session.ClientSession;
import com.syncvault.server.security.PathValidator;
import com.syncvault.shared.constant.FtpReplyCode;
import com.syncvault.shared.model.FileInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

// Xử lý lệnh LIST liệt kê danh sách tệp tin và thư mục theo chuẩn UNIX
public class ListCommand implements FtpCommand {
    @Override
    public void execute(ClientSession session, String argument) {
        if (!session.isAuthenticated()) {
            session.sendReply(FtpReplyCode.NOT_LOGGED_IN, "Please login with USER and PASS");
            return;
        }

        PassiveDataChannel dataChannel = session.getPassiveDataChannel();
        if (dataChannel == null) {
            session.sendReply(FtpReplyCode.CANT_OPEN_DATA_CONNECTION, "Use PASV first");
            return;
        }

        ServerContext context = session.getContext();
        String username = session.getAuthenticatedUser();
        File userRootDir = new File(context.getStorageRoot(), username);
        if (!userRootDir.exists()) {
            userRootDir.mkdirs();
        }

        File targetDir = PathValidator.resolvePhysicalFile(userRootDir, session.getCurrentVirtualDir());
        if (targetDir == null || !targetDir.exists()) {
            session.sendReply(FtpReplyCode.FILE_UNAVAILABLE, "Directory not found");
            return;
        }

        session.sendReply(FtpReplyCode.FILE_STATUS_OK_OPEN_DATA, "Opening ASCII mode data connection for file list");

        try (Socket dataSocket = dataChannel.acceptDataSocket();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream(), StandardCharsets.UTF_8))) {

            File[] files = targetDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault());
                    FileInfo info = new FileInfo(
                            file.getName(),
                            session.getCurrentVirtualDir() + "/" + file.getName(),
                            file.length(),
                            file.isDirectory(),
                            ldt,
                            ""
                    );
                    writer.write(info.toUnixListFormat(username) + "\r\n");
                }
            }
            writer.flush();
            session.sendReply(FtpReplyCode.CLOSING_DATA_CONNECTION, "Transfer complete");
        } catch (Exception e) {
            session.sendReply(FtpReplyCode.CONNECTION_CLOSED_TRANSFER_ABORTED, "Data transfer failed: " + e.getMessage());
        } finally {
            session.setPassiveDataChannel(null);
        }
    }
}
