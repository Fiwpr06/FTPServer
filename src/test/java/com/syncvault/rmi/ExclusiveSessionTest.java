package com.syncvault.rmi;

import com.syncvault.client.network.FtpControlClient;
import com.syncvault.server.ServerContext;
import com.syncvault.server.core.FtpServer;
import com.syncvault.server.db.store.InMemoryDataStore;
import com.syncvault.server.rmi.registry.RmiRegistryManager;
import com.syncvault.shared.exception.FtpProtocolException;
import com.syncvault.shared.model.ServerMode;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExclusiveSessionTest {

    private File storageA;
    private File storageB;

    private ServerContext contextA;
    private ServerContext contextB;

    private FtpServer serverA;
    private FtpServer serverB;

    private int ftpPortA = 22131;
    private int ftpPortB = 22132;
    private int rmiPortA = 12101;
    private int rmiPortB = 12102;

    @BeforeAll
    void setupCluster() throws Exception {
        storageA = Files.createTempDirectory("syncvault_storage_a").toFile();
        storageB = Files.createTempDirectory("syncvault_storage_b").toFile();

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

        // Khởi động RMI Local
        RmiRegistryManager rmiA = new RmiRegistryManager(contextA);
        rmiA.startLocalServices();

        RmiRegistryManager rmiB = new RmiRegistryManager(contextB);
        rmiB.startLocalServices();

        // Kết nối chéo RMI Peer
        rmiA.connectToPeer();
        rmiB.connectToPeer();

        // Khởi động FTP Servers
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
    void testSingleSessionRule_AccountCannotLoginToBothServersSimultaneously() throws Exception {
        // 1. Client 1 đăng nhập vào Server A
        FtpControlClient client1 = new FtpControlClient();
        client1.connect("127.0.0.1", ftpPortA);
        String replyA = client1.login("student01", "123456");
        assertTrue(replyA.startsWith("230"), "Client 1 đăng nhập Server A phải thành công");
        assertTrue(client1.isLoggedIn());

        // 2. Client 2 cố gắng đăng nhập cùng tài khoản student01 vào Server B (khi phiên A vẫn đang mở)
        FtpControlClient client2 = new FtpControlClient();
        client2.connect("127.0.0.1", ftpPortB);

        FtpProtocolException ex = assertThrows(FtpProtocolException.class, () -> {
            client2.login("student01", "123456");
        }, "Server B phải ném ngoại lệ từ chối đăng nhập khi tài khoản đang ở Server A");

        assertEquals(530, ex.getReplyCode(), "Mã phản hồi từ chối phải là 530");
        assertTrue(ex.getMessage().contains("SERVER_A"), "Thông báo lỗi phải chỉ rõ đang đăng nhập tại SERVER_A");
        assertFalse(client2.isLoggedIn());
        client2.close();

        // 3. Client 1 đăng xuất khỏi Server A
        client1.close();
        assertFalse(client1.isLoggedIn());

        // 4. Client 3 đăng nhập vào Server B sau khi Client 1 đã giải phóng phiên
        FtpControlClient client3 = new FtpControlClient();
        client3.connect("127.0.0.1", ftpPortB);
        String replyB = client3.login("student01", "123456");
        assertTrue(replyB.startsWith("230"), "Client 3 đăng nhập Server B phải thành công sau khi phiên Server A được giải phóng");
        assertTrue(client3.isLoggedIn());

        client3.close();
    }
}
