# CR-011 — Đăng ký nhiều hồ sơ, Archivist chỉ định ngày nhận

Ngày. Người dùng yêu cầu bỏ chọn ngày của Reader; ngày cho nhận do nhân viên lưu trữ quyết định, không quá hai ngày sau đăng ký. Reader có mục Đăng ký mượn hồ sơ và chọn nhiều hồ sơ tương tự chọn file đính kèm.

Người dùng đã xác nhận **2 ngày lịch, tính cả cuối tuần**, từ ngày gửi đến hết ngày đăng ký + 2; một lần gửi có thể chọn nhiều hồ sơ nhưng mỗi hồ sơ được theo dõi/duyệt riêng.  Bốn role và quyền Director duyệt không đổi.

Triển khai chọn hồ sơ từ database bằng cửa sổ tìm kiếm/checkbox, không phải tải file từ máy. Tối đa 20 hồ sơ mỗi lần để giới hạn khối lượng giao dịch; từng hồ sơ là một reading_requests liên kết chung reading_request_batches. Tất cả phải hợp lệ để lưu cả lần đăng ký; gửi lại cùng token/nội dung không tạo trùng.

Luồng mới: Reader chọn hồ sơ + mục đích, không ngày → Archivist kiểm tra/trình → Director duyệt → Archivist chọn một ngày nhận cho từng yêu cầu → giao/nhận trả theo quy trình hiện có. Ngày nhận phải từ hôm nay và không sau ngày đăng ký + 2. Nếu xử lý hết hạn thì hủy/đăng ký mới; không tự gia hạn hoặc ghi nhận lùi ngày. Reader sửa mục đích không đổi ngày đăng ký gốc, xóa quyết định/lịch cũ và phải duyệt lại.

Lịch đã có trước nâng cấp được giữ theo nhánh LEGACY, không tự đổi ngày hay lịch sử. Đăng ký mới dùng ARCHIVIST. Quyết định này thay thế phần Reader chọn 1–5 ngày/30 ngày tới và bộ chọn ngày vừa làm trước đó; các báo cáo cũ được giữ như lịch sử.

Chi tiết tại [báo cáo triển khai](../verification/BORROWING_REGISTRATION.md). V14 bổ sung batch và hạn nhận; không sửa các migration đã áp dụng, schema tiếp theo dùng V15.
