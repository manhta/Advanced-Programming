# CR-003 — Ưu tiên đăng nhập tối giản

Ngày, người dùng xác nhận chỉ xác thực rồi chuyển tới trang theo role; tạm bỏ quản lý phiên, bắt buộc đổi password lần đầu và quên password. D-26 CONFIRMED_BY_USER.

Thiết kế hiện hành cho lượt đầu là [basic-login-design.md](../design/authentication/basic-login-design.md). “Hoãn quản lý phiên” được phân biệt với session HTTP kỹ thuật tối thiểu để trạng thái đăng nhập tồn tại giữa các request; không tự tạo cơ chế stateless hoặc chỉ redirect mà bỏ bảo vệ URL.

Giữ bốn role và một role/user đã xác nhận. Hoãn các luồng nâng cao và bộ test tương ứng; không xóa yêu cầu rộng khỏi lịch sử, không coi chúng đã đạt. Tài khoản thử không dùng trạng thái buộc đổi khi màn hình đổi chưa triển khai. Các D khác không tự được phê duyệt. Chỉ cập nhật tài liệu, chưa code/SQL/runtime change.
