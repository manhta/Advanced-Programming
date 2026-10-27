# CR-008 — Thông báo công việc và quyền xem chi tiết hồ sơ

Ngày. Người dùng yêu cầu chuông cho Archivist khi Reader gửi đăng ký, cho Director khi Archivist trình duyệt; số lưu trữ mở hồ sơ thay vì mở lại yêu cầu. Reader chưa được duyệt phải vào trang xin phép.

Người dùng đã xác nhận: quyền xem metadata chi tiết có hiệu lực ngay khi Director duyệt đến hết ngày đọc cuối cùng. Sửa/hủy yêu cầu làm mất quyền của yêu cầu đó; một yêu cầu hợp lệ khác của cùng Reader và cùng hồ sơ vẫn có thể cấp quyền. Vị trí kho, ghi chú nội bộ, chính sách nội bộ và audit không được mở thêm cho Reader. Không có bản quét/nội dung khám chữa bệnh điện tử trong phạm vi phần mềm hiện tại.

Thông báo công việc gửi cho tất cả tài khoản đang ACTIVE thuộc role cần xử lý. “Archivist xác nhận” ở bước này là kiểm tra và chọn “Đủ điều kiện — trình Director”, chuyển sang PENDING_APPROVAL; bước xác nhận lịch sau phê duyệt vẫn gửi Reader như trước. Không cấp quyền duyệt cho Archivist hoặc Admin.

Đọc [báo cáo triển khai](../verification/WORKFLOW_ALERTS_AND_READER_ACCESS.md). Quyết định này thay thế quy tắc Reader được xem chi tiết metadata trước khi xin phép và phạm vi chuông chỉ Reader trong CR-005/CR-006/CR-007 ở những phần tương ứng. Các mốc kiểm thử cũ được giữ làm lịch sử.
