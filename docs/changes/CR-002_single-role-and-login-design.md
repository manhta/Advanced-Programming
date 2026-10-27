# CR-002 — Xác nhận mỗi tài khoản một vai trò và thiết kế đăng nhập

Ngày, **CONFIRMED_BY_USER**: người dùng xác nhận mỗi tài khoản chỉ có một vai trò; giữ catalog bốn vai trò từ CR-001. Đây là xác nhận dự án học tập, không giả lập phê duyệt đơn vị y tế.

## Tác động thiết kế

- D-25 được xác nhận; tài khoản có role_id bắt buộc, tham chiếu một trong READER/ARCHIVIST/DIRECTOR/ADMIN. Nhiều tài khoản có thể cùng vai trò; không UNIQUE role_id.
- Lịch sử đổi vai trò dùng ROLE_CHANGE_HISTORY, không dùng USER_ROLE để cho phép nhiều quyền vai trò hiện hành. Access grant không cấp quyền trái với vai trò, không tạo vai trò thứ hai.
- Đổi role phải ghi audit/lịch sử, tăng auth_version và vô hiệu session cũ ở request tiếp theo. Quyết định/giao–trả cũ không mất.
- Chuẩn bị thiết kế UC-01 tập trung đăng nhập/đăng xuất/session, giao diện, dữ liệu, backend và kiểm thử. Chưa triển khai Java/SQL/cấu hình.
- Các ngưỡng D-22 và cơ chế xác thực cấp 2 D-10 vẫn là đề xuất, không tự xác nhận theo D-25.

## Tài liệu đọc

[Thiết kế đăng nhập](../design/authentication/login-design.md), [nhận xét biểu đồ](../design/authentication/diagram-review.md), [ma trận quyền](../requirements/permission-matrix.md).
