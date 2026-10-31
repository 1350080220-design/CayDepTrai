# VietGo – Quản lý đặt vé du lịch (Đề 5)

Nhóm phụ trách **2. Vé tàu hỏa** và **3. Vé xe khách**.
Công nghệ: **Spring Boot 3.3 · Spring Web MVC · Spring Data JPA · Spring Security · Thymeleaf · SQL Server**.

---

## 1. Chạy ứng dụng

**Yêu cầu:** JDK 17 trở lên, Maven 3.8+ (hoặc IntelliJ / Eclipse / NetBeans có sẵn Maven), SQL Server 2017+.

1. Mở SSMS, chạy `database/01_create_database.sql` để tạo database `VietGoDB`.
2. Mở `src/main/resources/application.properties`, sửa cho khớp máy của bạn:
   ```properties
   spring.datasource.username=sa
   spring.datasource.password=123456
   ```
   SQL Server phải bật **TCP/IP, cổng 1433** (SQL Server Configuration Manager → Protocols → TCP/IP → Enabled) và cho phép đăng nhập *SQL Server Authentication*.
3. Chạy:
   ```bash
   mvn spring-boot:run
   ```
   hoặc chạy hàm `main` trong `VietGoApplication.java` từ IDE.
4. Mở http://localhost:8080

Lần chạy đầu, Hibernate tự tạo bảng và `DataSeeder` sinh dữ liệu mẫu (≈1.300 chuyến tàu của 42 mác tàu, ≈5.100 chuyến xe của 31 tuyến trong 30 ngày tới, đơn đã thanh toán, lịch tư vấn) — vài giây với H2, khoảng 30–60 giây với SQL Server.

> Database SQL Server đã có dữ liệu từ phiên bản cũ sẽ **không** tự sinh thêm chuyến mới. Chạy `database/02_reset_data.sql` rồi khởi động lại để có bộ dữ liệu đầy đủ (hạng ngồi cứng, khoang VIP, xe cabin).

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Quản trị | `admin@vietgo.vn` | `Admin@123` |
| Khách hàng | `khachhang@vietgo.vn` | `Khach@123` |
| Khách hàng | `thuha@vietgo.vn` | `Khach@123` |

> Chạy thử nhanh không cần SQL Server: `mvn spring-boot:run -Dspring-boot.run.profiles=h2` (dữ liệu trong RAM, mất khi tắt). Bản nộp dùng cấu hình mặc định SQL Server.

Chạy kiểm thử đơn vị (thuật toán giá, validation, hoàn hủy): `mvn test`.

Muốn sinh lại dữ liệu mẫu: chạy `database/02_reset_data.sql` rồi khởi động lại ứng dụng.

---

## 2. Chức năng

**Khách hàng**
- **Bảng giờ khởi hành** ở trang chủ: 8 chuyến tàu/xe sắp chạy, chữ lật kiểu bảng ga, đồng hồ và đếm ngược giờ đi.
- **Nhiều hạng chỗ**: tàu có 5 hạng (ngồi cứng phổ thông, ngồi mềm, giường khoang 6, khoang 4, khoang VIP 2 giường); xe có 4 loại (ghế ngồi 45 chỗ, giường nằm 36 chỗ, phòng cabin 24 phòng, Limousine 9 chỗ). Mỗi hạng có nhãn Phổ thông / Tiêu chuẩn / Cao cấp / VIP và danh sách tiện ích.
- **Kết quả tìm kiếm**: giá và số chỗ còn của từng hạng ngay trên thẻ chuyến; sắp xếp theo giờ đi / giá / thời gian; lọc theo hạng toa hoặc loại xe (có hiệu ứng chuyển vị trí); xem nhanh lộ trình.
- **Bản đồ lộ trình** (trang chọn chỗ và trang đơn hàng): tuyến đường vẽ dần trên bản đồ, phương tiện chạy dọc lộ trình, dừng ở từng ga / trạm nghỉ; thanh tua thời gian, đồng hồ hành trình và danh sách điểm dừng đồng bộ với bản đồ.
- Đăng ký (kiểm tra email/SĐT trùng, mật khẩu mạnh, đồng hồ đo độ mạnh), đăng nhập, ghi nhớ đăng nhập, đăng xuất, đổi thông tin, đổi mật khẩu.
- **Đăng ký lịch hẹn tư vấn**: chọn loại vé, nội dung, hình thức, ngày + khung giờ (hiển thị số chỗ còn lại theo thời gian thực), xem / hủy lịch.
- **Đặt giữ chỗ**: tìm chuyến → chọn chỗ trên sơ đồ → khai báo hành khách → xem bảng giá chi tiết → giữ chỗ (có đồng hồ đếm ngược) → thanh toán → vé điện tử có mã QR, in vé.
- Hủy giữ chỗ (miễn phí) hoặc hủy vé đã thanh toán theo chính sách hoàn tiền của từng loại vé.

