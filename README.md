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

## 2. Hướng Dẫn Vận Hành & Khởi Động

Hệ thống hỗ trợ 2 chế độ khởi động linh hoạt:
* **Khởi động kèm Console (`.bat`):** Dành cho kiểm thử, chấm điểm và theo dõi chi tiết log kết nối mạng. Tự động giải phóng cổng nếu có phiên cũ bị treo.
* **Khởi động Ẩn Console (`.vbs`):** Chạy ứng dụng ngầm không hiện cửa sổ CMD đen (như kiến trúc dự án PeerChat).

### Kịch bản 1: Chạy cả Server A và Server B trên CÙNG 1 MÁY

1. **Khởi động Server A (Primary):**
   * Nhấp đúp [`START-SERVER-A.bat`](START-SERVER-A.bat) (hoặc [`START-SERVER-A.vbs`](START-SERVER-A.vbs)).
   * Server A lắng nghe tại FTP Port `2121` và RMI Port `1099`.
2. **Khởi động Server B (Secondary):**
   * Nhấp đúp [`START-SERVER-B.bat`](START-SERVER-B.bat) (hoặc [`START-SERVER-B.vbs`](START-SERVER-B.vbs)).
   * Khi được hỏi IP Server A, nhấn `ENTER` để chọn mặc định `127.0.0.1`.
   * Server B lắng nghe tại FTP Port `2122` và RMI Port `1100`, tự động liên kết tới Server A.
3. **Khởi động Client JavaFX:**
   * Nhấp đúp [`START-CLIENT.vbs`](START-CLIENT.vbs) (hoặc [`START-CLIENT.bat`](START-CLIENT.bat)).
   * Giao diện đăng nhập hiện lên, chọn kết nối vào Server A hoặc Server B.
4. **Dừng toàn bộ hệ thống:**
   * Nhấp đúp [`STOP-ALL.bat`](STOP-ALL.bat) để tắt sạch toàn bộ các Server và giải phóng toàn bộ cổng mạng.

### Kịch bản 2: Chạy trên HAI MÁY TÍNH KHÁC NHAU (Qua mạng LAN)

1. **Trên Máy A (Server A):**
   * Nhấp đúp [`START-SERVER-A.bat`](START-SERVER-A.bat).
   * Màn hình sẽ hiển thị địa chỉ IP LAN của Máy A (ví dụ: `192.168.1.15`).
2. **Trên Máy B (Server B):**
   * Nhấp đúp [`START-SERVER-B.bat`](START-SERVER-B.bat) (hoặc [`START-SERVER-B.vbs`](START-SERVER-B.vbs)).
   * Nhập địa chỉ IP của Máy A (ví dụ: `192.168.1.15`) rồi nhấn `ENTER`.
   * Server B tự động kết nối và bắt tay 2 chiều với Server A.
3. **Trên Máy Client (Máy A, Máy B hoặc Máy C trong cùng mạng LAN):**
   * Khởi chạy [`START-CLIENT.vbs`](START-CLIENT.vbs).
   * Nhập địa chỉ IP của Server muốn kết nối (IP Máy A với cổng 2121, hoặc IP Máy B với cổng 2122).

---

## 3. Tài Khoản Kiểm Thử Mặc Định

| Tên đăng nhập (User) | Mật khẩu (Password) | Vai trò | Quyền hạn |
|---|---|---|---|
| `student01` | `123456` | Sinh viên | Đọc/Ghi thư mục cá nhân & công cộng |
| `student02` | `123456` | Sinh viên | Đọc/Ghi thư mục cá nhân & công cộng |
| `admin` | `admin123` | Quản trị viên | Toàn quyền trên toàn hệ thống |
