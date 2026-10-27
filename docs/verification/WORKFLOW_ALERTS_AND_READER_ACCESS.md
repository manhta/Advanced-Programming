# Thông báo công việc và quyền đọc —

Phạm vi đã xác nhận tại [CR-008](../changes/CR-008_workflow-alerts-and-reader-access.md).

## Cách sử dụng

1. Reader tra cứu và bấm số lưu trữ: nếu chưa có quyền xem chi tiết còn hiệu lực, hệ thống chuyển tới **Xin phép đọc hồ sơ**. Trang chỉ có số lưu trữ/tên để xác định hồ sơ, form mục đích/ngày đọc và liên kết theo dõi yêu cầu cũ. Không hiển thị thêm mã người bệnh, ngày điều trị hay thông tin chi tiết trên trang xin phép. Danh sách tra cứu vẫn giữ thông tin nhận diện và bộ lọc đã có.
2. Reader gửi: các Archivist đang hoạt động nhận chuông **Có yêu cầu đọc cần kiểm tra**. Mở thông báo đi tới yêu cầu trong trang Archivist.
3. Archivist chọn **Đủ điều kiện — trình Director**: các Director đang hoạt động nhận **Có yêu cầu đọc chờ phê duyệt**. Bấm Mở yêu cầu để duyệt bằng mật khẩu cấp 2. Trả bổ sung vẫn thông báo cho Reader; Reader gửi lại sẽ tạo thông báo công việc mới cho Archivist.
4. Sau Director duyệt, Reader bấm số lưu trữ ở danh sách yêu cầu hoặc tra cứu đều mở **Thông tin hồ sơ bệnh án**. Cột riêng **Theo dõi yêu cầu** mở lịch sử, trạng thái và lịch đọc. Hai liên kết phục vụ hai mục đích khác nhau.
5. Archivist xác nhận lịch sau duyệt; Reader nhận thông báo lịch như trước. Chuông cập nhật khi tải/chuyển trang, không gửi email hoặc thông báo đẩy.

Mỗi người nhận có trạng thái đã đọc riêng; một Archivist đọc thông báo không làm mất thông báo của Archivist khác. Thông báo cũ không biến mất khi công việc đã xử lý: mở yêu cầu để xem trạng thái mới nhất, hệ thống chặn xử lý lặp bằng trạng thái/version.

## Điều kiện xem chi tiết

Backend yêu cầu đúng Reader sở hữu yêu cầu, tài khoản ACTIVE, hồ sơ STORED không tử vong, yêu cầu APPROVED/CONFIRMED có người/thời điểm duyệt và còn ngày đọc không bị hủy từ hôm nay trở đi. Như vậy quyền có hiệu lực từ lúc duyệt, kể cả ngày đọc đầu chưa tới, và hết sau ngày cuối (giờ Asia/Bangkok).

Sửa đưa yêu cầu về SUBMITTED và xóa quyết định cũ; hủy cũng không cấp quyền. Nếu còn một yêu cầu khác được duyệt và còn hiệu lực cho cùng hồ sơ thì Reader vẫn được xem. Quyết định từ chối, yêu cầu đang gửi/chờ duyệt và phê duyệt của người khác không cấp quyền.

Controller chuyển tới trang xin phép khi chưa đủ quyền; service kiểm tra lại trước khi trả chi tiết, nên đoán URL không bỏ qua được bảo vệ. Draft hoặc UUID không tồn tại tiếp tục 404. Trang xin phép dùng DTO riêng với dữ liệu tối thiểu, tránh vòng lặp giữa trang chi tiết và form xin phép. Hồ sơ tử vong không có form đăng ký trực tuyến, tiếp tục quy trình riêng.

Chi tiết Reader là metadata đang được phần mềm lưu và cho phép công bố; không phải toàn văn bệnh án, bản quét hay thông tin điều trị chưa từng được nhập. Không mở các DTO nghiệp vụ kho, ghi chú/nguồn nội bộ hoặc audit cho Reader. Director vẫn có quyền giám sát metadata; Admin không có quyền bệnh án.

## Database và nâng cấp

