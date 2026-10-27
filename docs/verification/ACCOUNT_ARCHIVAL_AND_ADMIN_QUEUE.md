# Xóa tài khoản Admin, hàng đợi thay đổi nhân sự và trang tạo riêng — V26

Ngày cập nhật:.

## Phạm vi

- Admin có thể xóa tài khoản khỏi danh sách quản lý.
- Hàng đợi thay đổi vai trò/khoa đã được Giám đốc duyệt nằm trong mục **Quản lý tài khoản**, dưới tên **Cập nhật thay đổi vai trò/khoa**.
- Kết quả tạo tài khoản và form tạo tài khoản ở hai trang riêng.

## Cách xóa tài khoản

Đây là xóa mềm: `app_users.deleted_at` ghi thời điểm xóa, `status` chuyển thành `DISABLED`, phiên bản tài khoản tăng và mật khẩu cấp 2 bị xóa. Middleware phiên và Spring Security từ chối tài khoản bị xóa. Tài khoản không xuất hiện trong tìm kiếm, danh sách hoặc bảng tài khoản gần đây.

Không xóa hàng `app_users` hay hồ sơ `employees`, vì hồ sơ, yêu cầu mượn, giao nhận, danh mục do người dùng tạo và nhật ký đều tham chiếu tới ID tài khoản. Giữ ID/name liên kết để lịch sử không mất nguồn gốc. Username và CCCD được giữ riêng, không được tái sử dụng sau khi xóa.

Backend từ chối xóa:

- Tài khoản Admin đang đăng nhập.
- Admin hoạt động cuối cùng hoặc Giám đốc hoạt động cuối cùng.
- Tài khoản có đề nghị đổi vai trò/khoa chưa kết thúc.
- Reader còn yêu cầu mượn đang xử lý, lịch chưa thực hiện hoặc phiên đang đọc.
- Tài khoản còn bệnh án điện tử đang chờ xử lý.
- Archivist còn yêu cầu đang phụ trách để kiểm tra.
- Tài khoản còn đợt kiểm kê đang mở.

Lệnh xóa dùng POST và CSRF; backend kiểm tra quyền ADMIN, phiên bản bản ghi để tránh xóa dữ liệu đã cập nhật, ràng buộc cuối cùng và các nghiệp vụ đang mở. Khi thành công, nhật ký ghi hành động xóa nhưng không ghi thông tin CCCD hay mật khẩu.

## Quản lý thay đổi nhân sự

Liên kết Admin cũ **Thay đổi nhân sự đã duyệt** đã bỏ khỏi navigation bar. Trang `/admin/accounts` có liên kết **Cập nhật thay đổi vai trò/khoa** tới `/admin/accounts/changes`. Trang này hiển thị đề nghị đã được duyệt/chờ cập nhật và đã áp dụng. Trang chi tiết và thao tác áp dụng có route mới dưới `/admin/accounts/changes/{id}`; các URL thông báo cũ vẫn hoạt động tương thích. Việc phê duyệt của Giám đốc và quy tắc thông báo giữ nguyên.

## Tách kết quả tạo tài khoản

POST `/admin/accounts` chuyển hướng sau tạo thành công đến `/admin/accounts/created`. Trang kết quả chỉ hiển thị tên đăng nhập và mật khẩu ban đầu trong flash session một lần, có `Cache-Control: no-store`, không hiển thị form tạo. Nút **Đến trang tạo tài khoản** mở `/admin/accounts/new`. Lỗi validation vẫn chuyển về form riêng và giữ các trường đã nhập.

## Files

- Migration Java: `src/main/java/mrm/database/migration/V26__AddAccountArchival.java`.
- Dịch vụ/xóa: `src/main/java/mrm/account/AccountService.java`.
- Route và redirect kết quả: `src/main/java/mrm/account/AccountController.java`, `StaffChangeController.java`.
- Danh sách và xác nhận xóa: `src/main/resources/templates/account/list.html`.
- Kết quả riêng: `src/main/resources/templates/account/created.html`.
- Danh sách thay đổi: `src/main/resources/templates/account/change-list.html`.
- Điều hướng: `src/main/resources/templates/fragments/layout.html`.

## Kiểm tra

Build đã kiểm tra bằng Maven với test execution bị tắt; không áp dụng V26 lên database `public` và không xóa tài khoản người dùng. Sau khi cập nhật mã nguồn, chạy ứng dụng bằng profile thường để Flyway áp dụng migration V26, rồi đăng nhập Admin và thử xóa một tài khoản thử nghiệm không có yêu cầu/kiểm kê đang mở. Xác nhận tài khoản biến khỏi danh sách, không đăng nhập được, và các hồ sơ/nhật ký cũ vẫn tham chiếu được. Kiểm tra thêm rằng Admin hiện tại, Admin/Giám đốc hoạt động cuối cùng và tài khoản đang xử lý nghiệp vụ bị chặn với thông báo phù hợp.
