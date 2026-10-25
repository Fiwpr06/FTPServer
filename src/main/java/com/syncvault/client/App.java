package com.syncvault.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Điểm khởi chạy ứng dụng SyncVault Client
public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/client/login.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("SyncVault // YoRHa File Transfer Client");
        primaryStage.setScene(new Scene(root, 580, 480));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
