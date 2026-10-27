# MRM-UC-14 — Đối soát chưa trả, không đến và bất thường

**Trạng thái:** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | NVLT; Giám đốc giám sát |
| Nguồn và quy tắc | BR-11 nguồn; FR-06/12/13; BR-13/15/23/24 |
| Tiền điều kiện | Có quyền theo dõi kho; cấu hình giờ chốt và người chịu trách nhiệm được xác nhận. |
| Dữ liệu đầu vào | Loại exception, HSBA/session/handover liên quan, mô tả, căn cứ, người phụ trách, hướng xử lý, kết quả. |

## Kích hoạt và luồng chính

1. NVLT mở danh sách chưa trả theo giờ/ngày nghiệp vụ; hệ thống lấy handover chưa đóng, không dựa riêng nhãn request.
2. Sau giờ chốt, mở hoặc bổ sung exception OVERDUE cho mỗi lượt; chạy lại không tạo trùng.
3. Liên hệ người giữ, ghi nguyên nhân và người xử lý; giữ session=READING/physical=AT_READING_ROOM.
4. Khi nhận trả, liên kết UC-13; nếu có chênh lệch cần xử lý tiếp, exception chưa đóng.
5. Đóng ngoại lệ chỉ khi có kết quả/căn cứ và người đủ quyền; mở block cần xác nhận vị trí/tình trạng thực tế.

## Luồng thay thế và ngoại lệ

A1: Phiên CONFIRMED không giao sau chốt → NO_SHOW có lý do; không tạo handover.

A2: Mất hồ sơ → mở MISSING, block; không đóng lượt bằng “đã trả” giả; Giám đốc quyết định quy trình xử lý ngoài và điều chỉnh có căn cứ.

E1: Ngoại lệ đã có → bổ sung diễn biến, không ghi đè lịch sử.

E2: Đóng OVERDUE khi lượt vẫn mở → bị chặn, trừ bản xử lý bất thường có thẩm quyền và căn cứ riêng không đánh dấu RETURNED.

## Hậu điều kiện, giao dịch và audit

Có hồ sơ xử lý truy vết, không tạo trạng thái khả dụng sai. Không tự khóa quyền đăng ký mới của người đọc trong phương án cơ sở.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-14-01:** Chạy đối soát hai lần → một OVERDUE mở mỗi handover.
- **AC-14-02:** Đánh dấu không đến cho READING → bị chặn.
- **AC-14-03:** Nhận trả nhưng còn hư hỏng → đóng phần quá hạn, ngoại lệ condition còn mở.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
