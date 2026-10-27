# CR-001 — Thu hẹp mô hình vai trò cho dự án học tập

Ngày:. **CONFIRMED_BY_USER** qua yêu cầu cập nhật tài liệu sau khi chọn bốn vai trò. Đây là xác nhận phạm vi của người dùng dự án, không giả lập phê duyệt của đơn vị y tế.

## Thay đổi đã xác nhận

- Giữ READER, ARCHIVIST, DIRECTOR, ADMIN.
- Không có tài khoản MANAGER hoặc DELEGATE. Giám sát trước đây của MANAGER chuyển DIRECTOR; NVLT vẫn đối soát trực tiếp.
- Chỉ DIRECTOR được duyệt sau xác thực cấp 2; ADMIN không duyệt thay. DIRECTOR vắng mặt thì yêu cầu chờ.
- Cấp/thu hồi ủy quyền UC-15 được hoãn, chuyển sang optional giữ lịch sử; AC/TC-15 DEFERRED, SCR-18 không sử dụng. Không đổi số UC/SCR còn lại.
- ERD/dictionary bản đầu bỏ APPROVAL_DELEGATION và delegation_id; chưa có schema nghiệp vụ nên không thực hiện migration xóa dữ liệu.

## Phần không tự suy ra đã được xác nhận

Mỗi tài khoản một vai trò là khuyến nghị riêng D-25, giữ PROPOSED. Ma trận/ERD dùng nó làm phương án cơ sở có ghi rõ cần duyệt trước code. Các lựa chọn cấp 2, scope dữ liệu, giới hạn lịch, khóa hồ sơ, báo cáo và NFR khác vẫn theo trạng thái đề xuất trước đó. KHNV có thể vẫn là bên tư vấn nghiệp vụ ngoài phần mềm, không thành role đăng nhập.

## Tài liệu ảnh hưởng

Phạm vi/sổ quyết định, ma trận quyền/quy tắc, UC tài khoản/duyệt/nhận trả/lịch sử/báo cáo, danh mục mandatory/optional, luồng, UI, ERD/dictionary, AC/TC/NFR, checklist và chỉ mục. Tài liệu SRS nguồn không bị sửa; phần ủy quyền FR-10 được ghi rõ là thu hẹp có căn cứ CR này.

Không sửa mã Java, Maven, cấu hình runtime hoặc PostgreSQL trong thay đổi tài liệu này.

## Cập nhật tiếp theo

Nội dung “D-25 chưa xác nhận” phía trên phản ánh thời điểm CR-001. Sau đó ngày người dùng đã xác nhận mỗi tài khoản một vai trò; xem [CR-002](CR-002_single-role-and-login-design.md). Không áp dụng lại trạng thái cũ khi đọc thiết kế hiện hành.
