# Thông báo đăng nhập nhanh —

Dòng đăng nhập thành công cố định trên trang chủ đã được thay bằng toast JavaScript, dùng chung Reader, Archivist, Director và Admin. Toast ở góc trên bên phải, có nút × để đóng sớm; tự xóa khỏi DOM sau 5000 ms kể từ khi script hiển thị. Không dùng alert chặn thao tác. Cần JavaScript để thấy thông báo.

SecurityConfig đặt cờ showLoginToast sau khi xác thực thành công. HomeController chuyển cờ vào model và xóa khỏi session khi hiển thị trang chủ theo role lần đầu. Tải lại trang hoặc quay về trang chủ không tạo thông báo thành công mới. Cờ chỉ phục vụ giao diện; không thay xác thực/phân quyền hay database.

File giao diện: templates/home.html. File JavaScript: static/js/login-toast.js. CSS:.login-toast trong static/css/app.css. Các hướng dẫn của cả bốn vai trò đã ghi vị trí chỉnh thời gian và giao diện.

Đã kiểm tra cú pháp JavaScript bằng node -check; build Maven package thành công. Kiểm thử HTTP kiểm tra đăng nhập/chuyển trang/toast lần đầu và tải lại cho cả bốn vai trò. Kiểm thử được thực hiện trên schema PostgreSQL riêng tự xóa. Trong lần chạy đầu, một kỳ vọng cũ đòi trang tiến trình hiển thị ghi chú không còn phù hợp với HTML hiện tại; đã điều chỉnh kiểm tra vẫn bảo đảm không có script thực thi, giữ nguyên giao diện người dùng đã sửa.

Chưa xác minh trực quan bằng trình duyệt trong lần này. Sau khởi động lại, đăng xuất và đăng nhập lại để kiểm tra thông báo 5 giây, bấm ×, tải lại trang để xác nhận không lặp.

Kết quả chạy đầy đủ cuối cùng: **375/375 kiểm tra FR đạt**, schema thử nghiệm đã xóa. Log: [login-toast-.txt](evidence/login-toast-2026-10-25.txt).
