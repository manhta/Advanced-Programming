# Thông báo hủy, danh mục Archivist và bệnh án tử

Ngày thực hiện:. Migration hiện tại: V20.

## Những thay đổi đã hoàn thành

### Thông báo

- Các thông báo gắn với yêu cầu đã hủy không còn nút **Mở yêu cầu**, kể cả thông báo cũ phát sinh trước khi hủy. Vẫn có thể đánh dấu đã đọc.
- Khi hủy yêu cầu mượn, mọi Archivist đang hoạt động nhận thông báo **Yêu cầu với hồ sơ [mã] đã hủy**. Reader vẫn nhận tiêu đề ngắn **Yêu cầu đã hủy**.
- Thông báo công việc SUBMIT/AMEND/REVIEW_PENDING_APPROVAL của yêu cầu đó được ẩn khỏi danh sách, phân trang và số chuông chưa đọc. Dữ liệu gốc vẫn tồn tại trong `notifications`, phục vụ lịch sử; không xóa vật lý.
- Cùng một sự kiện chỉ tạo một thông báo cho mỗi người nhận. Thông báo và thay đổi trạng thái vẫn nằm trong cùng transaction.
- V20 bổ sung thông báo Archivist cho các yêu cầu đã hủy trước cập nhật; chạy lại ứng dụng không tạo trùng. Nút/liên kết gửi từ tab cũ cũng không chuyển vào yêu cầu hủy.

File chỉnh sửa: `src/main/java/mrm/notification/NotificationService.java`, `src/main/resources/templates/notification/inbox.html`, Java migration `V20__BackfillArchivistCancellationNotices.java`.

### Ba ô thống kê Archivist

| Ô thống kê | Đường dẫn | Nội dung |
|---|---|---|
| Chờ kiểm tra | `/archivist/work?queue=review` | Hai bảng: yêu cầu mượn SUBMITTED và bệnh án mới khai báo SUBMITTED |
| Hồ sơ trong kho | `/archivist/records?state=STORED&physicalStatus=IN_STORAGE` | Hồ sơ đã nhập kho, đang ở trong kho |
| Chờ giao | `/archivist/work?queue=handover` | Các phiên CONFIRMED, hiển thị ngày nhận và liên kết mở yêu cầu để giao |

Số lượng tại ô thống kê khớp danh mục được mở. Chờ giao đếm phiên chờ giao, không đếm hồ sơ điện tử không có bản giấy. Danh mục này bao gồm mọi ngày nhận đang chờ giao, kể cả lịch tương lai; chức năng giao thực tế vẫn kiểm tra đúng ngày.

Java: `DashboardService.java`, `HomeController.java`. HTML mới: `src/main/resources/templates/reading/archivist-work.html`. Header/navigation hiện tại được giữ nguyên.

### Dữ liệu bệnh án tử thực tế

Đã đọc trực tiếp `mrm/public`: trước cập nhật có **16 hồ sơ, 0 hồ sơ tử vong**. Chọn ngẫu nhiên 3 hồ sơ DEMO có bản điện tử, đã công bố/nhập kho và chưa bị giữ chỗ bởi yêu cầu mượn đang xử lý:

| Mã | Hình thức hiện tại | Kết quả |
|---|---|---|
| DEMO-MRM-006 | Chỉ điện tử | `deceased=true`, thêm ghi chú “Bệnh án tử” |
| DEMO-MRM-008 | Chỉ điện tử | `deceased=true`, thêm ghi chú “Bệnh án tử” |
| DEMO-MRM-009 | Giấy và điện tử | `deceased=true`, thêm ghi chú, bổ sung chính sách DECEASED và thời hạn lưu 30 năm |

Sau cập nhật vẫn **16 hồ sơ**, trong đó **3 tử vong**. Các ghi chú trước đó được giữ nguyên. Mã, người bệnh, ngày điều trị, vị trí và hình thức không bị đổi. Hồ sơ chỉ điện tử không có nút mượn giấy; dùng **DEMO-MRM-009** để kiểm tra màu trong bộ chọn và luồng xin duyệt tử vong.

