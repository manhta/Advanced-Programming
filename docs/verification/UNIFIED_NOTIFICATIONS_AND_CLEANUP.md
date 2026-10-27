# Thông báo dùng chung và xóa thông báo đã xử lý

Ngày cập nhật:. Migration Java V21.

## Giao diện chung

Reader, Archivist, Director và Admin dùng cùng `templates/notification/inbox.html`. Các thông báo mượn hồ sơ và khai báo bệnh án đều có thẻ giống Archivist: tiêu đề, trạng thái tiến trình/đã đọc, họ tên + tên đăng nhập người yêu cầu, mã + tên người bệnh và ngày giờ đăng ký. Thông báo xác nhận lịch còn hiển thị ngày/phòng nhận. Nội dung yêu cầu bổ sung hoặc căn cứ từ chối được giữ để người nhận biết cần làm gì, nhưng không lặp chuỗi UUID/mục đích trong mô tả kỹ thuật cũ.

Admin dùng cùng giao diện nhưng vẫn không nhận dữ liệu bệnh án hoặc quyền duyệt. Không tạo thông báo lâm sàng giả cho Admin.

## Chờ xử lý khác với chưa đọc

Mặc định `/notifications` mở **Đang chờ xử lý**. **Đã xử lý** chứa kết quả/việc đã hoàn tất để xem hoặc xóa. **Tất cả** dùng khi cần đối chiếu; phân trang và bộ lọc chưa đọc giữ scope hiện tại.

| Người nhận | Thông báo còn chờ |
|---|---|
| Archivist | Yêu cầu mượn hoặc bệnh án mới còn SUBMITTED; yêu cầu đã APPROVED nhưng chưa xác nhận lịch |
| Director | Yêu cầu mượn/bệnh án vẫn PENDING_APPROVAL |
| Reader | Yêu cầu cần bổ sung vẫn NEEDS_INFO; yêu cầu mượn đã APPROVED nhưng còn chờ xác nhận lịch |

Bước đã hoàn tất, kết quả đã xác nhận, từ chối, hủy hoặc thuộc phiên bản cũ không còn nằm trong danh sách đang chờ. Phê duyệt còn chờ xác nhận lịch vẫn được giữ theo ma trận trên. Kiểm tra cả revision của yêu cầu mượn hoặc version của bệnh án để không làm sống lại thông báo cũ khi gửi lại nội dung.

Chuông đếm **thông báo đang chờ chưa đọc**. Bấm Đánh dấu đã đọc chỉ thay read_at; thông báo chưa xong vẫn nằm trong tab Đang chờ xử lý và không được xóa.

Khi Director phê duyệt yêu cầu mượn, Archivist nhận việc **Có yêu cầu mượn cần xác nhận lịch**. Các phê duyệt thông thường theo luồng hai bước cũ cũng có việc này. Sau confirm, việc tự chuyển sang đã xử lý. Với form gộp duyệt/lịch, hai bước hoàn tất trong cùng transaction nên không để lại việc chờ giả.

## Xóa thông báo

- Từng thông báo đã xử lý có nút **Xóa thông báo**.
- **Xóa tất cả đã xử lý** chỉ dọn hộp thư của người đang đăng nhập; giữ nguyên mọi thông báo còn chờ.
- Backend tự xác định tiến trình mới nhất. Không thể xóa việc đang chờ bằng cách sửa HTML/gửi ID; không thể xóa thông báo của tài khoản khác.
- POST cần CSRF. Xóa lặp không phát sinh thao tác lặp hoặc xóa nhầm việc đang chờ.
- Thực hiện bằng deleted_at (xóa khỏi hộp thư, không DELETE vật lý), giữ khóa sự kiện, yêu cầu, nhật ký và các bản ghi nghiệp vụ. Thông báo đã xóa không xuất hiện trong tab/phân trang/chuông và không mở lại bằng URL thông báo cũ.

## File và database

- `NotificationService.java`: bố cục dữ liệu chung cho cả yêu cầu mượn/bệnh án; trạng thái pending theo role + phiên bản; xóa riêng/hàng loạt, kiểm tra chủ thông báo.
- `NotificationController.java`: scope mặc định pending; POST `/{id}/delete`, `/delete-handled`.
- `templates/notification/inbox.html`: các tab, metadata chung, ngày/phòng nhận và nút xóa.
- `static/css/portal.css`: tab và nút xóa.
- `database/migration/V21__AddNotificationDismissal.java`: thêm notifications.deleted_at, chỉ mục, bổ sung việc xác nhận lịch còn thiếu cho yêu cầu đã duyệt trước cập nhật. Thông báo cũ mặc định chưa xóa, không tự dọn dữ liệu người dùng.

Không đổi recipient_role hoặc mở quyền dữ liệu chéo vai trò. Các sự kiện nguồn vẫn bất biến; thông báo đã xóa không được tạo lại khi thử gửi cùng sự kiện nhờ khóa unique hiện có.

## Kiểm thử

**BUILD SUCCESS; 722/722 kiểm tra chức năng/HTTP và 16/16 kiểm tra nâng cấp dữ liệu đạt.** Đã xác minh trên trình duyệt tab mặc định chỉ có việc đang chờ, không có nút xóa riêng; tab Đã xử lý có nút xóa. Reader dùng cùng bố cục metadata và thông báo đã đọc nhưng còn chờ vẫn được giữ lại. Các schema tạm đã xóa; không còn server xem thử.

Bằng chứng: [functional.txt](evidence/notification-lifecycle-2026-10-26/functional.txt), [upgrade.txt](evidence/notification-lifecycle-2026-10-26/upgrade.txt), [build.txt](evidence/notification-lifecycle-2026-10-26/build.txt), [đang chờ](evidence/notification-lifecycle-2026-10-26/pending.png), [đã xử lý](evidence/notification-lifecycle-2026-10-26/handled.png), [Reader](evidence/notification-lifecycle-2026-10-26/reader-pending.png). Kiểm tra gồm metadata thống nhất của Reader/Archivist/Director và bệnh án điện tử; Admin không có dữ liệu lâm sàng; đọc không xóa việc đang chờ; bảo vệ ID/CSRF; xóa riêng và hàng loạt không xóa pending; ghi soft-delete và audit nhưng giữ yêu cầu; chuyển việc từ Director sang Archivist; xác nhận lịch kết thúc việc; phiên bản cũ không được coi là việc đang chờ.

## Sử dụng trên máy phát triển

Khởi động lại ứng dụng bằng cấu hình đang dùng để Flyway tự áp dụng V21. Mở chuông: mặc định là việc còn chờ. Muốn dọn kết quả cũ, mở Đã xử lý và xóa từng thẻ, hoặc bấm Xóa tất cả đã xử lý. Không cần tạo lại tài khoản hoặc hồ sơ.
