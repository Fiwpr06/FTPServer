package com.syncvault.server;

import com.syncvault.server.controller.ServerController;
import com.syncvault.shared.constant.SystemConfig;
import com.syncvault.shared.model.ServerMode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.charset.StandardCharsets;
import java.io.PrintStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

// Ứng dụng Quản trị Máy chủ SyncVault Server Dashboard (JavaFX UI/UX)
public class ServerApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Thiết lập mã hóa UTF-8 cho toàn bộ luồng console và logging
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
            for (Handler h : Logger.getLogger("").getHandlers()) {
                if (h instanceof ConsoleHandler) {
                    h.setEncoding("UTF-8");
                }
            }
        } catch (Exception ignored) {}

        String modeArg = "primary";
        String peerHost = "127.0.0.1";

        Parameters params = getParameters();
        if (params != null) {
            for (String arg : params.getRaw()) {
                if (arg.equalsIgnoreCase("--secondary") || arg.equalsIgnoreCase("-s")) {
                    modeArg = "secondary";
                } else if (arg.startsWith("--peer=")) {
                    peerHost = arg.substring("--peer=".length()).trim();
                } else if (!arg.startsWith("-")) {
                    peerHost = arg.trim();
                }
            }
        }

        boolean isPrimary = modeArg.equalsIgnoreCase("primary");
        String serverId = isPrimary ? SystemConfig.SERVER_A_ID : SystemConfig.SERVER_B_ID;
        ServerMode mode = isPrimary ? ServerMode.PRIMARY : ServerMode.SECONDARY;
        int ftpPort = isPrimary ? SystemConfig.DEFAULT_SERVER_A_PORT : SystemConfig.DEFAULT_SERVER_B_PORT;
        int rmiPort = isPrimary ? SystemConfig.DEFAULT_SERVER_A_RMI_PORT : SystemConfig.DEFAULT_SERVER_B_RMI_PORT;
        int peerRmiPort = isPrimary ? SystemConfig.DEFAULT_SERVER_B_RMI_PORT : SystemConfig.DEFAULT_SERVER_A_RMI_PORT;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/server/server.fxml"));
        Parent root = loader.load();

        ServerController controller = loader.getController();
        controller.configure(serverId, mode, ftpPort, rmiPort, peerHost, peerRmiPort);

        Scene scene = new Scene(root, 1020, 680);
        stage.setScene(scene);
        stage.setTitle(String.format("SyncVault - Trung tâm Điều hành Máy chủ [%s // %s]", serverId, mode));
        stage.setMinWidth(800);
        stage.setMinHeight(520);

        stage.setOnCloseRequest(e -> {
            controller.cleanup();
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
