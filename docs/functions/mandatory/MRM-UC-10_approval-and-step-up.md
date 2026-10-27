# MRM-UC-10 — Phê duyệt/từ chối với xác thực cấp 2

**Quy tắc hiện tại V19 —:** [CR-013](../../changes/CR-013_borrowing-approval-routing.md) thay thế phần duyệt mượn trong bản dự thảo dưới đây. Không có tử vong: Archivist phê duyệt/từ chối. Có bất kỳ tử vong: Director duyệt mọi hồ sơ trong lượt. Lý do bắt buộc nếu trên 5 hồ sơ hoặc có tử vong; Reader không chọn. Yêu cầu cũ giữ tuyến duyệt trước nâng cấp.

**Trạng thái:** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | DIRECTOR đang hoạt động |
| Nguồn và quy tắc | FR-10; D-03/09/10/24; BR-06/07 |
| Tiền điều kiện | PENDING_APPROVAL, revision hiện hành; người duyệt khác owner, vai trò DIRECTOR hiện hành và tài khoản hoạt động. |
| Dữ liệu đầu vào | Request/revision/version, quyết định APPROVE/REJECT, lý do từ chối 1–1000 ký tự; chứng cứ xác thực cấp 2 một lần. |

## Kích hoạt và luồng chính

1. Người duyệt mở chi tiết đúng quyền, đọc toàn bộ nội dung revision và các ngày.
2. Chọn quyết định; hệ thống yêu cầu mật khẩu cấp 2 theo D-10.
3. Xác thực tạo bằng chứng ngắn hạn gắn user, revision, action; không dùng mật khẩu trong bản ghi quyết định.
4. Khi xác nhận, kiểm tra lại state, version, owner, vai trò DIRECTOR, trạng thái tài khoản, hạn/chưa dùng của bằng chứng.
5. Ghi một quyết định bất biến cho revision, snapshot căn cứ quyền DIRECTOR; đổi APPROVED hoặc REJECTED; nếu APPROVED tạo phiên PLANNED riêng từng ngày trong revision, tiêu thụ bằng chứng và audit trong cùng giao dịch.

## Luồng thay thế và ngoại lệ

E1: Sai/chưa có/khóa cấp 2 → không duyệt, ghi thất bại không chứa bí mật.

E2: Quyền bị thu hồi hoặc hết hạn giữa mở trang và ghi → từ chối, request còn chờ.

E3: Hai phiên đăng nhập DIRECTOR quyết định → chỉ một quyết định hợp lệ; người sau thấy kết quả đã xử lý.

E4: Request sửa/hủy hoặc chứng cứ dành revision khác → không áp dụng quyết định cũ.

E5: Email/kênh thông báo lỗi → không đảo quyết định đã lưu.

## Hậu điều kiện, giao dịch và audit

Quyết định và snapshot bất biến; chưa phải đã xác nhận lịch và chưa giao. Audit kết quả, căn cứ và xác thực; không ghi mật khẩu.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-10-01:** Chưa xác thực mà gửi action trực tiếp → bị chặn.
- **AC-10-02:** Tài khoản bị khóa hoặc mất vai trò DIRECTOR đúng lúc lưu → không tạo decision.
- **AC-10-03:** Cùng operation key retry → cùng decision; tự duyệt → bị chặn.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.

## Ranh giới với UC-01 và đổi secret cấp 2

Login tài khoản không yêu cầu secret cấp 2. Chỉ DIRECTOR có chức năng tự đổi secret phục vụ phê duyệt: xác thực hiện tại hoặc reset có căn cứ qua ADMIN UC-02, kiểm tra chính sách, lưu hash mới và vô hiệu proof chưa dùng. Không tự cấp role DIRECTOR qua đổi/reset secret. Không mặc định logout mọi session đăng nhập khi thay secret cấp 2; chính sách bổ sung này cần quyết định riêng. Proof chỉ một lần, khác HTTP session. Đây là phần thiết kế phê duyệt, không thuộc lát cắt login đầu tiên.
