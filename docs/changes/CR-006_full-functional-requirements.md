# CR-006 Mở rộng các yêu cầu chức năng SRS

Ngày. Người dùng cho phép trực tiếp hoàn thiện các FR trong SRS v0.2; có thể cân nhắc hoãn các NFR. Phạm vi thu gọn chỉ nhập kho trước đó được mở rộng sang đăng ký đọc, trình/duyệt, lịch, giao–trả, nhật ký, quản trị, báo cáo và kiểm kê.

## Xác nhận trực tiếp trong phiên làm việc

- Giữ bốn role READER, ARCHIVIST, DIRECTOR, ADMIN; mỗi tài khoản một vai trò.
- **Chỉ Director phê duyệt, tiếp tục hoãn ủy quyền** theo câu trả lời. Đây là ngoại lệ có chủ đích so với nhánh người được ủy quyền của FR-10; không thêm DELEGATE.
- **Hoãn toàn bộ chức năng gửi email**, cân nhắc tương lai, theo câu trả lời. FR-11 hiện chỉ thông báo kết quả/lịch trong ứng dụng, không tuyên bố đáp ứng yêu cầu email trong 5 phút.
- Giữ quản lý metadata hồ sơ giấy, không bản quét/toàn văn, không HIS trực tiếp, không chữ ký số, không tiêu hủy và không workflow khai thác tử vong/cơ quan ngoài như SRS.

## Mặc định triển khai để có thể kiểm thử

- Một yêu cầu thuộc một người Reader và một hồ sơ; 1–5 ngày đọc trong 30 ngày tới, kể cả các ngày liên tiếp. Chưa có lịch nghỉ riêng, không áp dụng giờ giao giả định.
- Archivist kiểm tra một yêu cầu mỗi lần, có danh sách tổng hợp và báo cáo lọc trạng thái; có thể trả bổ sung hoặc trình Director.
- Mật khẩu cấp 2 là mật khẩu riêng do Director thiết lập sau xác thực mật khẩu đăng nhập; băm PBKDF2, nhập lại ở từng quyết định. Năm lần sai trong cửa sổ 15 phút tạm chặn việc duyệt; không ghi mật khẩu vào log.
- Duyệt/từ chối toàn phiên bản yêu cầu. Sửa trước lần giao đầu tăng revision, mất hiệu lực quyết định/lịch cũ và phải trình duyệt lại. Sau lần giao đầu chỉ hủy phần chưa làm rồi đăng ký mới.
- Mỗi ngày một phiên, một hồ sơ/ngày tối đa một lịch CONFIRMED/READING/RETURNED. Chỉ giao trong ngày đã xác nhận; trả khác ngày cần ngoại lệ, giữ thời điểm trả thực tế. Không tự nhận trả khi hết ngày.
- Hủy không xóa lượt đã RETURNED và không được hủy khi đang có phiên READING. Hồ sơ hư hỏng/thiếu khi nhận trả trở thành UNAVAILABLE.
- Dữ liệu ban đầu nhập tay, chưa chọn nhập tệp. Không có API HIS; điều kiện nghiệm thu nhập tệp AC-06 không áp dụng.
- Báo cáo xem trên web, xuất CSV UTF-8 mở bằng Excel, in/lưu PDF bằng hộp in trình duyệt; không có dịch vụ tạo XLSX/PDF riêng. Xuất/in có audit.
- Admin chỉ được log xác thực/tài khoản, không bệnh án. Director có báo cáo giám sát/thời hạn nhưng không vị trí kho chi tiết. Archivist được báo cáo vị trí và audit nghiệp vụ đầy đủ.
- Kiểm kê chụp danh sách hồ sơ đang ở kho theo vị trí tại lúc mở đợt, không gồm READING. Đợt chỉ chốt sau khi đã ghi kết quả từng hồ sơ; không âm thầm sửa metadata/vị trí khi chốt.
- Ứng dụng không có chức năng xóa audit; chính sách lưu tối thiểu 10 năm được ghi rõ. Chưa chứng nhận bảo quản đủ 10 năm thực tế hoặc chống sửa bởi superuser PostgreSQL. Sao lưu/khôi phục, đo tải, HTTPS và nghiệm thu vận hành thuộc NFR chưa xác nhận.

Mặc định triển khai không phải biên bản phê duyệt của bệnh viện. Xem [báo cáo FR và kiểm thử](../verification/FR_COMPLETION_REPORT.md); tài liệu gốc giữ nguyên trong Downloads.
