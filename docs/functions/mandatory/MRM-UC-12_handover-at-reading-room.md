# MRM-UC-12 — Giao HSBA tại phòng đọc

**Trạng thái:** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | NVLT; người đọc nhận vật lý |
| Nguồn và quy tắc | FR-06/12; D-09/14/23; BR-10/11/12 |
| Tiền điều kiện | Phiên CONFIRMED đúng ngày, decision/revision hợp lệ; physical IN_STORAGE, không block, không lượt mở; người nhận là reader. |
| Dữ liệu đầu vào | Session/version, tình trạng lúc giao, người nhận xác nhận bằng kiểm tra danh tính, operation key; thời gian/người giao do server. |

## Kích hoạt và luồng chính

1. NVLT chọn phiên hôm nay, xem HSBA, người đọc và vị trí.
2. Lấy hồ sơ, đối chiếu mã/danh tính, kiểm tra tình trạng vật lý.
3. Nhập tình trạng và ghi chú, xác nhận giao.
4. Máy chủ khóa/kiểm tra lại HSBA và session, ràng buộc tối đa một lượt mở.
5. Lưu handover, session=READING, physical=AT_READING_ROOM, audit và receipt thao tác nguyên tử; hiển thị mã giao và giờ thực tế.

## Luồng thay thế và ngoại lệ

E1: Không tìm thấy/sai mã/người nhận khác → không giao, mở ngoại lệ nếu cần.

E2: Đang bị block/đã có lượt mở/ngày sai/chưa xác nhận → chặn; không ghi giờ giao.

E3: Hai NVLT giao → một thành công, một nhận xung đột; ràng buộc CSDL bảo vệ, không chỉ kiểm tra UI.

E4: Lỗi mạng sau commit → tra receipt/retry cùng khóa; không tạo lượt thứ hai.

A1: Hư hỏng đã biết → theo căn cứ D-23; nghi thiếu/mất chặn và đối soát.

## Hậu điều kiện, giao dịch và audit

HSBA tại phòng đọc, phiên đang đọc, một handover mở. Vị trí trong kho giữ làm vị trí về dự kiến, không được hiển thị như hồ sơ hiện nằm đó.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-12-01:** Hai lệnh đồng thời → một handover chưa trả duy nhất.
- **AC-12-02:** Lỗi ghi audit trong transaction → toàn bộ giao rollback.
- **AC-12-03:** Retry cùng khóa → trả lại cùng mã/giờ giao, không cập nhật lại giờ.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
