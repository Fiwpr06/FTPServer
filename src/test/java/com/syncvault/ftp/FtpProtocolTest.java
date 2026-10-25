package com.syncvault.ftp;

import com.syncvault.client.model.RemoteFileItem;
import com.syncvault.client.network.FtpControlClient;
import com.syncvault.server.ServerContext;
import com.syncvault.server.core.FtpServer;
import com.syncvault.server.db.store.InMemoryDataStore;
import com.syncvault.shared.model.ServerMode;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FtpProtocolTest {

    private File tempStorage;
    private File tempClientDir;
    private FtpServer server;
    private ServerContext context;
    private int testFtpPort = 22121;

    @BeforeAll
    void setupServer() throws IOException {
        tempStorage = Files.createTempDirectory("syncvault_test_server_storage").toFile();
        tempClientDir = Files.createTempDirectory("syncvault_test_client_storage").toFile();

        InMemoryDataStore dataStore = new InMemoryDataStore();
        dataStore.init();

        context = new ServerContext("SERVER_TEST", ServerMode.PRIMARY, testFtpPort, 12099, tempStorage, dataStore);
        server = new FtpServer(context);
        assertTrue(server.start(), "Server phải khởi động thành công");
    }

    @AfterAll
    void tearDown() throws IOException {
        if (server != null) {
            server.stop();
        }
        deleteDir(tempStorage);
        deleteDir(tempClientDir);
    }

    private void deleteDir(File dir) throws IOException {
        if (dir != null && dir.exists()) {
            Files.walk(dir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(java.nio.file.Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    void testFtpLifecycle_ConnectLoginListUploadDownloadQuit() throws Exception {
        FtpControlClient client = new FtpControlClient();
        client.connect("127.0.0.1", testFtpPort);
        assertTrue(client.isConnected(), "Client phải kết nối thành công");

        // Đăng nhập
        String reply = client.login("student01", "123456");
        assertTrue(reply.startsWith("230"), "Đăng nhập phải trả về mã 230");
        assertTrue(client.isLoggedIn());

        // Kiểm tra PWD
        String pwd = client.printWorkingDirectory();
        assertEquals("/", pwd);

        // Tạo thư mục mới
        client.makeDirectory("test_folder");

        // Tạo file cục bộ để upload
        File uploadFile = new File(tempClientDir, "sample_report.txt");
        try (FileWriter fw = new FileWriter(uploadFile)) {
            fw.write("SYNCVAULT DISTRIBUTED FILE REPLICATION TEST CONTENT - 2026");
        }

        // Upload
        client.uploadFile(uploadFile, "sample_report.txt", null);

        // List files
        List<RemoteFileItem> remoteFiles = client.listFiles();
        assertFalse(remoteFiles.isEmpty(), "Danh sách file từ xa không được rỗng");
        boolean foundUploaded = remoteFiles.stream().anyMatch(f -> f.getName().equals("sample_report.txt"));
        assertTrue(foundUploaded, "Phải tìm thấy sample_report.txt trên máy chủ");

        // Download về máy
        File downloadedFile = new File(tempClientDir, "downloaded_sample.txt");
        client.downloadFile("sample_report.txt", downloadedFile, null);
        assertTrue(downloadedFile.exists(), "File tải về phải tồn tại");
        assertEquals(uploadFile.length(), downloadedFile.length(), "Kích thước file tải về phải khớp");

        // Xóa file trên server
        client.deleteFile("sample_report.txt");

        // Ngắt kết nối
        client.close();
        assertFalse(client.isLoggedIn());
    }
}
