# Migration bằng Java thay cho file SQL

Ngày. Người dùng yêu cầu đặt SQL trong Java và xác nhận chưa chạy migration. Đợt này chuyển mã nguồn, không chạy ứng dụng hoặc kết nối database.

## Cấu trúc hiện hành

```text
src/main/java/mrm/database/migration/
├── V1__CreateAuthenticationTables.java
└── V2__InsertRoles.java
```

- V1 chứa SQL tạo `roles` và `app_users`; `role_id` là một FK bắt buộc, không UNIQUE, nên mỗi tài khoản một role và nhiều tài khoản có thể cùng role.
- V2 chứa SQL thêm bốn vai trò, không chứa mật khẩu hoặc tài khoản website.
- Không còn file migration `.sql` trong source resources. File V2 cũ được thay bằng class Java; trước đó chưa có file V1 trong source nên V1 được bổ sung theo thiết kế đã hướng dẫn.
- Vẫn sử dụng Flyway để quản lý thứ tự/lịch sử, không dùng ApplicationRunner chạy CREATE TABLE mỗi lần khởi động.

## Flyway tìm và chạy class như thế nào?

`application-dev.properties` chỉ rõ:

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:mrm/database/migration
```

Maven biên dịch class Java vào classpath ở package tương ứng. Flyway quét location, tìm class kế thừa BaseJavaMigration và lấy version từ tên V1/V2. Các class này không cần @Component, @Profile hoặc phương thức main. Không đăng ký thêm chúng thành bean để tránh đăng ký trùng cùng version.

Khi sau này chạy ứng dụng với profile dev, Flyway sẽ tạo bảng lịch sử và chạy V1 → V2 trước các ApplicationRunner. Đây là thao tác thay đổi database thật; lượt cập nhật mã này chưa thực hiện nó. Chạy với profile db-check cũng không ngăn Flyway nếu dev và Flyway vẫn bật.

## Cách viết SQL trong Java

SQL được lưu trong text block `String`. `migrate(Context context)` lấy connection thuộc transaction do Flyway quản lý rồi thực thi từng statement. Chỉ đóng Statement, không đóng connection của Context. Không inject JdbcTemplate dùng một connection khác vào migration.

`getChecksum()` tính CRC32 UTF-8 trên chính SQL, vì Java migration mặc định không có checksum. Sau khi áp dụng không sửa SQL hoặc nội dung logic migration cũ; thêm V3/V4. Checksum hiện kiểm tra các hằng SQL, không phải toàn bộ bytecode hay mọi thay đổi điều khiển của class.

Nếu bảng đã được tạo thủ công trước khi chạy, cần đối chiếu schema/history; không tự dùng IF NOT EXISTS, baseline, repair hoặc xóa database để che khác biệt. Chuyển SQL migration đã áp dụng sang Java cùng version có thể làm lệch type/checksum; việc chuyển lần này dựa trên xác nhận chưa chạy migration.

## Các truy vấn khi chạy ứng dụng

Các SELECT/INSERT/UPDATE nghiệp vụ vẫn đặt trong Java Repository/Service bằng JdbcTemplate với tham số `?`, không nối dữ liệu nhập vào SQL. DatabaseConnectionCheck hiện có đã dùng SQL trong Java. Khởi tạo mật khẩu thử sau này dùng PasswordEncoder và Java initializer riêng, không đưa password vào migration.

## Kiểm chứng

Chỉ kiểm tra compile, không gọi spring-boot:run, Flyway migrate, test khởi động context hoặc SQL trong lượt này. Khi bắt đầu chạy thật, cần DB environment đúng và kiểm tra bảng/history sau khởi động; chỉ compile không chứng minh migration đã thực thi thành công.

[Flyway Java migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/java-based-migrations) giải thích BaseJavaMigration, checksum và connection của Context.

Thư mục Note giữ nguyên theo quy tắc người dùng; các ví dụ hướng dẫn trước đây có file SQL chỉ là phiên bản cũ, không phải cấu trúc triển khai hiện hành.
