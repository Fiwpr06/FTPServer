# SyncVault — Distributed File Management & Synchronization System
### Hệ thống Quản lý và Đồng bộ Tệp tin Phân tán (FTP RFC 959 + Java RMI)

**SyncVault** là giải pháp kết hợp toàn diện giữa **Lab 2 (Giao thức FTP Server/Client)** và **Lab 5 (Java RMI Đồng bộ Phân tán trên Hai Máy Chủ)** theo mô hình **Primary–Secondary**.

---

## 1. Các Tính Năng Đột Phá & Chuẩn Giao Thức

1. **Chuẩn giao thức FTP RFC 959:**
   * Tách bạch 2 kênh: **Control Connection** và **Data Connection (Passive Mode `PASV`)**.
   * Bộ lệnh hỗ trợ: `USER`, `PASS`, `PWD`, `CWD`, `CDUP`, `MKD`, `RMD`, `PASV`, `TYPE`, `LIST`, `RETR`, `STOR`, `DELE`, `SYST`, `FEAT`, `NOOP`, `QUIT`.
   * **Tương thích 100% với FileZilla Client thật:** Bạn có thể mở ứng dụng FileZilla trên máy và kết nối trực tiếp vào `127.0.0.1:2121` hoặc `127.0.0.1:2122` để demo.

2. **Linh hoạt lựa chọn Server theo IP & Độc quyền phiên (Exclusive Single-Session):**
   * Người dùng có quyền chọn kết nối vào IP Server A hoặc IP Server B.
   * **Một tài khoản chỉ được phép đăng nhập ở 1 server tại 1 thời điểm.** Nếu đang online tại Server A, Server B sẽ từ chối kết nối ngay lập tức với mã lỗi `530` và hiển thị chi tiết thời gian, IP đang giữ phiên.

3. **Ủy quyền thao tác ghi (Write-Forwarding từ B sang A qua RMI):**
   * Khi người dùng đăng nhập tại Server B (Secondary) và thực hiện các thao tác thay đổi dữ liệu (`STOR`, `DELE`, `MKD`), Server B ủy quyền sang Server A qua RMI (`IWriteForwarder`). Server A xử lý trên bản gốc và cập nhật CSDL master.

4. **Đồng bộ tức thời (Instant Replication từ A sang B qua RMI):**
   * Bất kỳ khi nào Server A có thay đổi dữ liệu, Server A lập tức kích hoạt sự kiện đẩy bản sao sang Server B qua RMI (`IReplicationService`) và truyền luồng dữ liệu tức thì mà không phải chờ đợi hàng đợi trễ.
   * Tính toàn vẹn dữ liệu được kiểm định $100\%$ bằng mã băm **SHA-256 Checksum**.

5. **Cơ sở dữ liệu MySQL thông minh:**
   * Quét tự động danh sách mật khẩu root (`root:""`, `root:"root"`, `root:"123456"`, `root:"admin"`).
   * Tự động tạo CSDL `syncvault_db_a` / `syncvault_db_b` và tự động thực thi `schema.sql`.
   * Fallback tự động sang `InMemoryDataStore` nếu máy chấm bài chưa bật MySQL.

6. **Giao diện YoRHa Client (Nier:Automata):**
   * Bảng màu Warm Sand `#d3cebf` / Deep Charcoal `#1c1b17` từ dự án `PeerChat`.

---

## 2. Hướng Dẫn Vận Hành

### Cách 1: Khởi động với cửa sổ Console (để quan sát log)
* **Khởi động Server A (Primary - 2121):** Chạy [`START-SERVER-A.bat`](file:///d:/Workspace/Practice/Java/FTPServer/START-SERVER-A.bat)
* **Khởi động Server B (Secondary - 2122):** Chạy [`START-SERVER-B.bat`](file:///d:/Workspace/Practice/Java/FTPServer/START-SERVER-B.bat)
* **Khởi chạy Client JavaFX:** Chạy [`START-CLIENT.bat`](file:///d:/Workspace/Practice/Java/FTPServer/START-CLIENT.bat)

### Cách 2: Khởi động ẩn Console (Chạy nền)
* Nhấp đúp vào [`START-CLIENT.vbs`](file:///d:/Workspace/Practice/Java/FTPServer/START-CLIENT.vbs) để mở ứng dụng Client không kèm cửa sổ cmd đen.

### Tài khoản thử nghiệm mặc định:
* `student01` / Mật khẩu: `123456`
* `admin` / Mật khẩu: `admin`
