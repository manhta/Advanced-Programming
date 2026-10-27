# MRM-OPT-04 — Kiểm kê kho theo đợt

**Trạng thái:** dự thảo, chưa chọn triển khai. Nguồn: FR-19.

## Điều kiện đưa vào phạm vi

Đơn vị xác nhận kiểm kê thuộc phạm vi, tần suất, người kiểm, người xác nhận và cách khóa/snapshot dữ liệu.

## Tác nhân và đầu vào

NVLT kiểm đếm, Giám đốc xác nhận chênh lệch. Dữ liệu chỉ trong phạm vi nghiệp vụ được cấp; tài khoản/session hợp lệ. Áp dụng quyền, transaction, audit và retry trong [quy tắc chung](../mandatory/COMMON_RULES.md).

## Luồng chính

1. Tạo đợt, chọn kho/phạm vi và chụp danh sách kỳ vọng tại thời điểm bắt đầu.
2. Ghi nhận từng hồ sơ thực tế, vị trí, tình trạng, không thấy hoặc có ngoài danh sách.
3. Phân biệt hồ sơ đang có lượt đọc hợp lệ với hồ sơ thật sự thất lạc.
4. Đối soát biến động phát sinh trong lúc kiểm kê; xác nhận chênh lệch có căn cứ.
5. Điều chỉnh qua chức năng kho/ngoại lệ có lịch sử, đóng đợt khi có người xác nhận.

## Ngoại lệ và hậu điều kiện

Không tự đổi trạng thái hồ sơ đang đọc thành mất.

Không ghi đè current location theo bản snapshot cũ.

Đợt đóng chỉ đọc; bổ sung bằng đợt/biên bản điều chỉnh.

Thành công có kết quả tham chiếu và audit; thất bại không làm sai nghiệp vụ lõi. Không cấp thêm quyền ngoài ma trận.

## Tiêu chí chấp nhận đề xuất

- **AC-OPT-04-01:** Hồ sơ giao sau snapshot → có thể đối chiếu handover, không báo mất sai.
- **AC-OPT-04-02:** Chênh lệch vị trí → adjustment/lịch sử, không sửa im lặng.
- **AC-OPT-04-03:** Chạy lại đối soát không nhân đôi kết quả kiểm đếm.
