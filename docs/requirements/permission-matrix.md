# Ma trận quyền — phiên bản bốn vai trò

**Cập nhật quyền theo yêu cầu người dùng:** Kiểm kê (danh sách, chi tiết, báo cáo/xuất kiểm kê) chỉ dành ARCHIVIST; DIRECTOR không còn quyền này. Ghi nhận này thay các mô tả cũ cho Director xem tổng hợp kiểm kê. [Chi tiết](../verification/DIRECTOR_REPORTS_AND_NOTICE_PRESENTATION.md).

Cập nhật, CR-001 / D-24: **người dùng đã xác nhận chỉ giữ READER, ARCHIVIST, DIRECTOR, ADMIN**. Không có MANAGER hoặc DELEGATE trong phiên bản đầu. Giám sát nghiệp vụ trước đây của MANAGER chuyển DIRECTOR; không có ủy quyền duyệt. Chi tiết còn đề xuất D-07/08/09/10; D-25 mỗi tài khoản một vai trò đã được người dùng xác nhận.

| Hành động | READER | ARCHIVIST | DIRECTOR | ADMIN |
|---|---|---|---|---|
| Đăng nhập/đăng xuất/tự đổi mật khẩu | Có | Có | Có | Có |
| Tra cứu metadata cơ bản | Toàn đơn vị theo D-07 | Toàn kho | Toàn đơn vị | Không |
| Xem vị trí chi tiết | Không | Có | Có nếu được cấp LOCATION_VIEW | Không |
| Tạo/sửa metadata/nhập kho | Không | Có | Không | Không |
| Quản lý danh mục/vị trí/chuyển vị trí | Không | Có | Chỉ xem theo quyền; xác nhận danh mục nghiệp vụ | Không |
| Đăng ký đọc | Của mình | Không mặc định | Không mặc định | Không |
| Theo dõi/bổ sung/sửa/hủy yêu cầu | Của mình, đúng trạng thái | Kiểm tra/trình, hủy phiên có lý do | Xem toàn đơn vị; không sửa hộ | Không |
| Kiểm tra/trả bổ sung/trình duyệt | Không | Có | Xem hàng đợi duyệt | Không |
| Duyệt/từ chối | Không | Không | Có sau cấp 2, không tự duyệt | Không |
| Xác nhận lịch/giao/trả | Không | Có | Không | Không |
| Xử lý ngoại lệ | Xem phiên mình, không đóng | Ghi diễn biến/nhận trả/đối soát theo quyền | Giám sát, xác nhận điều chỉnh có căn cứ | Không |
| Lịch sử nghiệp vụ | Yêu cầu/phiên mình | Nghiệp vụ kho | Toàn đơn vị theo quyền | Không |
| Audit chi tiết | Không | Khi có AUDIT_VIEW | Khi có AUDIT_VIEW | Không mặc định |
| Báo cáo vận hành | Chỉ màn hình của tôi | Kho được giao | Báo cáo quản lý toàn đơn vị | Không |
| Xuất tệp bổ sung | Không | Chỉ nếu chọn chức năng và cấp EXPORT | Tương tự | Không |
| Tạo/khóa/reset tài khoản, gán vai trò | Không | Không | Xác nhận căn cứ quyền nhạy cảm | Thực hiện theo căn cứ, không tự nâng quyền |
| Sao lưu/khôi phục | Không | Đối soát sau phục hồi | Cho phép mở lại, giám sát nghiệp vụ | Người vận hành được chỉ định |

## Phạm vi và trường dữ liệu

- BASIC: mã HSBA/mã bệnh nhân, họ tên, năm sinh, giới tính, khoa điều trị cuối, ngày điều trị, trạng thái tổng quát. READER không nhận vị trí, log người khác hoặc ghi chú nhạy cảm.
- OPERATIONAL: vị trí, nhóm/ngày lưu, tình trạng, người giao/nhận, lịch sử; ARCHIVIST theo nhiệm vụ, DIRECTOR theo quyền tương ứng.
- APPROVAL: chủ/khoa/mục đích/HSBA/ngày/ghi chú kiểm tra; chỉ DIRECTOR ra quyết định; không cần vị trí chi tiết.
- TECHNICAL: tài khoản/cấu hình/trạng thái dịch vụ. ADMIN không có quyền đọc HSBA chỉ vì quản trị kỹ thuật. Backup cần quyền file riêng vì có dữ liệu nhạy cảm.

Kiểm tra action/record/field cho mọi URL, kết quả tìm, tổng số, báo cáo và file. D-25 đã xác nhận mỗi tài khoản đúng một vai trò; chỉ READER tạo yêu cầu. Không có cơ chế gộp nhiều vai trò cho một tài khoản trong bản đầu. Cấm tự duyệt vẫn kiểm tra theo actor_id/owner_id để bảo vệ lịch sử khi vai trò thay đổi.

## Thay đổi/khóa vai trò

ADMIN chỉ chọn đúng một trong bốn vai trò theo D-25 đã xác nhận. Cấp DIRECTOR hoặc đổi vai trò nhạy cảm phải có căn cứ người phụ trách dự án/Giám đốc; không thể dùng ADMIN tự cấp quyền duyệt. Đổi vai trò/khóa tài khoản thu hồi session/proof cần thiết, kiểm tra quyền mới tại lần thao tác tiếp theo. Lịch sử tài khoản, quyết định và lượt đang mở không bị xóa. Người đọc bị khóa vẫn có thể trả hồ sơ qua NVLT.

Nếu DIRECTOR vắng mặt, yêu cầu tiếp tục PENDING_APPROVAL; ADMIN/ARCHIVIST không duyệt thay. Reset mật khẩu không phải ủy quyền. Bản học tập có thể dùng nhiều tài khoản thử, mỗi tài khoản một vai trò để kiểm tra; không thêm vai trò thứ năm.
