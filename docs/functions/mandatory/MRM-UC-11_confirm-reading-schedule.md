# MRM-UC-11 — Xác nhận lịch và xem kết quả trong ứng dụng

**Trạng thái:** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | NVLT; chủ yêu cầu xem kết quả |
| Nguồn và quy tắc | FR-08/11; D-05/06/16; BR-08/09/21 |
| Tiền điều kiện | Revision APPROVED hiện hành; các ngày chưa hết hạn; HSBA không có vấn đề chưa xử lý cản lịch. |
| Dữ liệu đầu vào | Request/revision, ngày trong phạm vi duyệt, ghi chú; server liên kết decision và reader. |

## Kích hoạt và luồng chính

1. NVLT mở yêu cầu đã duyệt, xem từng ngày và lịch HSBA.
2. Chọn ngày thuộc phạm vi đã duyệt để xác nhận; không sửa ngày hoặc người đọc.
3. Máy chủ kiểm tra chỗ lịch và quyết định hiện hành, tạo/đổi phiên thành CONFIRMED cho các ngày chọn.
4. Lưu xác nhận và sự kiện trong ứng dụng; nếu email được chọn, ghi outbox cùng giao dịch.
5. Chủ yêu cầu xem lịch xác nhận và ngày chưa xác nhận riêng biệt.

## Luồng thay thế và ngoại lệ

A1: Có thể xác nhận lịch theo từng ngày sau quyết định duyệt toàn revision; điều này không phải duyệt một phần.

E1: Trùng chỗ với phiên khác → không xác nhận tập ngày vừa gửi; chỉ rõ ngày xung đột theo quyền.

E2: Ngày ngoài duyệt → UC-09, không lách bằng lịch.

A2: Ngày mong muốn đã qua trước khi xác nhận → đánh dấu EXPIRED cho phiên dự kiến, không tự chuyển ngày.

E3: Lỗi email → giữ CONFIRMED, trạng thái thông báo độc lập.

## Hậu điều kiện, giao dịch và audit

Mỗi ngày mong muốn có phiên PLANNED sau duyệt; ngày chọn thành CONFIRMED. Physical vẫn trong kho. Audit ai xác nhận ngày nào.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-11-01:** Duyệt 3 ngày, xác nhận 2 → hai CONFIRMED và một PLANNED, không đổi phạm vi quyết định.
- **AC-11-02:** Hai NVLT giữ chỗ cùng HSBA/ngày cho hai người → chỉ một thành công.
- **AC-11-03:** READER xem lịch người khác → từ chối; thông báo không lộ vị trí.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
