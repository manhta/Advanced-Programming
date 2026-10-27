# MRM-UC-02 — Quản lý tài khoản, vai trò và cấp lại thông tin xác thực

**Trạng thái:** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | ADMIN kỹ thuật; Giám đốc xác nhận quyền nhạy cảm |
| Nguồn và quy tắc | FR-02/04; D-08/10/22; BR-01/19/22 |
| Tiền điều kiện | ADMIN đăng nhập; cấp quyền nghiệp vụ nhạy cảm có căn cứ được Giám đốc xác nhận. |
| Dữ liệu đầu vào | Username duy nhất không phân biệt hoa/thường; họ tên, khoa, email nếu có, trạng thái, vai trò; lý do thay đổi; căn cứ cấp quyền nhạy cảm. |

## Kích hoạt và luồng chính

1. ADMIN mở danh sách tài khoản kỹ thuật, tạo hoặc chọn tài khoản.
2. Nhập thông tin và vai trò thuộc READER/ARCHIVIST/DIRECTOR/ADMIN; hệ thống kiểm tra trùng, khoa hoạt động và quyền người thao tác.
3. Nếu gán vai trò DIRECTOR/quyền nhạy cảm, yêu cầu căn cứ phê duyệt; ADMIN không tự cấp tùy ý.
4. Lưu APP_USER.role_id đơn, ROLE_CHANGE_HISTORY và audit; thay quyền/khóa làm mất hiệu lực quyền cũ ở lần thao tác tiếp theo.
5. Khi đặt lại mật khẩu, tạo quy trình cấp tạm an toàn, buộc đổi lần tiếp theo; reset cấp 2 không tự cấp vai trò DIRECTOR.

## Luồng thay thế và ngoại lệ

E1: Username trùng → báo lỗi trường, không tạo tài khoản thứ hai.

E2: Tự nâng quyền hoặc thiếu căn cứ → từ chối; Giám đốc ban đầu phải được bootstrap theo biên bản CNTT + người phụ trách.

E3: Tài khoản có lịch sử → chỉ khóa, không xóa; khóa người đang giữ HSBA không được xóa lượt giao.

E4: Khóa quản trị cuối cùng → cảnh báo và chặn nếu chưa có tài khoản thay thế hợp lệ.

## Hậu điều kiện, giao dịch và audit

Tài khoản/quyền cập nhật và giữ lịch sử; bí mật tạm không xuất trong danh sách. Có quy trình khôi phục quản trị khi mất tài khoản, không bỏ xác thực.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-02-01:** Khóa tài khoản đang mở trang duyệt → lần lưu sau bị chặn.
- **AC-02-02:** ADMIN không có quyền đọc HSBA gọi URL chi tiết → bị từ chối.
- **AC-02-03:** Đặt lại mật khẩu cấp 2 → chứng cứ xác thực cũ mất hiệu lực, quyết định cũ vẫn còn.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
