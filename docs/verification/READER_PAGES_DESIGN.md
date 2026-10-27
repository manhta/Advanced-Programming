# Giao diện thêm bệnh án và yêu cầu mượn —

## Phạm vi

Áp dụng ý tưởng phần nội dung từ hai HTML người dùng cung cấp. Header/navigation chính giữ nguyên; font nội dung ít nhất 15px. Không dùng CDN, dữ liệu mẫu, mã dự kiến EHR giả, modal tiến trình giả hoặc submit giả của bản tham khảo.

## Thêm hồ sơ bệnh án

- Form chia ba nhóm: nhận diện người bệnh, đợt điều trị, thông tin bổ sung; giữ đầy đủ trường và validation hiện có, bao gồm ghi chú tùy chọn.
- Khối hướng dẫn xanh, nhãn cấp mã tự động và thẻ xem trước trực tiếp theo giá trị form. Xem trước không lưu dữ liệu và không thay thế mã server cấp sau gửi thành công.
- JS cập nhật tên, năm sinh, giới tính, mã người bệnh, khoa/loại, ngày điều trị và dấu tử vong bằng textContent, không chèn HTML từ đầu vào. Nhập lại dùng reset HTML, khôi phục giá trị lúc tải form, giữ token/version.
- Form POST tạo/bổ sung, CSRF, chống gửi trùng, workflow Archivist kiểm tra/Director duyệt giữ nguyên.

## Yêu cầu mượn

- Dùng bố cục tiêu đề, hộp hướng dẫn, bộ lọc, bảng thẻ và nhãn trạng thái màu theo mẫu thứ hai.
- Bảng: Hồ sơ, Người yêu cầu, Ngày đăng ký, Ngày nhận, Trạng thái, Tiến trình xử lý, Ghi chú. Lý do mượn chuyển vào phần mở rộng ở Ghi chú; không bỏ dữ liệu mục đích khỏi trang.
- Theo dõi mở timeline thật hiện có; hủy vẫn là form POST có version/CSRF và chỉ xuất hiện cho Reader khi chưa có quyết định Director. Archivist/Director dùng Mở và xử lý.
- Ghi chú giải thích trạng thái qua `ReadingLabels.requestHint()`; không suy diễn hồ sơ đã giao/trả chỉ từ CONFIRMED. Không thay đổi mã trạng thái trong database.
- Giữ phân trang 10, mặc định 7 ngày, lọc tên/số lưu trữ/ngày và để trống ngày trên yêu cầu đã hủy. Không đưa lại tên bệnh nhân dưới mã hồ sơ, bộ đếm tổng hoặc trang điện tử riêng từ mẫu.

## Kiểm tra

- Maven offline package: BUILD SUCCESS.
- `FunctionalRequirementsCheck -preview`: 493/493 đạt; kiểm tra HTTP và database trong schema riêng, bao gồm các trường form, ghi chú không required, tạo/bổ sung/duyệt, phân quyền, danh sách ba role, hủy/tiến trình, lọc và phân trang. Schema đã được xóa sau đóng preview.
- Trình duyệt: kiểm tra cập nhật xem trước khi nhập/chọn, đánh dấu tử vong, reset form; bảng mới, mở Lý do mượn và liên kết tiến trình thật. Console không có lỗi trong phiên kiểm tra. Kiểm tra trực quan ở viewport mặc định, chưa xác nhận mọi thiết bị.
- Không thay đổi dữ liệu nghiệp vụ thật, schema hoặc Note trong lần chỉnh giao diện này.

Minh chứng: [thêm bệnh án](evidence/reader-pages-2026-10-25/new-record.png), [yêu cầu mượn](evidence/reader-pages-2026-10-25/requests.png), [log](evidence/reader-pages-2026-10-25/functional.txt).

## File chỉnh sửa

- `src/main/resources/templates/electronic/form.html`
- `src/main/resources/templates/reading/list.html` (dùng chung Reader, Archivist, Director)
- `src/main/resources/static/js/record-form-preview.js`
- `src/main/resources/static/css/portal.css`
- `src/main/java/mrm/workflow/ReadingLabels.java`
- `docs/user-guides/reader.md`

Khởi động lại ứng dụng và Ctrl+F5 để nạp template/CSS/JS mới.
