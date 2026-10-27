# Bộ chọn ngày đọc —

**Đã được thay thế bởi [CR-011](../changes/CR-011_borrowing-batches-and-pickup-window.md) / [đăng ký không ngày](BORROWING_REGISTRATION.md):** Reader không còn chọn ngày, Archivist chỉ định ngày nhận trong 2 ngày lịch; bộ chọn ngày dưới đây là lịch sử của thay đổi trước đó.


Theo yêu cầu người dùng: thay ô nhập chuỗi ngày bằng input type=date, đổi nhãn thành **Ngày đọc (Số ngày mượn tối đa: 5)**, xóa câu “Chọn 1–5 ngày, từ hôm nay đến 30 ngày tới.” trên form.

Đã áp dụng cho đăng ký và sửa yêu cầu. Ban đầu có một ô; JavaScript cho thêm tối đa năm ô, xóa khi có nhiều hơn một ô, cập nhật nhãn trợ năng và chuyển focus. Mỗi ô là một input dates; controller nhận List<String> rồi ghép cho service hiện tại. Backend tiếp tục kiểm tra 1–5 ngày khác nhau trong phạm vi hôm nay đến 30 ngày tới; bỏ lời hướng dẫn trên HTML không bỏ quy tắc này. Min/max trên input dựa theo ngày Asia/Bangkok từ server. Nếu tắt JavaScript vẫn gửi được một ngày bằng input gốc, nhưng không thêm/xóa nhiều ô.

Fragment: templates/fragments/reading-dates.html; JS: static/js/reading-dates.js; hai màn hình reading/new.html và reading/detail.html. Không thay database, schema vẫn V13. Ngày đã chọn được render lại khi form đăng ký gặp lỗi nghiệp vụ. CSS hidden của bộ chọn được khai báo riêng để không bị quy tắc display của button dùng chung ghi đè.

Build package thành công và node -check JS thành công. FunctionalRequirementsCheck đạt **250/250** trong schema tạm đã dọn: form render input date/nhãn mới, gửi hai input dates tạo hai phiên đọc, sửa bằng nhiều input, gửi sáu ngày bị chặn và render lại form; các ca nghiệp vụ/phân quyền hiện có cùng đạt. Log tại [reader-date-picker.txt](evidence/reader-date-picker-2026-10-24/reader-date-picker.txt). Kiểm tra qua HTTP/template; chưa xác minh trực tiếp lịch chọn ngày bằng trình duyệt trong lần cập nhật này.

Khởi động lại ứng dụng và tải lại trang để dùng Java/template/JS mới. Khi chạy bằng JAR cần dùng bản đã đóng gói mới.
