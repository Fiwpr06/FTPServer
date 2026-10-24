package com.syncvault.server.core.session;

// Trạng thái phiên kết nối của một Client với FTP Server
public enum SessionState {
    CONNECTED,          // Vừa kết nối qua Control Socket, chưa gửi USER
    NEED_PASSWORD,      // Đã gửi USER hợp lệ, đang chờ lệnh PASS
    AUTHENTICATED,      // Đã đăng nhập thành công, sẵn sàng thực hiện mọi lệnh
    TRANSFERRING,       // Đang trong tiến trình truyền dữ liệu (LIST, RETR, STOR)
    CLOSED              // Đã đóng phiên (sau lệnh QUIT hoặc mất kết nối)
}
