# Reader — Hủy yêu cầu và lịch sử xử lý

Hoàn thành.

## Hành vi

Ba trạng thái chưa có quyết định của Giám đốc là SUBMITTED (Mới gửi), NEEDS_INFO (Cần bổ sung), PENDING_APPROVAL (Chờ duyệt). Chỉ các dòng này của Reader có nút Hủy yêu cầu ngay dưới trạng thái và liên kết Theo dõi yêu cầu. APPROVED, REJECTED, CONFIRMED, CANCELLED không có hai thao tác này trong bảng.

Trạng thái APPROVED hiển thị Đã duyệt, chờ lịch; REJECTED là Từ chối; CONFIRMED là Đã xác nhận lịch; CANCELLED là Đã hủy phần chưa thực hiện. Bỏ phiên bản kỹ thuật cạnh trạng thái và đoạn chú giải mã cuối bảng.

Reader mở trang theo dõi sẽ thấy trạng thái và các sự kiện xử lý (thao tác tiếng Việt, người xử lý, thời điểm, phiên bản, nội dung); không có form sửa/hủy hay thao tác giao–trả. URL lịch sử vẫn mở được cho chủ yêu cầu qua thông báo sau duyệt. Phân quyền xem chi tiết bệnh án vẫn giữ nguyên.

POST hủy kiểm tra người sở hữu, CSRF, khóa bản ghi và phiên bản; Reader chỉ hủy trước quyết định Giám đốc. Hủy ghi sự kiện, chuyển CANCELLED, trở về danh sách; không xóa yêu cầu. Nhân viên lưu trữ vẫn có nghiệp vụ hủy phần chưa thực hiện theo quy tắc giao–trả hiện có. API/service sửa mục đích cũ vẫn được giữ cho tương thích; giao diện Reader không hiển thị chức năng đó.

## Kiểm thử thực tế

- `.\mvnw.cmd -o -B -ntp package`: BUILD SUCCESS. Lệnh build không tự chạy bộ kiểm tra main bên dưới.
- Chạy riêng `mrm.features.FunctionalRequirementsCheck` bằng JDK của JAVA_HOME với classpath `target/classes;target/test-classes;target/catalog-verification-libs/*`: **316/316 đạt**.
- Đã kiểm tra HTML qua HTTP thật: trạng thái tiếng Việt, không có chú giải mã, dòng đã duyệt không có link theo dõi, dòng đang chờ có nút hủy/link theo dõi, trang Reader chỉ có lịch sử; trang xử lý Archivist/Director vẫn hoạt động.
- Với từng trạng thái Mới gửi/Cần bổ sung/Chờ duyệt: thiếu CSRF bị chặn; Reader khác bị từ chối; chủ yêu cầu hủy thành công và về danh sách; lịch sử ghi đúng reader1; gửi lại phiên bản cũ không nhân đôi sự kiện; sau hủy không có nút/link theo dõi.
- Sau Director duyệt, Reader hủy bị từ chối và trạng thái APPROVED được giữ nguyên. Luồng duyệt, xếp lịch, giao–trả và thu hồi quyền truy cập sau hủy bởi nhân viên tiếp tục đạt.
- Toàn bộ dữ liệu kiểm thử nằm trong schema riêng, tự xóa khi hoàn tất. Không có migration mới, không sửa dữ liệu ứng dụng trong schema public. Chưa kiểm tra trực quan bằng trình duyệt trong lần thay đổi này.

Log: [reader-request-history.txt](evidence/reader-request-history-2026-10-24.txt).

## Tự kiểm tra trên web

1. Khởi động lại ứng dụng để dùng bản mới; đăng nhập Reader và mở Yêu cầu.
2. Tạo yêu cầu mới: kiểm tra Mới gửi, nút Hủy yêu cầu dưới trạng thái và link Theo dõi yêu cầu.
3. Mở theo dõi: chỉ thấy lịch sử, không có form sửa/hủy. Quay lại danh sách và hủy: trạng thái chuyển tiếng Việt, các thao tác biến mất.
4. Tạo đăng ký khác; Archivist trình, Director duyệt. Reader không còn nút hủy/link theo dõi trên dòng đã duyệt; số lưu trữ vẫn mở metadata theo quyền.
5. Archivist xác nhận lịch: Reader thấy Đã xác nhận lịch và ngày nhận.
