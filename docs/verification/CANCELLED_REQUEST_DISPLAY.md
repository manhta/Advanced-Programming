# Hiển thị yêu cầu hủy —

Trạng thái CANCELLED dùng nhãn Đã hủy trên danh sách/trang tiến trình. Danh sách để trống cả ngày đăng ký và ngày nhận khi hủy, kể cả còn lịch cũ trong database. Không sửa/xóa dữ liệu ngày hay lịch sử.

Thông báo CANCEL mới dùng tiêu đề Yêu cầu đã hủy và nội dung rỗng. Khi đọc thông báo cũ, service xác định loại từ request_events.action và chuẩn hóa tiêu đề/nội dung ở lớp hiển thị; không ghi đè dữ liệu lịch sử. Template không tạo đoạn nội dung rỗng. Giữ thời điểm thông báo, đã đọc/chưa đọc, mở yêu cầu, đánh dấu đã đọc.

File chính: ReadingLabels.java, NotificationService.java, templates/reading/list.html và templates/notification/inbox.html.

Build Maven offline package thành công. Chạy riêng FunctionalRequirementsCheck đạt **352/352** trên schema riêng tự xóa. Kiểm tra thêm thông báo hủy mới, chuẩn hóa thông báo cũ nhưng giữ dữ liệu gốc, HTML hàng hủy có hai ô ngày trống và trạng thái tiếng Việt, tiêu đề thông báo rút gọn. Các kiểm thử phân quyền, tiến trình và luồng giao–trả tiếp tục đạt. Không thay đổi schema/data ứng dụng trong public. Chưa xác minh trực quan bằng trình duyệt trong lần này.

Log: [cancelled-display-.txt](evidence/cancelled-display-2026-10-25.txt).
