# Admin tạo tài khoản Reader/Archivist —

**Đã thay thế phần giao diện tạo riêng bằng [CR-010](../changes/CR-010_unified-accounts-and-employees.md):** dùng một form Tạo tài khoản có lựa chọn Bác sĩ/Nhân viên lưu trữ và thông tin nhân viên. Đọc [hướng dẫn hiện tại](EMPLOYEE_MANAGEMENT.md). Các kết quả/nút tách riêng dưới đây là mốc trước thay đổi này.

Backend đã có AccountService.create và POST /admin/accounts. Lần cập nhật này bổ sung hai nút tạo rõ ràng tại trang chủ Admin và tại form quản trị, tự chọn role theo nút; vẫn kiểm tra quyền ADMIN ở URL và service. Không thay schema (vẫn V12), không thêm tài khoản vào database thật khi triển khai.

Form gồm username, họ tên, role, khoa tùy chọn, email tùy chọn và mật khẩu ban đầu 12–128 ký tự. Mỗi tài khoản một role, mặc định ACTIVE; PBKDF2 băm mật khẩu và audit ACCOUNT_CREATE. Unique username ngăn tài khoản trùng. Các thao tác tạo/sửa Director/Admin đã có không bị loại bỏ bởi yêu cầu bổ sung này.

Hướng dẫn thao tác tại [Admin](../user-guides/admin.md). Cần khởi động lại ứng dụng dev để nhận Java và template mới. Khi tạo thành công, dùng mật khẩu Admin đã nhập cho tài khoản mới để đăng nhập; không có mật khẩu mặc định.

Build package thành công. Kiểm chứng dùng FunctionalRequirementsCheck với schema tạm: tạo Reader qua HTTP, đăng nhập Reader mới, tạo Archivist qua HTTP, đăng nhập và mở form nhập kho, chặn Archivist vào quản trị tài khoản, chặn username trùng; kiểm tra trang chủ và lựa chọn role ban đầu. Những kiểm tra FR/CSRF/đọc/chuông hiện có chạy cùng bộ. Kết quả cuối cùng và log bổ sung bên dưới.

Kết quả ngày: **215/215 kiểm tra đạt**, schema thử đã dọn; không tạo tài khoản thử trong public. Đây là bộ kiểm tra Java chạy trực tiếp, không phải test JUnit tự chạy trong package. Log tại [admin-account-creation.txt](evidence/admin-accounts-2026-10-22/admin-account-creation.txt).
