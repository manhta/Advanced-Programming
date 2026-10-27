# CR-013 — Phân tuyến duyệt mượn theo lượt

Ngày, người dùng yêu cầu áp dụng ảnh quy trình tiếp nhận/khai thác hồ sơ: lượt thường do nhân viên lưu trữ duyệt; lượt có bất kỳ hồ sơ tử vong nào cần Giám đốc duyệt tất cả yêu cầu con. Lý do bắt buộc nếu trên 5 hồ sơ hoặc có hồ sơ tử vong. Đây là quy tắc hiện tại thay thế nội dung cũ chặn tử vong và trình mọi lượt mượn lên Director.

Thực hiện bằng Java migration V19, kiểm tra tại backend và bộ chọn, cập nhật node tiến trình, thông báo và giao diện xử lý. Yêu cầu cũ giữ tuyến Director để bảo toàn lịch sử. Quy trình công bố bệnh án do bác sĩ khai báo không thay đổi. Không thêm role ủy quyền hoặc gửi email.

Xem [báo cáo nguyên lý, sử dụng và kiểm thử](../verification/BORROW_APPROVAL_ROUTING.md), [Reader](../user-guides/reader.md), [Archivist](../user-guides/archivist.md), [Director](../user-guides/director.md). Bản giấy nháp vẫn là thông tin tạm chưa nhập kho, chỉ Archivist thấy.
