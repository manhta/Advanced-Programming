# Đăng nhập tối giản — phạm vi hiện hành cho lượt triển khai đầu

Ngày, D-26 / CR-003. **Chỉ thiết kế, chưa code.** Tài liệu này ưu tiên hơn thiết kế login-design.md và 28 ca LG-TC trước đó đối với lượt triển khai đầu. Thiết kế mở rộng vẫn giữ để làm sau, không phải điều kiện phải hoàn tất ngay.

## Phạm vi người dùng đã chọn

Đăng nhập bằng username/password → xác thực account → đọc một role từ DB → vào trang tương ứng. Hoãn quản lý phiên nâng cao, bắt buộc đổi password ở lần login đầu và quên/reset password trong lượt này. Không cần trang quản trị tài khoản hoặc dữ liệu HSBA để demo đăng nhập.

Session HTTP cơ bản của Spring Security vẫn được dùng để duy trì trạng thái đã xác thực giữa các trang. Không tự viết hệ thống session, không có bảng session, Redis, JWT, quản lý thiết bị, giới hạn nhiều phiên hoặc chính sách timeout tùy chỉnh trong lượt đầu. Đây là nền tảng kỹ thuật xác thực, không phải màn hình quản lý phiên.

## 1. Yêu cầu và luồng

1. Bốn role dùng một form chung, chỉ nhập username/password; không có dropdown role.
2. Server kiểm tra dữ liệu form, tìm account và xác minh password hash; account không hoạt động bị từ chối.
3. Sai account/password/trạng thái: cùng thông báo chung, không cấp quyền.
4. Đúng: Spring Security xác thực và duy trì context bằng session cơ bản; role lấy từ DB.
5. Chuyển tới trang đúng role. Chưa có dashboard nghiệp vụ, chỉ trang chào có tên/role.
6. Người đã login không được mở trang role khác chỉ bằng đổi URL. Người chưa login mở trang bảo vệ phải về login.

Chính sách D-22 về thử sai/timeout/password mới và tính năng auth nâng cao giữ cho đợt thiết kế tiếp; chưa coi đã được người dùng duyệt toàn bộ. Không tạo tài khoản tạm buộc đổi khi chưa có màn hình đổi password. Secret cấp 2 thuộc phê duyệt, không đưa vào form login.

## 2. Giao diện đề xuất

```text
MRM — Đăng nhập
Tên đăng nhập [____________________]
Mật khẩu [____________________]
 [Đăng nhập]
[Thông báo lỗi chung khi có]
```

Không có chọn role, đăng ký, quên password, bắt buộc đổi hoặc remember-me. Có label, Enter submit, kiểm tra trường bắt buộc; giữ username nhưng không trả lại password khi lỗi. Server validate lại dù trình duyệt đã kiểm tra.

| Role | Trang đích đề xuất |
|---|---|
| READER | /reader/home |
| ARCHIVIST | /archivist/home |
| DIRECTOR | /director/home |
| ADMIN | /admin/home |

Trang chào chỉ tên người dùng, role và nội dung chào phù hợp, không chứa dữ liệu bệnh án. Đường dẫn chưa được triển khai. POST login/logout nền tảng nếu có sử dụng Spring Security và CSRF mặc định, không tự lưu role do client gửi hoặc chỉ kiểm tra role bằng JavaScript.

## 3. Cơ sở dữ liệu cần chuẩn bị tiếp theo

Chọn mô hình tối thiểu hai bảng, chưa viết SQL:

| Bảng | Trường | Ràng buộc |
|---|---|---|
| ROLE | id, code, name | code unique, chỉ READER/ARCHIVIST/DIRECTOR/ADMIN |
| APP_USER | id, username, password_hash, display_name, role_id, status | username chuẩn hóa unique; role_id FK NOT NULL; hash không plaintext; status ACTIVE/LOCKED/DISABLED |

ID dự kiến UUID theo thiết kế chung. role_id không UNIQUE vì nhiều user được cùng role. Username trim/lowercase nhất quán, password không trim. Các trường kiểm toán/thời gian có thể bổ sung khi dựng schema nhưng không cần engine phân quyền nhiều vai trò hoặc bảng nghiệp vụ HSBA để login.

Chuẩn bị bốn tài khoản thử, mỗi role một account, mật khẩu được băm qua PasswordEncoder tương ứng. Chuyển thông tin mật khẩu/DB qua cấu hình môi trường phù hợp, không ghi secret vào Git, README hay log. Không đưa must_change_password vào điều kiện chặn lượt demo này; nếu field tồn tại từ thiết kế chung thì các account thử có giá trị false.

## 4. Backend và frontend khi bắt đầu code

- PostgreSQL + migration: tạo ROLE/APP_USER và dữ liệu role; account thử khởi tạo bằng cơ chế an toàn đã thống nhất.
- Repository/UserDetailsService: đọc account/role, không tải role từ form.
- Spring Security/PasswordEncoder: xác thực, kiểm tra URL theo role, session/CSRF cơ bản; không tự so mật khẩu bằng plaintext.
- GET /login render Thymeleaf; POST /login do cơ chế xác thực xử lý.
- Controller/view bốn trang chào; fail trả thông báo chung, success điều hướng server.
- Không triển khai UC-02 đầy đủ hoặc hệ thống quản lý phiên trong lượt này.

## 5. Kiểm thử tối thiểu — NOT RUN

| TC | Tình huống | Kết quả |
|---|---|---|
| BASIC-01 | Đăng nhập đúng từng role | Đúng một role DB, đúng trang chào |
| BASIC-02 | Sai username/password hoặc account khóa | Không xác thực; thông báo chung |
| BASIC-03 | Bỏ trống hoặc gửi đầu vào không hợp lệ | Validation server, không cấp quyền |
| BASIC-04 | Thêm role=DIRECTOR vào form READER | Không nâng quyền, role vẫn từ DB |
| BASIC-05 | Chưa login mở home bất kỳ role | Yêu cầu login |
| BASIC-06 | READER mở /admin/home hoặc /director/home | 403, không nội dung trang khác role |
| BASIC-07 | Sau login, chuyển/tải lại trang được phép | Context cơ bản duy trì, không hỏi password mọi request |
| BASIC-08 | DB lỗi hoặc thiếu role hợp lệ | Không cấp quyền mặc định; lỗi dịch vụ phù hợp |
| BASIC-09 | Kiểm tra DB/log và form gửi thiếu CSRF | Password chỉ dạng hash, log không bí mật; CSRF vẫn được bảo vệ |

Các test timeout/multiple devices/forced-change/recovery trong kế hoạch cũ DEFERRED cho lượt đầu, không đánh PASS. Lát cắt login hoàn thành khi bộ BASIC đạt sau triển khai; không đồng nghĩa toàn bộ UC-01 rộng hoặc UC-02 đã hoàn thành.

## 6. Bước tiếp theo

Chuyển bản thiết kế hai bảng và form này thành kế hoạch migration/khởi tạo account, sau đó mới code backend → Thymeleaf → bốn trang chào → chạy BASIC-01–09. Trước lúc kết nối thật cần xác định database phát triển riêng và cách cung cấp thông tin kết nối an toàn; không dùng password của người dùng trong file tài liệu.
