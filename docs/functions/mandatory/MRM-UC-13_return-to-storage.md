# MRM-UC-13 — Nhận trả, cất lại và ghi chênh lệch

**Trạng thái:** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | NVLT |
| Nguồn và quy tắc | FR-05/06/12; D-14/23; BR-11/12/15/16 |
| Tiền điều kiện | Có lượt giao mở gắn session READING; cho trả muộn dù tài khoản người đọc/người duyệt bị khóa hoặc đổi vai trò sau giao. |
| Dữ liệu đầu vào | Handover/version, tình trạng trả, vị trí cất hợp lệ, lý do chênh lệch/trả muộn khi có; operation key. |

## Kích hoạt và luồng chính

1. NVLT chọn lượt đang mở, đối chiếu hồ sơ và tình trạng lúc giao.
2. Kiểm tra vật lý khi trả, chọn vị trí cất thực tế; đánh dấu chênh lệch nếu có.
3. Xác nhận; server lấy giờ trả, kiểm tra không trước giờ giao và chưa có người trả trước.
4. Đóng handover, session=RETURNED, physical=IN_STORAGE, current location đích; lưu location history nếu đổi vị trí.
5. Nếu thiếu/hỏng, tạo exception và block phù hợp; nếu trả muộn, liên kết ngoại lệ cũ đã có hoặc tạo mới; lưu tất cả và audit nguyên tử.

## Luồng thay thế và ngoại lệ

E1: Chưa có lượt giao/đã trả → không tạo lượt giả; retry cùng khóa trả kết quả cũ.

E2: Vị trí ngừng dùng/không xác định → không báo hoàn tất cất kho; NVLT chọn vị trí đối soát được cấu hình trước.

A1: Hồ sơ đã nhận nhưng đang gián đoạn hệ thống → ghi chứng từ tạm theo kế hoạch vận hành, đối soát sau; không điền giờ giả.

E3: Hai người nhận trả → chỉ một đóng lượt; người sau xem kết quả.

## Hậu điều kiện, giao dịch và audit

Hồ sơ trở lại kho hoặc vị trí đối soát có thật, phiên đã trả; condition block có thể vẫn mở. Nhận trả không có nghĩa đã giải quyết hỏng/thiếu.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-13-01:** Trả ngày hôm sau → vẫn đóng lượt với timestamp thực, lưu lý do và ngoại lệ quá ngày.
- **AC-13-02:** Trả có thiếu → IN_STORAGE nhưng block, không được giao lượt mới.
- **AC-13-03:** Tài khoản người duyệt bị khóa sau giao → vẫn cho NVLT nhận trả.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