Công cụ Java chạy riêng: `src/test/java/mrm/features/DeceasedSampleMaintenance.java`, chỉ thay đổi khi có `-apply`, không tự chạy khi khởi động ứng dụng. Nếu đã có hồ sơ tử vong, công cụ không chọn thêm ba hồ sơ. Dữ liệu trước khi sửa gồm cả snapshot chính sách được lưu tại `target/verification/deceased-backups/`; mỗi hồ sơ có một file JSON. Đã ghi sự kiện UPDATE trước/sau trong `record_events`. Không sửa hồ sơ không phải DEMO để tạo dữ liệu giả.

### Màu và ghi chú trên web

Bệnh án tử có nền hồng nhạt, viền đỏ và chữ nhận diện trong cả kết quả bộ chọn lẫn thẻ đã chọn; không chỉ dựa vào màu sắc. Tra cứu hiển thị nhãn **Bệnh án tử** trong cột Ghi chú dựa trên cờ tử vong, không cung cấp toàn bộ ghi chú nội bộ cho Reader. Cảnh báo bắt buộc lý do và tuyến Director của cả lượt vẫn giữ nguyên theo V19.

File giao diện: `reading/new.html`, `records/list.html`, `static/js/borrow-record-picker.js`, `static/css/portal.css`.

## Kết quả kiểm thử

- Build Maven: **BUILD SUCCESS**. JavaScript `node -check`: đạt.
- **591/591** kiểm tra chức năng/HTTP đạt. Bao gồm thông báo hủy có mã hồ sơ, ẩn thông báo việc cũ nhưng giữ DB, đồng bộ badge, bỏ nút mở ở cả Reader/Archivist, chặn truy cập khác chủ thông báo, ba đích dashboard trả 200 và đối chiếu số liệu, Reader bị chặn khỏi danh mục Archivist, nhãn/cấu hình màu tử vong.
- **14/14** kiểm tra nâng cấp dữ liệu đạt: V14→V20 bảo toàn dữ liệu, V20 thêm thông báo hủy cũ, khởi động lại không sinh trùng.
- Kiểm thử trình duyệt bằng dữ liệu giả: chọn một hồ sơ tử vong thấy màu riêng/lý do bắt buộc; ô Chờ giao mở đúng bảng; thông báo Archivist đã hủy có mã và chỉ còn Đánh dấu đã đọc.
- Schema thử nghiệm đã được xóa sau khi kiểm tra. Công cụ cập nhật mẫu đã dừng sau khi áp dụng và xác minh dữ liệu thật; không để lại server xem thử.

Bằng chứng: [functional.txt](evidence/archivist-notices-2026-10-26/functional.txt), [upgrade.txt](evidence/archivist-notices-2026-10-26/upgrade.txt), [build.txt](evidence/archivist-notices-2026-10-26/build.txt), [kết quả cập nhật mẫu](evidence/archivist-notices-2026-10-26/sample-update.txt), [kiểm tra dữ liệu sau cập nhật](evidence/archivist-notices-2026-10-26/sample-verification.txt).

Ảnh: [màu tử vong](evidence/archivist-notices-2026-10-26/deceased-colour.png), [danh mục chờ giao](evidence/archivist-notices-2026-10-26/handover-queue.png), [thông báo hủy](evidence/archivist-notices-2026-10-26/cancelled-notice.png).

## Cách kiểm tra trên máy của bạn

1. Dừng phiên ứng dụng đang dùng và chạy lại bằng cấu hình dev/biến DB hiện tại. V20 và ba mẫu đã được áp dụng vào mrm/public; không cần chạy lại công cụ tạo mẫu.
2. Đăng nhập Archivist, nhấp từng ô Chờ kiểm tra/Hồ sơ trong kho/Chờ giao; mở yêu cầu từ danh sách.
3. Đăng nhập Reader, tạo một yêu cầu thường rồi hủy khi còn chờ xử lý. Mở thông báo Reader: không còn nút Mở yêu cầu cho yêu cầu hủy.
4. Mở chuông Archivist: thấy thông báo hủy chứa mã hồ sơ; thông báo Có yêu cầu đọc cần kiểm tra của chính yêu cầu đó được ẩn.
5. Tra cứu DEMO-MRM-006/008/009 để kiểm tra nhãn. Chọn mượn DEMO-MRM-009: thấy màu riêng và phải ghi lý do; nếu cùng lượt chọn thêm hồ sơ thường, tất cả vẫn phải được Director duyệt.
