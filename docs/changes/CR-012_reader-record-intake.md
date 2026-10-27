# CR-012 — Bác sĩ khai báo bệnh án mới, lưu trữ kiểm tra và trình duyệt

**Quyết định mới của người dùng:** bản điện tử cập nhật ngay khi Giám đốc duyệt; bỏ qua bản giấy trong nhánh bác sĩ khai báo. Thiết kế triển khai hiện tại mô tả tại [ELECTRONIC_RECORDS.md](../database/ELECTRONIC_RECORDS.md): lưu record_code ở electronic_records riêng, không ánh xạ medical_record_number của hồ sơ giấy. Các đề xuất chờ chọn thời điểm nhập kho và ánh xạ mã ở dưới là lịch sử, đã được thay thế.

Ngày:. Trạng thái: đề xuất thiết kế, chưa triển khai; đang làm rõ thời điểm nhập kho sau quyết định của Giám đốc.

## Phạm vi đã được yêu cầu

Bổ sung chức năng Reader (bác sĩ) khai báo hồ sơ bệnh án cho bệnh nhân mới. Archivist kiểm tra/xác nhận rồi trình Director; đồng thời giữ chức năng nhập bệnh án giấy chưa có dữ liệu trên hệ thống. Không thay luồng đăng ký mượn hồ sơ bằng luồng tạo bệnh án. Đây là khai báo thông tin phục vụ quản lý lưu trữ, không phải xây dựng bệnh án điện tử lâm sàng đầy đủ.

## Mã đề xuất

Mã hồ sơ mới do hệ thống tạo: `MRM-REC-000001`.

- MRM: ứng dụng; REC: record;: năm tạo mã theo Asia/Bangkok; 000001: số thứ tự trong năm, ít nhất 6 chữ số, không cắt khi vượt 999999.
- Mã này đề xuất dùng cho `medical_record_number` đối với hồ sơ mới do Reader khai báo. Không tự ghi đè số bệnh án có sẵn trên hồ sơ giấy/HIS của chức năng Archivist.
- Giữ UUID `medical_records.id` làm khóa kỹ thuật. Giữ `patient_code` tách biệt: định danh người bệnh nếu có, không phải mã cho từng đợt bệnh án. Một người có thể có nhiều bệnh án qua nhiều đợt điều trị.
- Số lưu trữ `archive_number` chỉ cấp khi nhập kho; cơ chế hiện có là `YYYY-MM-000001` theo tháng của ngày đầu lưu. Không đổi lại mã của 15 bệnh án mẫu hoặc các bệnh án đã có.
- Sinh mã ở backend bằng bộ đếm PostgreSQL khóa/cập nhật nguyên tử, có UNIQUE trong database. Không dùng COUNT(*)+1, mã từ trình duyệt, tên, căn cước hoặc khoa để tạo mã.
- Mã không đổi khi sửa tên/khoa/trạng thái/vị trí; không cấp lại cho hồ sơ khác khi hủy. Chấp nhận số thứ tự bị khuyết; dùng token gửi để chống tạo lặp khi bấm nút nhiều lần. Cần kiểm tra mã đã tồn tại, kể cả dữ liệu nhập thủ công.

## Phân công và luồng đề xuất

1. Reader nhập thông tin nhận diện người bệnh: họ tên, năm sinh (dữ liệu hiện tại của dự án), giới tính; mã người bệnh nếu có. Khoa, loại và đợt điều trị có thể khai báo nếu đã biết. Không cho chọn vị trí kho, chính sách lưu hoặc tự nhập kho. Hệ thống tự cấp mã khi lưu/gửi thành công, không cấp chỉ vì mở form.
2. Archivist kiểm tra hồ sơ và nội dung khai báo, trả bổ sung nếu thiếu; đủ điều kiện thì trình Director. Lưu người xử lý, thời gian, nội dung và phiên bản.
3. Director duyệt hoặc từ chối; hồ sơ không xuất hiện như một hồ sơ đã nhập kho chỉ vì bác sĩ tạo.
4. Đề xuất: sau duyệt, Archivist xác nhận thực nhận giấy, bổ sung thông tin nhập kho còn thiếu, chọn ngăn/chính sách và nhập kho. Cấp số lưu trữ tại bước này. Phương án khác là Archivist hoàn tất toàn bộ thông tin kho trước khi trình và Director duyệt sẽ nhập kho ngay; cần xác nhận lựa chọn nghiệp vụ trước triển khai.
5. Reader theo dõi hồ sơ do mình khai báo và kết quả xử lý; không được xem/sửa hồ sơ nháp của bác sĩ khác bằng cách đoán ID. Quyền chỉnh sau gửi hoặc sau được trả bổ sung cần theo trạng thái và phiên bản. Hồ sơ đã nhập kho vẫn áp dụng quyền đọc bệnh án đã thống nhất, không cấp quyền vĩnh viễn chỉ vì là người tạo.

Luồng nhập hồ sơ giấy của Archivist được giữ; không bắt nhập lại hồ sơ đã tồn tại. Phạm vi cần làm rõ tiếp là có áp dụng Director duyệt cho nhánh giấy cũ hay giữ cách nhập kho hiện có. Không đổi chức năng nhập kho cũ khi chưa xác định quy tắc này.

## Công việc triển khai sau khi rõ nghiệp vụ

Migration Java mới (không sửa V1–V14), bộ đếm mã và bảng/sự kiện duyệt bệnh án; form Reader chỉ chứa trường phù hợp; phân quyền backend; danh sách chờ Archivist và Director; thông báo nội bộ; chặn nhập kho trước duyệt cho nhánh Reader; nhật ký và kiểm soát phiên bản; cập nhật hướng dẫn từng vai trò.

Kiểm thử cần có: sinh mã đồng thời không trùng; gửi lại không nhân bản; Reader không truyền được trường kho/quyền duyệt; chỉ sửa/xem bản của mình theo trạng thái; sai vai trò bị chặn; không bỏ qua Archivist/Director; yêu cầu trả bổ sung và từ chối; danh mục/vị trí ngừng dùng hoặc đầy; nhập kho đúng quyền; dữ liệu cũ không bị đổi mã.
