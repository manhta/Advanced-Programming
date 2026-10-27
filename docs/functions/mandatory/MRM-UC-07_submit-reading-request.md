# MRM-UC-07 — Tạo và gửi yêu cầu đọc

**Quy tắc hiện tại V19 —:** [CR-013](../../changes/CR-013_borrowing-approval-routing.md) thay thế phần duyệt mượn trong bản dự thảo dưới đây. Không có tử vong: Archivist phê duyệt/từ chối. Có bất kỳ tử vong: Director duyệt mọi hồ sơ trong lượt. Lý do bắt buộc nếu trên 5 hồ sơ hoặc có tử vong; Reader không chọn. Yêu cầu cũ giữ tuyến duyệt trước nâng cấp.

**Trạng thái:** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | READER |
| Nguồn và quy tắc | FR-08; D-02/03/06/09; BR-03/05 |
| Tiền điều kiện | Có HSBA công bố, không tử vong; người đọc là tài khoản đang đăng nhập. |
| Dữ liệu đầu vào | HSBA, mục đích 10–1000 ký tự đề xuất, ngày đọc duy nhất theo D-06, operation key; người/khoa lấy từ server. |

## Kích hoạt và luồng chính

1. Người dùng từ chi tiết HSBA chọn đăng ký; hệ thống nêu quy định đọc tại kho/trả trong ngày.
2. Chọn các ngày hợp lệ và nhập mục đích; có thể lưu nháp.
3. Xem tóm tắt một HSBA/nhiều ngày trước khi gửi.
4. Máy chủ kiểm tra HSBA, người, lịch, dữ liệu và quyền; cảnh báo khả dụng hiện tại không phải đảm bảo tương lai.
5. Ghi request + revision 1 bất biến + các ngày, state=SUBMITTED; trả mã yêu cầu và đưa vào hàng đợi NVLT.

## Luồng thay thế và ngoại lệ

E1: Tử vong/HSBA ngoài quyền → chặn cả submit trực tiếp.

E2: Ngày trùng/quá khứ/nghỉ/quá giới hạn → báo từng ngày, không gửi một phần.

A1: HSBA tạm không khả dụng → cho gửi kèm cảnh báo để NVLT xem xét theo đề xuất; không được giao khi còn bị chặn.

E3: Gửi lại cùng operation key → trả mã cũ, không sinh hai yêu cầu.

## Hậu điều kiện, giao dịch và audit

Yêu cầu SUBMITTED có owner, revision và; chưa giữ chỗ/đổi trạng thái vật lý. Audit tạo/gửi.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-07-01:** Một HSBA/3 ngày → một request, ba ngày mong muốn, chưa có lượt giao.
- **AC-07-02:** Đổi user_id trên form → server vẫn gắn chủ tài khoản hợp lệ, không tạo hộ.
- **AC-07-03:** Submit hai lần cùng khóa → một request duy nhất.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
