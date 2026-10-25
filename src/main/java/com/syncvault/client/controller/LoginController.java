package com.syncvault.client.controller;

import com.syncvault.client.network.FtpControlClient;
import com.syncvault.shared.constant.SystemConfig;
import com.syncvault.shared.exception.FtpProtocolException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

// Bộ điều khiển màn hình đăng nhập Client
public class LoginController {

    @FXML private RadioButton serverARadio;
    @FXML private RadioButton serverBRadio;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        errorLabel.setText("");
    }

    @FXML
    public void handleSelectServerA(ActionEvent event) {
        hostField.setText("127.0.0.1");
        portField.setText(String.valueOf(SystemConfig.DEFAULT_SERVER_A_PORT));
    }

    @FXML
    public void handleSelectServerB(ActionEvent event) {
        hostField.setText("127.0.0.1");
        portField.setText(String.valueOf(SystemConfig.DEFAULT_SERVER_B_PORT));
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (host.isEmpty() || portStr.isEmpty() || username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ thông tin máy chủ và tài khoản!");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            errorLabel.setText("Cổng kết nối không hợp lệ!");
            return;
        }

        errorLabel.setText("");
        statusLabel.setText("ĐANG KẾT NỐI...");
        loginButton.setDisable(true);

        new Thread(() -> {
            FtpControlClient client = new FtpControlClient();
            try {
                client.connect(host, port);
                client.login(username, password);

                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/client/main.fxml"));
                        Parent root = loader.load();
                        MainController mainCtrl = loader.getController();
                        mainCtrl.initSession(client, (port == SystemConfig.DEFAULT_SERVER_A_PORT));

                        Stage stage = (Stage) loginButton.getScene().getWindow();
                        stage.setTitle("SyncVault - " + username + " @" + host + ":" + port);
                        stage.setScene(new Scene(root, 1080, 700));
                        stage.centerOnScreen();
                    } catch (IOException e) {
                        errorLabel.setText("Lỗi giao diện chính: " + e.getMessage());
                        loginButton.setDisable(false);
                    }
                });

            } catch (FtpProtocolException e) {
                Platform.runLater(() -> {
                    client.close();
                    statusLabel.setText("TỪ CHỐI TRUY CẬP");
                    errorLabel.setText("[LỖI FTP " + e.getReplyCode() + "] " + e.getMessage());
                    loginButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    client.close();
                    statusLabel.setText("MẤT KẾT NỐI");
                    errorLabel.setText("Không thể kết nối đến máy chủ " + host + ":" + port + ": " + e.getMessage());
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }
}
