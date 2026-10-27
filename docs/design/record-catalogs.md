# Danh mục loại bệnh án và chính sách bảo quản — UC-03

Ngày triển khai:. Phạm vi: danh mục phục vụ UC-04, không tính hạn/tiêu hủy hoặc triển khai toàn bộ UC-18.

**Cập nhật hoàn thiện P2:** [báo cáo](../verification/P2_COMPLETION_REPORT.md) và CR-004 ưu tiên hơn các giới hạn lịch sử mô tả dưới đây. V6 đã cố định năm khoa, bổ sung version loại bệnh án và bốn chính sách mô phỏng DRAFT. Người dùng xác nhận D-19 cho học tập; bộ chọn đã tính ngày dự kiến và tạo snapshot bất biến, nhưng chưa lưu snapshot vào hồ sơ UC-04.

## 1. Dữ liệu và tổ chức mã nguồn

- Package `mrm.catalog`: DTO/Form, Repository dùng JdbcTemplate, Service có transaction, Controller dùng Bean Validation.
- Java migration `V5__CreateRecordCatalogs`: thêm `record_types`, `retention_policies`, `catalog_events`. Không sửa V1–V4 đã áp dụng.
- Template `catalog/record-types.html`, `catalog/retention-policies.html`; có liên kết từ trang chủ theo role.
- SecurityConfig hiện bảo vệ `/archivist/**` và `/director/**`; form POST dùng CSRF của Thymeleaf/Spring Security.

`record_types` gồm UUID, mã unique, tên, active, người tạo và thời gian. Sáu mục khởi tạo: Nội khoa, Ngoại khoa, Nhi khoa, Truyền nhiễm, Sản khoa, Phụ khoa. Mục hệ thống có `created_by = NULL`, audit actor `SYSTEM:V5`; không giả định một nhân viên đã nhập dữ liệu đó.

Loại bệnh án không phải khoa điều trị, không phải cờ tử vong và không tự quyết định thời hạn lưu. Chưa tự gán quan hệ loại–chính sách vì quy tắc chưa được xác nhận.

`retention_policies` gồm UUID, mã, tên, phiên bản, số năm dương, ngày bắt đầu/kết thúc hiệu lực, căn cứ/phạm vi, DRAFT/APPROVED, active, người/thời điểm tạo và xác nhận. Mã+phiên bản duy nhất. Không tạo sẵn số năm hoặc căn cứ giả.

`catalog_events` lưu loại đối tượng, ID, hành động, actor, chi tiết và thời gian. Tạo/sửa danh mục và audit cùng transaction; lỗi audit làm rollback nghiệp vụ. Đây là nhật ký danh mục tối thiểu, chưa phải toàn bộ UC-16 hoặc hệ thống audit chống can thiệp.

## 2. Luồng sử dụng

### Archivist — loại bệnh án

1. Đăng nhập `archivist1` bằng mật khẩu người dùng đã đặt.
2. Chọn **Quản lý loại bệnh án** ở trang chủ, URL `/archivist/record-types`.
3. Xem sáu loại khởi tạo; thêm mã/tên nếu cần. Mã chuẩn hóa uppercase, trim; chỉ 2–32 ký tự chữ không dấu/số/`_`/`-`.
4. Sửa tên hoặc trạng thái; mã giữ nguyên. Không có xóa cứng để tránh mất tham chiếu lịch sử.

### Archivist — chính sách lưu

1. Chọn **Quản lý chính sách bảo quản**, URL `/archivist/retention-policies`.
2. Nhập mã, tên, phiên bản dương, số năm dương, ngày bắt đầu và căn cứ/phạm vi áp dụng.
3. Ngày kết thúc không bắt buộc; nếu có phải từ ngày bắt đầu trở đi. Hai đầu khoảng thời gian đều tính bao gồm.
4. Lưu tạo DRAFT và active=false; chưa thể dùng cho hồ sơ nhập kho chính thức.
5. Khi thay quy tắc, tạo phiên bản mới, không sửa nội dung phiên bản đã lưu. Bản dự thảo nhập sai có thể để không xác nhận và tạo bản mới; chưa có thao tác hủy/sửa bản dự thảo.

### Director — xác nhận/ngừng dùng

1. Đăng nhập `director1`, chọn **Xác nhận chính sách bảo quản**, URL `/director/retention-policies`.
2. Đối chiếu căn cứ trước khi xác nhận. Thao tác xác nhận ghi approver/time, chuyển APPROVED và active=true.
3. Nếu cùng mã có phiên bản active/APPROVED chồng khoảng hiệu lực, thao tác bị chặn. Các phiên bản có khoảng không giao nhau được phép cùng active.
4. Có thể ngừng dùng phiên bản đã xác nhận, nhưng không xóa lịch sử. Nếu cần thay phiên bản đang mở hiệu lực, ngừng dùng phiên bản cũ rồi xác nhận phiên bản mới khi phù hợp.

Xác nhận này là xác nhận danh mục, không phải UC-10 phê duyệt yêu cầu đọc và chưa dùng secret cấp 2. READER/ADMIN không được vào hai chức năng; DIRECTOR không có quyền thêm/sửa loại bệnh án qua URL Archivist.

## 3. Phụ thuộc cần hoàn thiện ở UC-04/UC-18

- Khi chọn loại: chỉ lấy active=true cho lựa chọn mới; hồ sơ lịch sử vẫn tham chiếu được loại đã ngừng.
- Khi chọn chính sách: phải APPROVED, active=true và nằm trong khoảng hiệu lực theo ngày nghiệp vụ được chốt. `active=true` không tự chứng minh hôm nay đang trong khoảng hiệu lực.
- Lưu policy ID/version/snapshot vào hồ sơ; không cập nhật lại thời hạn hồ sơ cũ chỉ vì danh mục xuất hiện phiên bản mới.
- Còn chờ chốt: ngày gốc tính hạn, năm nhuận 29/2, nhiều nhóm cùng áp dụng, thời hạn theo căn cứ được duyệt và xử lý hồ sơ lịch sử thiếu thông tin.
- Ngừng dùng danh mục không sửa các hồ sơ có sẵn. Chưa có bảng hồ sơ trong lượt này nên chưa kiểm thử FK/snapshot xuyên suốt với UC-04.
- Đã khóa thao tác xác nhận theo mã chính sách để tránh hai phiên bản chồng khoảng cùng được xác nhận đồng thời. V6 bổ sung optimistic version cho loại bệnh án và kiểm thử hai thao tác sửa đồng thời.

## 4. Giới hạn hoàn thành

Hoàn thành phần quản lý hai danh mục bằng Java/Thymeleaf, lưu dữ liệu, xác nhận theo role, ngừng dùng và audit. Chưa coi toàn bộ UC-03/UC-18 đã nghiệm thu: chính sách nghiệp vụ thực tế và quy trình chọn/snapshot ở UC-04 còn chờ chốt/triển khai. Chưa cung cấp chính sách pháp lý mặc định.

Kết quả kiểm tra và hướng dẫn chạy kiểm tra: `docs/testing/CATALOG_VERIFICATION.md`.