**Quản trị** (`/admin`)
- Bảng điều khiển: doanh thu ròng theo loại vé, biểu đồ 7 ngày, đơn mới, lịch tư vấn sắp tới.
- Thêm / sửa / tạm dừng bán chuyến tàu (cơ cấu toa) và chuyến xe (loại xe, giá, điểm đón trả).
- Quản lý đơn (lọc, tìm, hủy và hoàn 100%), xử lý lịch tư vấn, khóa / mở khóa tài khoản.

---

## 3. Nghiệp vụ riêng của từng loại vé

| | **Vé tàu hỏa** (`com.vietgo.train`) | **Vé xe khách** (`com.vietgo.bus`) |
|---|---|---|
| **Quy trình** | Tìm chuyến → chọn **toa** & chỗ (ghế ngồi mềm 4 hàng / giường theo **khoang, tầng**) → khai báo **CCCD, ngày sinh, thẻ SV** → xác nhận → giữ chỗ **15 phút** | Tìm chuyến (lọc loại xe) → chọn ghế trên sơ đồ **2 tầng / 45 chỗ / Limousine** → chọn **điểm đón/trả, đón tận nơi, hành lý** → xác nhận → giữ chỗ **10 phút** |
| **Thuật toán** | **Giá lũy tiến theo bậc cự ly** (900đ/km cho 300 km đầu, 700đ đến 1.000 km, 500đ sau đó) × hệ số hạng toa × hệ số tầng giường. **Gợi ý chỗ liền nhau**: ưu tiên chung khoang, nếu không thì cửa sổ trượt tìm nhóm có khoảng cách nhỏ nhất | **Giá động theo tỷ lệ lấp đầy**: < 50% giá gốc, 50–79% +5%, ≥ 80% +10% (đo ngay lúc giữ chỗ). Hệ số vị trí: giường tầng dưới +10%, ghế VIP Limousine +15% |
| **Chính sách phí** | Lễ Tết +20%, cuối tuần +5%; trẻ em −25%, SV −10%, người cao tuổi −15%; đặt trước 30 ngày −5%; bảo hiểm 1.000đ/khách | Lễ Tết +25%, cuối tuần +10%; trẻ em −30%; nhóm ≥ 4 khách −5%; hành lý vượt 20 kg: 10.000đ/kg; đón tận nơi 50.000đ |
| **Hoàn hủy** | ≥ 24h: phí 10%, 4–24h: 20%, < 4h: không nhận; phí tối thiểu 10.000đ/vé; bảo hiểm không hoàn | ≥ 24h: phí 10%, 2–24h: 25%, < 2h: không nhận; phí đón tận nơi không hoàn |
| **Hạng chỗ** | Ngồi cứng 80 chỗ (×0,80), ngồi mềm 64 (×1,00), khoang 6 – 42 giường (×1,35), khoang 4 – 28 giường (×1,60), khoang VIP 2 giường – 14 giường (×2,40) | Ghế ngồi 45 chỗ, giường nằm 36 chỗ (tầng dưới +10%), phòng cabin 24 phòng (tầng dưới +10%), Limousine 9 chỗ (hàng VIP +15%) |
| **Lộ trình** | Dừng ở mọi ga trên đoạn tuyến (ga lớn 10–15 phút, ga nhỏ 3–5 phút); giờ đến từng ga chia theo cự ly | Đường ngắn nhất trên mạng đường bộ (Dijkstra); từ 150 phút có trạm nghỉ, cứ ~210 phút nghỉ một lần (20 phút, trùng giờ ăn trưa/tối 30 phút) |
| **Validation** | Đóng bán trước 60 phút; tối đa 6 vé; tuổi theo ngày tàu chạy phải khớp đối tượng; < 6 tuổi miễn vé; từ 14 tuổi bắt buộc CCCD 12 số / hộ chiếu, không trùng; SV bắt buộc mã thẻ; trẻ em phải đi cùng người lớn | Đóng bán trước 30 phút; tối đa 8 ghế; điểm đón/trả phải thuộc lộ trình; đón tận nơi bắt buộc địa chỉ; hành lý ≤ 40 kg/khách; trẻ em khai năm sinh 5–11 tuổi; SĐT hành khách đúng định dạng; phải có người lớn |

