# MRM-OPT-01 — Email kết quả và lịch đọc

**Trạng thái:** dự thảo, chưa chọn triển khai. Nguồn: FR-11; D-16.

## Điều kiện đưa vào phạm vi

Giám đốc/CNTT xác nhận email có thuộc bản đầu, SMTP nội bộ, người nhận, nội dung và phép đo. Nếu chưa xác nhận, FR-11 vẫn là gap cần quyết định; thông báo trong ứng dụng không tự thay yêu cầu.

## Tác nhân và đầu vào

NVLT xác nhận lịch; hệ thống gửi; CNTT xử lý lỗi. Dữ liệu chỉ trong phạm vi nghiệp vụ được cấp; tài khoản/session hợp lệ. Áp dụng quyền, transaction, audit và retry trong [quy tắc chung](../mandatory/COMMON_RULES.md).

## Luồng chính

1. Giao dịch xác nhận lịch lưu outbox có event_key duy nhất cùng dữ liệu nghiệp vụ.
2. Tác vụ lấy PENDING, gửi nội dung tóm tắt không vị trí/toàn văn; ghi attempt và timestamp.
3. SMTP chấp nhận thì ACCEPTED; lỗi tạm đưa lịch retry có giới hạn; lỗi cuối FAILED và hiển thị người vận hành.
4. Gửi lại có kiểm soát cùng event; ghi audit retry và lỗi được làm sạch.

## Ngoại lệ và hậu điều kiện

SMTP lỗi không rollback lịch/quyết định.

Gửi thành công nhưng mất phản hồi có thể dẫn đến email lặp; cùng event ID trong thư giúp nhận diện, không hứa exactly-once qua SMTP.

Không có email → báo lỗi dữ liệu người nhận, không giả ACCEPTED; người đọc vẫn xem kết quả trong ứng dụng.

Thành công có kết quả tham chiếu và audit; thất bại không làm sai nghiệp vụ lõi. Không cấp thêm quyền ngoài ma trận.

## Tiêu chí chấp nhận đề xuất

- **AC-OPT-01-01:** Đo từ lúc NVLT xác nhận commit đến lúc SMTP chấp nhận ≤5 phút chỉ khi D-16 được duyệt; không chứng minh thư vào hộp nhận.
- **AC-OPT-01-02:** Retry không tạo một quyết định/lịch mới.
- **AC-OPT-01-03:** Email không chứa vị trí chi tiết hoặc dữ liệu ngoài quyền.
