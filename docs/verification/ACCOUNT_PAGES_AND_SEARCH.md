# Tách trang tài khoản và tìm kiếm —

**Bổ sung tìm căn cước:** trang quản lý hiện có ba bộ lọc Tên, Căn cước công dân và Loại tài khoản. Căn cước tùy chọn, nhập đủ 10 chữ số theo quy ước dự án, tìm chính xác trên employees.citizen_id và giữ số 0 đầu. Có thể dùng riêng hoặc kết hợp bất kỳ bộ lọc nào; điều kiện có giá trị được áp dụng đồng thời (AND). Khi trống, vẫn giữ tài khoản chưa có hồ sơ employees. Không sửa schema V13 hoặc thêm cột căn cước vào bảng danh sách. Phần mô tả hai bộ lọc bên dưới là mốc trước bổ sung này.

Build bổ sung thành công; bộ FR đạt **246/246**, schema tạm đã dọn. Có kiểm tra giữ số 0 đầu, tìm chính xác, kết hợp ba điều kiện, ID không có/sai độ dài, bỏ trống và HTTP giữ giá trị bộ lọc. Log: [account-citizen-search.txt](evidence/account-citizen-2026-10-22/account-citizen-search.txt).

Theo yêu cầu người dùng: phần tạo không kèm danh sách quản lý; phần quản lý chỉ có bảng tài khoản và bộ lọc, chỉnh sửa ở trang riêng mở từ tên.

| Trang | Đường dẫn | Nội dung |
|---|---|---|
| Tạo tài khoản | `/admin/accounts/new` | Form tài khoản/nhân viên, thông báo kết quả; không danh sách hoặc form sửa |
| Quản lý tài khoản | `/admin/accounts` | Hai bộ lọc và bảng ba cột, không form tạo/sửa tại dòng |
| Chỉnh sửa | `/admin/accounts/{id}/edit` | Một tài khoản cùng thông tin nhân viên, trạng thái, role, password mới tùy chọn; liên kết về danh sách |

POST tạo vẫn `/admin/accounts`, sau xử lý trở về trang tạo để hiển thị kết quả. POST sửa vẫn `/admin/accounts/{id}`, sau xử lý trở về trang sửa đúng tài khoản. Menu và nút trang chủ Admin dẫn đúng hai trang tạo/quản lý. Các route đều yêu cầu Admin; ID không tồn tại trả 404.

## Bảng và bộ lọc

- **Loại tài khoản:** mã role READER / ARCHIVIST / DIRECTOR / ADMIN.
- **Tên:** username bấm được mở trang sửa; họ tên hiển thị kèm để nhận diện.
- **Vai trò:** nhãn Bác sĩ / Nhân viên lưu trữ / Giám đốc / Quản trị viên tương ứng.

Ô **Tên** tìm theo một phần username hoặc họ tên, không phân biệt chữ hoa/thường. Tên có dấu được tìm theo dấu đã nhập; không bổ sung cơ chế bỏ dấu trong lần sửa này. Ô **Loại tài khoản** chọn role hoặc Tất cả.

| Tên | Loại tài khoản | Kết quả |
|---|---|---|
| Có | Tất cả | Tìm theo tên |
| Trống | Có | Tìm theo role |
| Có | Có | Đồng thời thỏa cả hai |
| Trống | Tất cả | Toàn bộ tài khoản |

Nút Xóa bộ lọc trở về danh sách đầy đủ. Bộ lọc và số lượng kết quả render lại cùng trang; không có kết quả sẽ hiện thông báo. Bao gồm cả tài khoản bị khóa/vô hiệu hóa để Admin còn quản lý được. SQL dùng tham số, `%`, `_`, `!` trong tên được xử lý là ký tự thường, không phải wildcard. Role lạ hoặc tên quá 200 ký tự bị từ chối.

Không thay schema V13 hoặc dữ liệu tài khoản thật. Quy tắc căn cước 10 chữ số, hồ sơ nhân viên, mật khẩu băm, phiên/version, CSRF và bảo vệ Admin cuối vẫn được giữ. Danh sách chỉ SELECT các trường nhận diện cần hiển thị; căn cước/ngày sinh/email ở trang sửa, không nằm trong bảng quản lý. Khởi động lại ứng dụng để dùng các route/template mới.

Kiểm thử tự động chạy trong schema tạm, gồm tách trang, liên kết sửa, dữ liệu trang sửa không có password, tìm tên, role, kết hợp, hai ô trống, không có kết quả, chuỗi SQL/wildcard, 404 và chặn Reader vào trang tạo/sửa. Kết quả hoàn tất được bổ sung bên dưới.

## Kết quả hoàn tất

Build package thành công. FunctionalRequirementsCheck đạt **240/240**; schema thử đã dọn, không đổi dữ liệu tài khoản thật. Lần này chạy bộ FR bằng Java trực tiếp, không gọi đây là số test JUnit hoặc cộng với các bộ lịch sử. Kiểm tra HTTP/template xác nhận các trang render và luồng tìm/sửa; không bổ sung ảnh chụp trình duyệt.

Log tại [account-pages-search.txt](evidence/account-pages-2026-10-22/account-pages-search.txt). Người dùng khởi động lại ứng dụng rồi mở hai menu riêng Tạo tài khoản/Quản lý tài khoản để nghiệm thu.