Chống đặt trùng ghế: bảng `seat_locks` có ràng buộc **UNIQUE (ticket_type, trip_id, seat_key)**; tác vụ nền `HoldExpiryJob` mỗi 30 giây giải phóng ghế của đơn giữ chỗ quá hạn.

---

## 4. Cấu trúc mã nguồn

```
src/main/java/com/vietgo
├── domain/        Entity JPA + enum (User, TrainTrip, TrainCoach, BusTrip, Booking, Ticket, FeeLine, SeatLock, Consultation)
├── repository/    Spring Data JPA repositories
├── train/         Vé tàu: Controller, Service, FareCalculator, BookingValidator, RefundPolicy, SeatLayout
├── bus/           Vé xe:  Controller, Service, FareCalculator, BookingValidator, RefundPolicy, SeatLayout
├── booking/       Nghiệp vụ chung của đơn: giữ chỗ, thanh toán, hủy, hết hạn, mã QR
├── route/         Lộ trình: tuyến đường sắt, mạng đường bộ, lập lịch ga dừng / trạm nghỉ, dữ liệu bản đồ
├── consultation/  Lịch hẹn tư vấn (khung giờ, sức chứa 3 khách/khung)
├── auth/          Đăng ký, đăng nhập, tài khoản
├── admin/         Trang quản trị
├── config/        Spring Security, dữ liệu mẫu
├── common/        Tiện ích: định dạng tiền/ngày, lịch Lễ Tết, kết quả tính giá
└── web/           Trang chủ, chính sách, xử lý lỗi chung
src/main/resources
├── templates/     Giao diện Thymeleaf
├── static/        CSS, JS (hiệu ứng, sơ đồ chọn chỗ, bản đồ hành trình journey.js), favicon
└── application.properties
src/test/java      Kiểm thử đơn vị JUnit 5
database/          Script SQL Server
```

## 5. Lỗi thường gặp

- **Bản đồ lộ trình không hiện** → bản đồ cần Internet (thư viện Leaflet tải từ unpkg, nền bản đồ Esri). Khi mất mạng, trang vẫn hiển thị danh sách điểm dừng, giờ đến và trình phát hành trình.

- **Login failed for user 'sa'** → sai mật khẩu trong `application.properties`, hoặc chưa bật SQL Server Authentication.
- **The TCP/IP connection to the host localhost, port 1433 has failed** → bật TCP/IP trong SQL Server Configuration Manager rồi khởi động lại dịch vụ SQL Server. Nếu dùng instance tên (vd `SQLEXPRESS`), đổi URL thành `jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=VietGoDB;encrypt=true;trustServerCertificate=true` (và bật SQL Server Browser).
- **Cannot open database "VietGoDB"** → chưa chạy `database/01_create_database.sql`.
- Tiếng Việt bị lỗi dấu trong DB → đã cấu hình `use_nationalized_character_data=true` (cột NVARCHAR); nếu bảng được tạo từ phiên bản cũ, chạy lại `02_reset_data.sql` hoặc xóa database và tạo lại.
- Cổng 8080 bận → đổi `server.port` trong `application.properties`.
