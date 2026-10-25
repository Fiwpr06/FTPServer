package com.syncvault.rmi;

import com.syncvault.client.network.FtpControlClient;
import com.syncvault.server.ServerContext;
import com.syncvault.server.core.FtpServer;
import com.syncvault.server.db.store.InMemoryDataStore;
import com.syncvault.server.rmi.registry.RmiRegistryManager;
import com.syncvault.shared.model.ServerMode;
import com.syncvault.shared.util.ChecksumUtil;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InstantReplicationTest {

    private File storageA;
    private File storageB;
    private File localClientDir;

    private ServerContext contextA;
    private ServerContext contextB;

    private FtpServer serverA;
    private FtpServer serverB;

    private int ftpPortA = 22141;
    private int ftpPortB = 22142;
    private int rmiPortA = 12111;
    private int rmiPortB = 12112;

    @BeforeAll
    void setupCluster() throws Exception {
        storageA = Files.createTempDirectory("syncvault_repl_storage_a").toFile();
        storageB = Files.createTempDirectory("syncvault_repl_storage_b").toFile();
        localClientDir = Files.createTempDirectory("syncvault_repl_client").toFile();

        InMemoryDataStore dbA = new InMemoryDataStore();
        dbA.init();
        InMemoryDataStore dbB = new InMemoryDataStore();
        dbB.init();

        contextA = new ServerContext("SERVER_A", ServerMode.PRIMARY, ftpPortA, rmiPortA, storageA, dbA);
        contextA.setPeerHost("127.0.0.1");
        contextA.setPeerRmiPort(rmiPortB);
        contextA.setPeerServerId("SERVER_B");

        contextB = new ServerContext("SERVER_B", ServerMode.SECONDARY, ftpPortB, rmiPortB, storageB, dbB);
        contextB.setPeerHost("127.0.0.1");
        contextB.setPeerRmiPort(rmiPortA);
        contextB.setPeerServerId("SERVER_A");

        RmiRegistryManager rmiA = new RmiRegistryManager(contextA);
        rmiA.startLocalServices();

        RmiRegistryManager rmiB = new RmiRegistryManager(contextB);
        rmiB.startLocalServices();

        rmiA.connectToPeer();
        rmiB.connectToPeer();

        serverA = new FtpServer(contextA);
        serverA.start();

        serverB = new FtpServer(contextB);
        serverB.start();
    }

    @AfterAll
    void tearDown() throws IOException {
        if (serverA != null) serverA.stop();
        if (serverB != null) serverB.stop();

        deleteDir(storageA);
        deleteDir(storageB);
        deleteDir(localClientDir);
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
    void testInstantReplication_UploadToServerAAppearsImmediatelyOnServerB() throws Exception {
        FtpControlClient client = new FtpControlClient();
        client.connect("127.0.0.1", ftpPortA);
        client.login("student01", "123456");

        // Tạo file tại máy khách
        File uploadFile = new File(localClientDir, "important_contract.pdf");
        try (FileWriter fw = new FileWriter(uploadFile)) {
            fw.write("CONFIDENTIAL REAL-TIME REPLICATION DATA - SHA256 INTEGRITY VERIFIED");
        }

        // Upload lên Server A
        client.uploadFile(uploadFile, "important_contract.pdf", null);
        client.close();

        // 1. Kiểm tra file trên Server A
        File fileOnServerA = new File(new File(storageA, "student01"), "important_contract.pdf");
        assertTrue(fileOnServerA.exists(), "File phải tồn tại trên Server A");
        String sha256A = ChecksumUtil.calculateSHA256(fileOnServerA);

        // 2. Kiểm tra file trên Server B: PHẢI TỒN TẠI NGAY LẬP TỨC KHÔNG PHẢI CHỜ ĐỢI
        File fileOnServerB = new File(new File(storageB, "student01"), "important_contract.pdf");
        assertTrue(fileOnServerB.exists(), "File phải xuất hiện ngay lập tức trên Server B");
        String sha256B = ChecksumUtil.calculateSHA256(fileOnServerB);

        // 3. Đối chiếu mã băm toàn vẹn SHA-256
        assertEquals(sha256A, sha256B, "Mã SHA-256 giữa Server A và Server B phải khớp nhau 100%");
        assertEquals(uploadFile.length(), fileOnServerB.length(), "Kích thước file bản sao phải khớp bản gốc");
    }

    @Test
    void testInstantReplication_DeleteOnServerAIsImmediatelyDeletedOnServerB() throws Exception {
        FtpControlClient client = new FtpControlClient();
        client.connect("127.0.0.1", ftpPortA);
        client.login("student01", "123456");

        File fileToDelete = new File(localClientDir, "file_to_delete.txt");
        try (FileWriter fw = new FileWriter(fileToDelete)) {
            fw.write("Delete me soon");
        }
        client.uploadFile(fileToDelete, "file_to_delete.txt", null);

        File replicaOnB = new File(new File(storageB, "student01"), "file_to_delete.txt");
        assertTrue(replicaOnB.exists(), "Bản sao trên B phải có trước khi xóa");

        // Client gọi lệnh xóa trên Server A
        client.deleteFile("file_to_delete.txt");
        client.close();

        // Kiểm tra trên Server B: Phải bị xóa tức thời
        assertFalse(replicaOnB.exists(), "Bản sao trên Server B phải bị xóa tức thời khi Server A xóa");
    }
}
