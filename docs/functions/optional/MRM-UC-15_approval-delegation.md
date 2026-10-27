# MRM-UC-15 — Cấp, thu hồi và kiểm tra ủy quyền duyệt

**Trạng thái: DEFERRED — không thuộc phiên bản đầu từ.** Nội dung phía dưới chỉ giữ làm lịch sử thiết kế; vai trò DELEGATE và mọi AC-15 không áp dụng cho phiên bản bốn vai trò. Chỉ xem xét lại khi có yêu cầu mở rộng mới. Xem D-24 và CR-001.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | Giám đốc; ADMIN chỉ hỗ trợ theo căn cứ |
| Nguồn và quy tắc | FR-10; D-08/11; BR-07/19/22 |
| Tiền điều kiện | Giám đốc có quyền quản lý ủy quyền; người nhận có tài khoản hoạt động. |
| Dữ liệu đầu vào | Grantee, scope khoa/nội bộ, valid_from/valid_to, căn cứ, lý do cấp/thu hồi; người cấp do server. |

## Kích hoạt và luồng chính

1. Giám đốc chọn người nhận, phạm vi và khoảng thời gian hiệu lực.
2. Hệ thống kiểm tra bắt đầu<kết thúc, không tự ủy quyền tiếp và quyền quản lý của người cấp.
3. Lưu delegation bất biến về lịch sử, có thể thu hồi bằng sự kiện riêng.
4. Người nhận chỉ xuất hiện hàng đợi phù hợp khi còn hiệu lực; tại quyết định kiểm tra lại.
5. Thu hồi ghi revoked_at/người/lý do, vô hiệu chứng cứ cấp 2 liên quan quyền cũ; quyết định trước đó vẫn giữ căn cứ snapshot.

## Luồng thay thế và ngoại lệ

E1: Người không phải Giám đốc tự tạo → từ chối dù có ADMIN.

E2: Chồng phạm vi/thời gian → cảnh báo và chặn bản trùng hoàn toàn; không mở rộng quyền ngầm.

E3: Hết hạn khi đang mở trang → duyệt bị từ chối.

A1: Giám đốc là người đăng ký → chọn người được ủy quyền khác, không tắt quy tắc cấm tự duyệt.

## Hậu điều kiện, giao dịch và audit

Ủy quyền có hiệu lực rõ, thu hồi không xóa lịch sử quyết định. Cấp lại thông tin xác thực không đồng nghĩa cấp lại quyền.

Áp dụng [quy tắc chung](../mandatory/COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-15-01:** Đúng valid_to → hết hiệu lực theo [from,to).
- **AC-15-02:** Thu hồi khi request đang chờ → delegate không quyết định được.
- **AC-15-03:** Quyết định trước thu hồi → vẫn xem được actor/căn cứ lúc duyệt.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
