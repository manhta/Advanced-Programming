# Dữ liệu hồ sơ mẫu —

Đã kiểm tra `mrm/public` trên PostgreSQL tại `127.0.0.1:5433`: trước nhập có 0 hồ sơ. Theo yêu cầu người dùng, đã nhập kho **tổng cộng 15 hồ sơ giả lập**, không phải 15 hồ sơ mỗi loại.

Vì hiện có 6 loại nên không thể chia đều tuyệt đối 15 mẫu. Phân bổ gần đều:

| Loại | Số mẫu | Số lưu trữ | Khoa phụ trách |
|---|---|---|---|
| Nội khoa | 3 | DEMO-MRM-001 đến 003 | Nội tổng hợp – YHCT – PHCN |
| Ngoại khoa | 3 | DEMO-MRM-004 đến 006 | Ngoại tổng hợp – Liên chuyên khoa |
| Nhi khoa | 3 | DEMO-MRM-007 đến 009 | Nhi – Chăm sóc sức khỏe sinh sản |
| Truyền nhiễm | 2 | DEMO-MRM-010 đến 011 | Truyền nhiễm |
| Sản khoa | 2 | DEMO-MRM-012 đến 013 | Nhi – Chăm sóc sức khỏe sinh sản |
| Phụ khoa | 2 | DEMO-MRM-014 đến 015 | Nhi – Chăm sóc sức khỏe sinh sản |

Đây là metadata hồ sơ giấy để học tập/kiểm thử phần mềm, không phải bản bệnh án lâm sàng hay thông tin bệnh nhân thật. Tên `Người bệnh giả lập 001` đến `015`, mã bệnh án `DEMO-BA-xxx`, mã người bệnh `DEMO-BN-xxx`; nguồn ghi rõ `DEMO_MRM_15`. Năm sinh Nhi thuộc –; Sản/Phụ có giới tính nữ. Ngày ra viện, đầu lưu; ngày vào viện trước ngày ra viện.

## Chính sách và vị trí

GENERAL phiên bản đã được duyệt trước đó đang inactive. Người dùng đã xác nhận **kích hoạt lại GENERAL và nhập kho 15 mẫu**. Chỉ đổi active của chính sách đã duyệt 10 năm; giữ nguyên người/thời điểm duyệt, ghi `REACTIVATE` vào catalog_events với actor kỹ thuật `SYSTEM:SAMPLE_IMPORT`. Đây không phải một quyết định duyệt mới của Director.

Tạo kho `DEMO_WAREHOUSE`, kệ `DEMO_SHELF`, sáu ngăn `DEMO_<mã loại>`, mỗi ngăn sức chứa 20. Tất cả mẫu là STORED / IN_STORAGE, chất lượng NORMAL, không tử vong; dùng GENERAL mô phỏng 10 năm, hạn lưu.

## Cách kiểm tra trên website

Khởi động ứng dụng theo hướng dẫn thông thường. Đăng nhập Archivist, vào Tra cứu hồ sơ; tìm `Người bệnh giả lập` hoặc một mã đầy đủ như `DEMO-MRM-001`. Lọc từng loại để thấy số lượng ở bảng. Mở chi tiết để xem chính sách, vị trí và lịch sử nhập kho; trang Vị trí có cây kho mẫu.

Reader cũng tra cứu được cả 15 mẫu đã nhập kho, nhưng không thấy vị trí kho chi tiết. Có thể dùng mẫu để đăng ký đọc và thử thông báo FR-11. Dữ liệu không tự sinh yêu cầu đọc hoặc thông báo.

## Cách thực hiện và xác minh

Chương trình Java `src/test/java/mrm/features/SampleMedicalRecordSeeder.java` được chạy chủ động một lần, không nằm trong luồng khởi động thông thường và không thêm migration V10. Nó gọi StorageService/RecordService để giữ validation, snapshot chính sách và lịch sử nhập kho. Sử dụng tài khoản Archivist đang hoạt động để ghi các thao tác nhập mẫu.

Kích hoạt chính sách, tạo vị trí và nhập 15 hồ sơ nằm trong một transaction; lỗi bất kỳ sẽ rollback toàn bộ. Có khóa nhập kho và kiểm tra lại database rỗng: chạy lại khi đã có hồ sơ sẽ dừng, không nhân đôi mẫu hoặc xóa dữ liệu hiện có.

Biên dịch thành công. Trong giao dịch đã kiểm tra 15 hồ sơ STORED/IN_STORAGE, snapshot 10 năm, 15 lịch sử vị trí và kết quả tìm tên tiếng Việt từ service dành cho Reader là 15. Sau commit, truy vấn nhóm xác nhận đúng 3/3/3/2/2/2. Kết quả/log tại [bằng chứng nhập mẫu](evidence/sample-records-2026-10-22/).

Chức năng phần mềm không thay đổi; không chạy lại toàn bộ bộ kiểm thử hồi quy vì đây là bổ sung dữ liệu mẫu qua service hiện có. Không sửa nội dung Note.