Java Flyway **V10__RouteWorkflowNotifications** bổ sung `notifications.recipient_role`. Dữ liệu V9 có giá trị READER; thông báo mới của nhân viên ghi role nhận tại thời điểm tạo. Chỉ chủ thông báo có role hiện tại khớp role nhận mới đọc/đánh dấu được; Admin hoặc người đã đổi role không được hưởng quyền cũ.

V10 tạo thông báo cho công việc đang SUBMITTED hoặc PENDING_APPROVAL từ sự kiện mới nhất của revision hiện tại, gửi tài khoản có role phù hợp đang hoạt động. Không gửi lại các công việc đã duyệt/kết thúc. Unique người nhận/sự kiện chống lặp. Sau lần nâng cấp này chỉ sự kiện mới tạo thông báo; tạo tài khoản nhân viên sau đó không tự phát lại mọi thông báo cũ.

Không cần bảng cấp quyền riêng: quyền được suy ra từ dữ liệu yêu cầu và lịch hiện hành, nên việc sửa/hủy/hết hạn được áp dụng trực tiếp. V1–V9 không bị sửa; thay đổi schema tiếp theo sau V10 dùng V11 trở lên.

## Kiểm chứng

Kiểm tra tự động gồm gửi/gửi lại, nhiều Director, quyền sở hữu thông báo, mở đúng namespace theo role, đánh dấu tất cả độc lập, trước/sau duyệt, Reader khác, sửa, hủy, hết hạn, quyền trước ngày đọc đầu, DTO tối thiểu và liên kết hồ sơ/yêu cầu tách biệt. Các kiểm tra giao–trả, CSRF, mật khẩu cấp 2, rollback, báo cáo và kho được chạy lại theo bộ liên quan. Kết quả và log được ghi tại phần hoàn tất bên dưới.

Người dùng cần khởi động lại ứng dụng để tiến trình đang chạy nhận Java/template mới. Mỗi role nên dùng profile/cửa sổ trình duyệt riêng; các tab dùng chung cookie sẽ dùng chung tài khoản.

## Kết quả hoàn tất

V10 đã áp dụng thành công vào `mrm/public`**08:04:49 **. Kiểm kê sau thử xác nhận vẫn có 15 hồ sơ mẫu, 15 snapshot chính sách, 15 lịch sử nhập kho và yêu cầu thực hành của người dùng (1 yêu cầu/2 ngày/3 sự kiện). Không sửa trạng thái yêu cầu thực hành. Migration không tạo thông báo chờ duyệt cho yêu cầu đã APPROVED; thử một yêu cầu mới hoặc gửi lại để kiểm tra chuông công việc mới.

Build package thành công. Bộ FR chạy trong schema tạm đạt 199 kiểm tra; schema đã dọn. Các bộ danh mục/bootstrap/năm chức năng chạy tiếp trên bản mới đạt 49 + 26 + 96. Tổng cộng **370/370 kiểm tra đạt**. Đây là các chương trình kiểm tra Java được chạy trực tiếp, không phải số test JUnit của lệnh package. Các bài HTTP kiểm chứng đường dẫn, template, dữ liệu được/không được trả và quyền; lần này không bổ sung ảnh chụp trình duyệt.

| Bằng chứng | Kết quả |
|---|---|
| [FR, thông báo và quyền đọc](evidence/workflow-access-2026-10-22/workflow-permissions.txt) | 199/199 |
| [Danh mục](evidence/workflow-access-2026-10-22/workflow-catalog.txt) | 49/49 |
| [Bootstrap V1–V10](evidence/workflow-access-2026-10-22/workflow-bootstrap.txt) | 26/26 |
| [Năm chức năng](evidence/workflow-access-2026-10-22/workflow-five-features.txt) | 96/96 |
| [Database sau thử](evidence/workflow-access-2026-10-22/workflow-inventory-after.txt) | V10, 15 hồ sơ; giữ dữ liệu nghiệp vụ |

Các thao tác xác thực/tra cứu của kiểm thử có thể bổ sung audit hoặc sự kiện xem; không xóa nhật ký để khôi phục số đếm cũ. Phiên ứng dụng phụ phục vụ kiểm tra đã dừng; người dùng cần khởi động lại phiên dev đang chạy để dùng bản mới.
