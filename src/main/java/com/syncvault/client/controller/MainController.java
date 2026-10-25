package com.syncvault.client.controller;

import com.syncvault.client.model.LocalFileItem;
import com.syncvault.client.model.RemoteFileItem;
import com.syncvault.client.network.FtpControlClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

// Bộ điều khiển màn hình làm việc chính của Client
public class MainController {

    @FXML private Label serverLabel;
    @FXML private Label userLabel;
    @FXML private Label serverRolePill;
    @FXML private Label localPathLabel;
    @FXML private Label remotePathLabel;
    @FXML private Label transferStatusLabel;
    @FXML private Label transferPercentLabel;
    @FXML private ProgressBar transferProgressBar;
    @FXML private Label footerStatusLabel;

    @FXML private TableView<LocalFileItem> localTableView;
    @FXML private TableColumn<LocalFileItem, String> colLocalName;
    @FXML private TableColumn<LocalFileItem, String> colLocalSize;
    @FXML private TableColumn<LocalFileItem, String> colLocalDate;

    @FXML private TableView<RemoteFileItem> remoteTableView;
    @FXML private TableColumn<RemoteFileItem, String> colRemoteName;
    @FXML private TableColumn<RemoteFileItem, String> colRemoteSize;
    @FXML private TableColumn<RemoteFileItem, String> colRemotePerms;
    @FXML private TableColumn<RemoteFileItem, String> colRemoteDate;

    private FtpControlClient client;
    private File currentLocalDir;
    private boolean isPrimaryNode;

    @FXML
    public void initialize() {
        // Cấu hình cột bảng cục bộ
        colLocalName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colLocalSize.setCellValueFactory(new PropertyValueFactory<>("formattedSize"));
        colLocalDate.setCellValueFactory(new PropertyValueFactory<>("lastModified"));

        // Cấu hình cột bảng từ xa
        colRemoteName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRemoteSize.setCellValueFactory(new PropertyValueFactory<>("formattedSize"));
        colRemotePerms.setCellValueFactory(new PropertyValueFactory<>("permissions"));
        colRemoteDate.setCellValueFactory(new PropertyValueFactory<>("lastModifiedRaw"));

        // Thư mục cục bộ mặc định: user.home/SyncVault_Local
        this.currentLocalDir = new File(System.getProperty("user.home"), "SyncVault_Local");
        if (!currentLocalDir.exists()) {
            currentLocalDir.mkdirs();
        }
        loadLocalDirectory();
    }

    public void initSession(FtpControlClient client, boolean isPrimaryNode) {
        this.client = client;
        this.isPrimaryNode = isPrimaryNode;

        serverLabel.setText(client.getCurrentServerHost() + ":" + client.getCurrentServerPort());
        userLabel.setText(client.getCurrentUsername());

        if (isPrimaryNode) {
            serverRolePill.setText("PRIMARY (A)");
            serverRolePill.setStyle("-fx-background-color: #276620; -fx-text-fill: #ffffff;");
        } else {
            serverRolePill.setText("SECONDARY (B)");
            serverRolePill.setStyle("-fx-background-color: #1f4f6e; -fx-text-fill: #ffffff;");
        }

        refreshRemoteFiles();
    }

    private void loadLocalDirectory() {
        localPathLabel.setText(currentLocalDir.getAbsolutePath());
        File[] files = currentLocalDir.listFiles();
        List<LocalFileItem> items = FXCollections.observableArrayList();
        if (files != null) {
            for (File f : files) {
                items.add(new LocalFileItem(f));
            }
        }
        localTableView.setItems(FXCollections.observableArrayList(items));
    }

