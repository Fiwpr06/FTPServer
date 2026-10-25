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
public class WriteForwardingTest {

    private File storageA;
    private File storageB;
    private File localClientDir;

    private ServerContext contextA;
    private ServerContext contextB;

    private FtpServer serverA;
    private FtpServer serverB;

    private int ftpPortA = 22151;
    private int ftpPortB = 22152;
    private int rmiPortA = 12121;
    private int rmiPortB = 12122;

    @BeforeAll
    void setupCluster() throws Exception {
        storageA = Files.createTempDirectory("syncvault_fwd_storage_a").toFile();
        storageB = Files.createTempDirectory("syncvault_fwd_storage_b").toFile();
        localClientDir = Files.createTempDirectory("syncvault_fwd_client").toFile();

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
    void testWriteForwarding_UploadToServerBDelegatesToAAndReplicatesBack() throws Exception {
        // Client kết nối trực tiếp vào Server B (Secondary)
        FtpControlClient clientOnB = new FtpControlClient();
        clientOnB.connect("127.0.0.1", ftpPortB);
        clientOnB.login("student01", "123456");

        // Tạo file tại máy khách
        File uploadFile = new File(localClientDir, "delegated_upload.txt");
        try (FileWriter fw = new FileWriter(uploadFile)) {
            fw.write("TEST WRITE-FORWARDING: SERVER B DELEGATES WRITE TO SERVER A VIA RMI");
        }

        // Upload lên Server B
        clientOnB.uploadFile(uploadFile, "delegated_upload.txt", null);
        clientOnB.close();

        // 1. Kiểm tra trên Server A (Primary Master): Phải nhận và lưu file do Server B ủy quyền sang
        File fileOnMasterA = new File(new File(storageA, "student01"), "delegated_upload.txt");
        assertTrue(fileOnMasterA.exists(), "Server A phải lưu file master được Server B ủy quyền");
        String sha256A = ChecksumUtil.calculateSHA256(fileOnMasterA);

        // 2. Kiểm tra trên Server B (Secondary Replica): Phải có bản sao đồng bộ tức thời từ A
        File fileOnReplicaB = new File(new File(storageB, "student01"), "delegated_upload.txt");
        assertTrue(fileOnReplicaB.exists(), "Server B phải có bản sao tệp tin sau khi ủy quyền thành công");
        String sha256B = ChecksumUtil.calculateSHA256(fileOnReplicaB);

        assertEquals(sha256A, sha256B, "Mã băm SHA-256 giữa 2 server sau ủy quyền phải khớp hoàn toàn");
    }
}
