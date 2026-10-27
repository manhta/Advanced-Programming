# Sửa lỗi form đăng nhập cũ trả 403 —

Người dùng báo 403 tại `/login` với Archivist và Admin. Kiểm tra database xác nhận bốn tài khoản vẫn ACTIVE và đúng role; nhật ký trước thời điểm báo lỗi chỉ có Reader đăng nhập mới. GET trang đăng nhập đang chạy trả 200 và có trường CSRF. Không có dữ liệu cookie/request lỗi thực tế để khẳng định tuyệt đối nguyên nhân của phiên trình duyệt người dùng.

Đã tái hiện một trường hợp phù hợp: tải form đăng nhập, đăng nhập Reader thành công (token CSRF thay đổi), rồi dùng lại form/token cũ để đăng nhập Archivist. Trước bản sửa, Spring Security từ chối POST đó bằng 403 trước bước kiểm tra username/password. Tình huống tương tự có thể xảy ra khi giữ tab cũ hoặc sau khi ứng dụng khởi động lại và mất phiên.

## Bản sửa

- `SecurityConfig`: chỉ khi POST `/login` gặp `CsrfException`, chuyển về GET `/login?expired` để lấy token mới. Không gửi lại mật khẩu, không bỏ CSRF, không thay quyền hoặc password.
- `login.html`: thông báo phiên/biểu mẫu hết hiệu lực và yêu cầu nhập lại thông tin.
- Các lỗi thiếu CSRF ở thao tác nghiệp vụ và URL trái role tiếp tục 403.

## Kiểm chứng

Build `mvnw.cmd -o -B -ntp package` thành công. Chạy `FunctionalRequirementsCheck` trong schema tạm, đạt **178/178**. Đây là bộ FR 170 kiểm tra trước đó cộng tám kiểm tra cho hồi phục đăng nhập; không tuyên bố chạy lại các bộ catalog/năm chức năng riêng trong lần sửa này.

Các ca mới: đăng nhập ban đầu; token cũ chuyển về form mới; thông báo hết hiệu lực có render; form mới đăng nhập Archivist thành công; Archivist vào trang chủ 200; vẫn không vào trang Admin; thiếu token đăng nhập bị từ chối và chuyển về form mới; Admin đăng nhập mới vào trang chủ 200. Các ca FR còn lại gồm CSRF nghiệp vụ và quyền vẫn đạt. Schema thử đã dọn; không thay đổi 15 hồ sơ mẫu trong public.

Log: [login-csrf-recovery.txt](evidence/login-csrf-2026-10-22/login-csrf-recovery.txt).

## Áp dụng trên máy phát triển

Ứng dụng đang chạy không tự nhận cấu hình Java mới. Dừng terminal đang chạy bằng Ctrl+C, rồi khởi động lại theo quy trình dev trong README. Mở mới `http://localhost:8080/login`, tải lại trang trước khi nhập tài khoản. Nếu so sánh nhiều role cùng lúc, dùng các cửa sổ/profile trình duyệt riêng vì các tab cùng trình duyệt chia sẻ cookie phiên. Khi còn lỗi, cần đối chiếu request mới và log server; không tự đổi vai trò để bỏ qua 403.
