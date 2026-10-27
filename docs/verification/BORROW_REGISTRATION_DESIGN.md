# Tra cứu và đăng ký mượn —

## Kết quả

- Nhãn hình thức theo hai cờ: Giấy, Điện tử, Giấy và điện tử. Bộ lọc hỗ trợ từng hình thức và Có bản giấy (gồm cả hai).
- Bỏ cột trạng thái ở bảng tra cứu. Cột Mượn hồ sơ chỉ có nút nếu đủ điều kiện; cột Ghi chú bên phải ghi nhận đăng ký/xác nhận lịch/đã giao hoặc điều kiện chưa đáp ứng. Ghi chú nội bộ bệnh án không đưa vào cột này. Khai báo chưa công bố vẫn có nhãn xét duyệt tại Ghi chú.
- Thêm breadcrumb vào 25 template trang sau đăng nhập. Trang chủ chỉ hiện một nhãn, các trang khác có liên kết Trang chủ và tên trang hiện tại. Login giữ giao diện riêng.
- Trang đăng ký mượn thiết kế theo nội dung HTML tham khảo: tiêu đề/thời hạn, khối quy định xanh, vùng chọn thẻ, lý do, tóm tắt phiếu và các bước xử lý. Header/navigation chính giữ nguyên.
- Không dùng dữ liệu giả, hành vi thông báo thành công giả hoặc điều hướng SPA trong HTML tham khảo. Các API, form POST, CSRF, token chống gửi trùng và phân quyền hiện có được giữ.
- Lý do bắt buộc khi chọn trên 5 hồ sơ; hỗ trợ bỏ từng thẻ, Xóa tất cả, Làm lại và mở thông tin nhận diện từng hồ sơ. Giới hạn kỹ thuật 20 hồ sơ/lần ở backend vẫn giữ, không hiện bộ đếm tối đa trên UI.
- Không đổi schema hoặc dữ liệu thật trong lần chỉnh giao diện này. Không sửa Note.

## Xác minh

1. `./mvnw.cmd -o -B -ntp package`: BUILD SUCCESS.
2. `mrm.features.FunctionalRequirementsCheck -preview`: 493 kiểm tra đạt; sử dụng schema tạm và xóa sau khi đóng preview. Có kiểm tra hình thức chỉ giấy, bảng/cột mới, breadcrumb, điều kiện mượn, đăng ký/duyệt/giao/trả và phân quyền bốn role.
3. Trình duyệt với tài khoản/dữ liệu kiểm thử: chọn 6 hồ sơ hiển thị cảnh báo và bật required cho lý do; giảm xuống 5 thì bỏ required và cảnh báo; tóm tắt cập nhật; Làm lại đưa về 0; chọn 2 thẻ và mở rộng tải metadata; trang tra cứu hiển thị thứ tự cột mới. Kiểm tra trực quan ở viewport trình duyệt mặc định, không xác nhận mọi kích thước màn hình.

Ảnh: [đăng ký](evidence/borrow-design-2026-10-25/registration.png), [tra cứu](evidence/borrow-design-2026-10-25/search.png). Nhật ký kiểm thử: [functional.txt](evidence/borrow-design-2026-10-25/functional.txt).

## Vị trí chỉnh sửa

- `src/main/resources/templates/reading/new.html`: nội dung trang đăng ký.
- `src/main/resources/static/js/borrow-record-picker.js`: bộ chọn và tóm tắt động.
- `src/main/resources/templates/records/list.html`: cột tra cứu, nhãn và bộ lọc.
- `src/main/java/mrm/record/RecordSummary.java`: nhãn hình thức; `RecordRepository.java`: bộ lọc hình thức.
- `src/main/resources/templates/fragments/layout.html`: fragment breadcrumb; các trang gọi fragment bên trong main.
- `src/main/resources/static/css/portal.css`: breadcrumb và nhóm borrow-registration-page.
- `docs/user-guides/reader.md`: hướng dẫn giao diện và thao tác mới.

Để dùng trên phiên chạy chính, khởi động lại ứng dụng rồi Ctrl+F5 để cập nhật CSS/JavaScript và template.