    public void refreshRemoteFiles() {
        new Thread(() -> {
            try {
                String pwd = client.printWorkingDirectory();
                List<RemoteFileItem> remoteFiles = client.listFiles();
                Platform.runLater(() -> {
                    remotePathLabel.setText(pwd);
                    remoteTableView.setItems(FXCollections.observableArrayList(remoteFiles));
                    footerStatusLabel.setText("Danh sách tệp tin đã được cập nhật thành công");
                });
            } catch (Exception e) {
                Platform.runLater(() -> footerStatusLabel.setText("Lỗi duyệt file máy chủ: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleBrowseLocalDir(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Chọn thư mục máy tính cục bộ");
        chooser.setInitialDirectory(currentLocalDir);
        File selected = chooser.showDialog(localTableView.getScene().getWindow());
        if (selected != null && selected.exists()) {
            this.currentLocalDir = selected;
            loadLocalDirectory();
        }
    }

    @FXML
    public void handleRefreshLocal(ActionEvent event) {
        loadLocalDirectory();
    }

    @FXML
    public void handleRefreshRemote(ActionEvent event) {
        refreshRemoteFiles();
    }

    @FXML
    public void handleUpload(ActionEvent event) {
        LocalFileItem selected = localTableView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isDirectory()) {
            showAlert("Thông báo", "Vui lòng chọn 1 tệp tin cục bộ để tải lên!");
            return;
        }

        File fileToUpload = selected.getFile();
        transferStatusLabel.setText("ĐANG TẢI LÊN: " + fileToUpload.getName());
        transferProgressBar.setProgress(0.0);

        new Thread(() -> {
            try {
                client.uploadFile(fileToUpload, fileToUpload.getName(), (transferred, total, pct) -> {
                    Platform.runLater(() -> {
                        transferProgressBar.setProgress(pct);
                        transferPercentLabel.setText(String.format("%.1f%%", pct * 100));
                    });
                });
                Platform.runLater(() -> {
                    transferStatusLabel.setText("TẢI LÊN THÀNH CÔNG: " + fileToUpload.getName());
                    transferProgressBar.setProgress(1.0);
                    transferPercentLabel.setText("100%");
                    refreshRemoteFiles();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    transferStatusLabel.setText("LỖI TẢI LÊN: " + e.getMessage());
                    showAlert("Lỗi tải lên", e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    public void handleDownload(ActionEvent event) {
        RemoteFileItem selected = remoteTableView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isDirectory()) {
            showAlert("Thông báo", "Vui lòng chọn 1 tệp tin từ xa để tải về!");
            return;
        }

        File destFile = new File(currentLocalDir, selected.getName());
        transferStatusLabel.setText("ĐANG TẢI VỀ: " + selected.getName());
        transferProgressBar.setProgress(0.0);

        new Thread(() -> {
            try {
                client.downloadFile(selected.getName(), destFile, (transferred, total, pct) -> {
                    Platform.runLater(() -> {
                        transferProgressBar.setProgress(pct);
                        transferPercentLabel.setText(String.format("%.1f%%", pct * 100));
                    });
                });
                Platform.runLater(() -> {
                    transferStatusLabel.setText("TẢI VỀ HOÀN TẤT: " + selected.getName());
                    transferProgressBar.setProgress(1.0);
                    transferPercentLabel.setText("100%");
                    loadLocalDirectory();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    transferStatusLabel.setText("LỖI TẢI VỀ: " + e.getMessage());
                    showAlert("Lỗi tải về", e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    public void handleDeleteRemote(ActionEvent event) {
        RemoteFileItem selected = remoteTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Thông báo", "Vui lòng chọn tệp tin cần xóa trên máy chủ!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Bạn có chắc chắn muốn xóa tệp: " + selected.getName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
        if (confirm.getResult() == ButtonType.YES) {
            new Thread(() -> {
                try {
                    client.deleteFile(selected.getName());
                    Platform.runLater(() -> {
                        footerStatusLabel.setText("Đã xóa tệp: " + selected.getName());
                        refreshRemoteFiles();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Lỗi xóa tệp", e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    public void handleNewFolder(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog("new_folder");
        dialog.setTitle("Tạo thư mục mới");
        dialog.setHeaderText("Nhập tên thư mục muốn tạo trên máy chủ:");
        dialog.showAndWait().ifPresent(folderName -> {
            if (!folderName.trim().isEmpty()) {
                new Thread(() -> {
                    try {
                        client.makeDirectory(folderName.trim());
                        Platform.runLater(() -> {
                            footerStatusLabel.setText("Đã tạo thư mục: " + folderName);
                            refreshRemoteFiles();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Lỗi tạo thư mục", e.getMessage()));
                    }
                }).start();
            }
        });
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        if (client != null) {
            client.close();
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/client/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) localTableView.getScene().getWindow();
            stage.setTitle("SyncVault - Login");
            stage.setScene(new Scene(root, 580, 480));
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
