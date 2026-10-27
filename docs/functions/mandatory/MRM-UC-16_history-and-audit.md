# MRM-UC-16 — Tra cứu lịch sử và nhật ký kiểm toán

**Trạng thái:** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | NVLT/Giám đốc theo quyền audit riêng |
| Nguồn và quy tắc | FR-13; D-20; BR-01/19/22 |
| Tiền điều kiện | Có quyền lịch sử theo đối tượng; quyền audit không tự đi kèm ADMIN. |
| Dữ liệu đầu vào | Đối tượng, actor, loại hành động, outcome, từ/đến, trang; khoảng ngày hợp lệ. |

## Kích hoạt và luồng chính

1. Người có quyền mở timeline HSBA/yêu cầu hoặc trang audit.
2. Chọn bộ lọc; máy chủ giới hạn đối tượng/trường theo quyền.
3. Hiển thị sự kiện có thời gian, actor, action, kết quả, revision và correlation; phân biệt lịch sử vật lý với log kỹ thuật.
4. Mở chi tiết thay đổi chỉ trường được phép; ghi sự kiện xem audit.
5. Không có nút sửa/xóa audit; hỗ trợ phát hiện sai bằng bản điều chỉnh được liên kết.

## Luồng thay thế và ngoại lệ

E1: Người đọc chỉ xem tiến độ yêu cầu mình, không audit toàn kho.

E2: Tài khoản đã khóa → sự kiện vẫn giữ ID/tên snapshot cần thiết.

A1: Kho lưu dài hạn không trực tuyến → trả trạng thái và quy trình truy xuất, không báo không có sự kiện.

E3: Hệ thống không thể ghi audit bắt buộc cho thay đổi → không xác nhận thay đổi đã thành công.

## Hậu điều kiện, giao dịch và audit

Chỉ đọc, không thay lịch sử. Chính sách 10 năm có lưu dài hạn/backup và kiểm tra phục hồi, không chỉ một biến cấu hình.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-16-01:** Lọc actor/time/action → đúng bộ dữ liệu biết trước.
- **AC-16-02:** Thử sửa/xóa qua đường dẫn trực tiếp → không có quyền chức năng.
- **AC-16-03:** Quyết định/giao–trả → có correlation liên kết các sự kiện, không có password/token.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
