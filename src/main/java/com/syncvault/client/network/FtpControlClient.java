package com.syncvault.client.network;

import com.syncvault.client.model.RemoteFileItem;
import com.syncvault.client.service.FileTransferProgressCallback;
import com.syncvault.shared.constant.SystemConfig;
import com.syncvault.shared.exception.FtpProtocolException;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Máy khách FTP điều khiển kênh Control và phối hợp kênh Data
public class FtpControlClient implements Closeable {
    private Socket controlSocket;
    private BufferedReader reader;
    private BufferedWriter writer;

    private String currentServerHost;
    private int currentServerPort;
    private String currentUsername;
    private boolean loggedIn = false;

    public void connect(String host, int port) throws IOException, FtpProtocolException {
        this.currentServerHost = host;
        this.currentServerPort = port;
        this.controlSocket = new Socket(host, port);
        this.controlSocket.setSoTimeout(15000);
        this.reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream(), StandardCharsets.UTF_8));

        // Đọc thông điệp chào mừng 220
        String reply = readReply();
        if (!reply.startsWith("220")) {
            throw new FtpProtocolException(parseReplyCode(reply), "Server chào mừng không hợp lệ: " + reply);
        }
    }

    public String login(String username, String password) throws IOException, FtpProtocolException {
        sendCommand("USER " + username);
        String userReply = readReply();
        if (!userReply.startsWith("331")) {
            throw new FtpProtocolException(parseReplyCode(userReply), userReply);
        }

        sendCommand("PASS " + password);
        String passReply = readReply();
        if (passReply.startsWith("230")) {
            this.currentUsername = username;
            this.loggedIn = true;
            return passReply;
        } else {
            throw new FtpProtocolException(parseReplyCode(passReply), passReply);
        }
    }

    public synchronized void sendCommand(String command) throws IOException {
        if (writer == null) throw new IOException("Chưa kết nối tới máy chủ");
        writer.write(command + "\r\n");
        writer.flush();
    }

    public synchronized String readReply() throws IOException {
        if (reader == null) throw new IOException("Chưa kết nối tới máy chủ");
        String line = reader.readLine();
        if (line == null) throw new IOException("Kết nối bị ngắt từ máy chủ");
        return line;
    }

    private int parseReplyCode(String reply) {
        try {
            return Integer.parseInt(reply.substring(0, 3));
        } catch (Exception e) {
            return 500;
        }
    }

    // Mở kênh Passive Mode và trả về mảng [Host, Port] của Data Channel
    public int[] enterPassiveMode() throws IOException, FtpProtocolException {
        sendCommand("PASV");
        String reply = readReply();
        if (!reply.startsWith("227")) {
            throw new FtpProtocolException(parseReplyCode(reply), "Lỗi khi gọi lệnh PASV: " + reply);
        }

        // Bóc tách cú pháp (h1,h2,h3,h4,p1,p2)
        Pattern pattern = Pattern.compile("\\((\\d+),(\\d+),(\\d+),(\\d+),(\\d+),(\\d+)\\)");
        Matcher matcher = pattern.matcher(reply);
        if (matcher.find()) {
            int p1 = Integer.parseInt(matcher.group(5));
            int p2 = Integer.parseInt(matcher.group(6));
            int dataPort = (p1 * 256) + p2;
            return new int[]{dataPort};
        }
        throw new IOException("Không thể phân tích phản hồi PASV: " + reply);
    }

    public List<RemoteFileItem> listFiles() throws IOException, FtpProtocolException {
        int[] pasv = enterPassiveMode();
        int dataPort = pasv[0];

        try (Socket dataSocket = new Socket(currentServerHost, dataPort)) {
            dataSocket.setSoTimeout(15000);
            sendCommand("LIST");
            String startReply = readReply();
            if (!startReply.startsWith("150") && !startReply.startsWith("125")) {
                throw new FtpProtocolException(parseReplyCode(startReply), startReply);
            }

            List<RemoteFileItem> list = new ArrayList<>();
            try (BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = dataReader.readLine()) != null) {
                    RemoteFileItem item = parseUnixListLine(line);
                    if (item != null) {
                        list.add(item);
                    }
                }
            }

            String finishReply = readReply();
            return list;
        }
    }

    private RemoteFileItem parseUnixListLine(String line) {
        // Ví dụ: drwxr-xr-x 1 student01 student01 4096 Oct 26 12:00 my_folder
        String[] parts = line.split("\\s+", 9);
        if (parts.length >= 9) {
            String perms = parts[0];
            boolean isDir = perms.startsWith("d");
            long size = Long.parseLong(parts[4]);
            String date = parts[5] + " " + parts[6] + " " + parts[7];
            String name = parts[8];
            return new RemoteFileItem(name, size, isDir, perms, date);
        }
        return null;
    }

    public void uploadFile(File localFile, String remoteFileName, FileTransferProgressCallback callback) throws IOException, FtpProtocolException {
        sendCommand("TYPE I");
        readReply();

        int[] pasv = enterPassiveMode();
        int dataPort = pasv[0];

        try (Socket dataSocket = new Socket(currentServerHost, dataPort)) {
            dataSocket.setSoTimeout(20000);
            sendCommand("STOR " + remoteFileName);
            String startReply = readReply();
            if (!startReply.startsWith("150") && !startReply.startsWith("125")) {
                throw new FtpProtocolException(parseReplyCode(startReply), startReply);
            }

            long totalBytes = localFile.length();
            long transferred = 0;
            try (FileInputStream fis = new FileInputStream(localFile);
                 OutputStream os = dataSocket.getOutputStream()) {

                byte[] buffer = new byte[SystemConfig.BUFFER_SIZE];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                    transferred += read;
                    if (callback != null && totalBytes > 0) {
                        callback.onProgress(transferred, totalBytes, (double) transferred / totalBytes);
                    }
                }
                os.flush();
            }

            String finishReply = readReply();
            if (!finishReply.startsWith("226")) {
                throw new FtpProtocolException(parseReplyCode(finishReply), finishReply);
            }
        }
    }

    public void downloadFile(String remoteFileName, File localDestination, FileTransferProgressCallback callback) throws IOException, FtpProtocolException {
        sendCommand("TYPE I");
        readReply();

        int[] pasv = enterPassiveMode();
        int dataPort = pasv[0];

        try (Socket dataSocket = new Socket(currentServerHost, dataPort)) {
            dataSocket.setSoTimeout(20000);
            sendCommand("RETR " + remoteFileName);
            String startReply = readReply();
            if (!startReply.startsWith("150") && !startReply.startsWith("125")) {
                throw new FtpProtocolException(parseReplyCode(startReply), startReply);
            }

            long totalBytes = 0; // Kích thước có thể lấy từ phản hồi 150 nếu có
            long transferred = 0;
            try (InputStream is = dataSocket.getInputStream();
                 FileOutputStream fos = new FileOutputStream(localDestination)) {

                byte[] buffer = new byte[SystemConfig.BUFFER_SIZE];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                    transferred += read;
                    if (callback != null && totalBytes > 0) {
                        callback.onProgress(transferred, totalBytes, (double) transferred / totalBytes);
                    }
                }
                fos.flush();
            }

            String finishReply = readReply();
            if (!finishReply.startsWith("226")) {
                throw new FtpProtocolException(parseReplyCode(finishReply), finishReply);
            }
        }
    }

    public void deleteFile(String remoteFileName) throws IOException, FtpProtocolException {
        sendCommand("DELE " + remoteFileName);
        String reply = readReply();
        if (!reply.startsWith("250")) {
            throw new FtpProtocolException(parseReplyCode(reply), reply);
        }
    }

    public void makeDirectory(String dirName) throws IOException, FtpProtocolException {
        sendCommand("MKD " + dirName);
        String reply = readReply();
        if (!reply.startsWith("257") && !reply.startsWith("250")) {
            throw new FtpProtocolException(parseReplyCode(reply), reply);
        }
    }

    public void changeDirectory(String dir) throws IOException, FtpProtocolException {
        sendCommand("CWD " + dir);
        String reply = readReply();
        if (!reply.startsWith("250")) {
            throw new FtpProtocolException(parseReplyCode(reply), reply);
        }
    }

    public String printWorkingDirectory() throws IOException, FtpProtocolException {
        sendCommand("PWD");
        String reply = readReply();
        if (reply.startsWith("257")) {
            int start = reply.indexOf('"');
            int end = reply.lastIndexOf('"');
            if (start != -1 && end > start) {
                return reply.substring(start + 1, end);
            }
        }
        return "/";
    }

    public boolean isConnected() {
        return controlSocket != null && !controlSocket.isClosed();
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getCurrentServerHost() { return currentServerHost; }
    public int getCurrentServerPort() { return currentServerPort; }
    public String getCurrentUsername() { return currentUsername; }

    @Override
    public void close() {
        try {
            if (isConnected()) {
                sendCommand("QUIT");
            }
        } catch (Exception ignored) {}

        loggedIn = false;
        try { if (reader != null) reader.close(); } catch (Exception ignored) {}
        try { if (writer != null) writer.close(); } catch (Exception ignored) {}
        try { if (controlSocket != null) controlSocket.close(); } catch (Exception ignored) {}
    }
}
