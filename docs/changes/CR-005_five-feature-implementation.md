# CR-005 — Triển khai nhóm năm chức năng ưu tiên

Ngày:. Căn cứ: người dùng cho phép trực tiếp viết code và hoàn thiện năm chức năng ưu tiên.

Trạng thái: **IMPLEMENTED_DEFAULT** đối với các lựa chọn kỹ thuật/nghiệp vụ tối giản dưới đây. Đây là phương án được chọn trong phạm vi giao việc, để người dùng đọc và phản hồi; không ghi thành xác nhận riêng từng quy tắc bởi người dùng hoặc phê duyệt của cơ sở y tế.

## Phạm vi và quy tắc đang chạy

1. UC-01 cơ bản giữ nguyên; UC-03 tiếp tục theo CR-004. Bổ sung UC-04, UC-05 và UC-06. Không triển khai các luồng yêu cầu đọc/giao–trả trong đợt này.
2. Quản lý metadata hồ sơ giấy bắt đầu từ nhập kho; việc kiểm tra hồ sơ giấy trước nhập vẫn do Archivist thực hiện ngoài ứng dụng.
3. Cây vị trí tối giản ba cấp **Kho → Kệ → Ngăn**, thay phương án bốn cấp trong dự thảo. Mỗi ngăn chứa nhiều hồ sơ; sức chứa tùy chọn, không nhập là không giới hạn. Mã, cấp và cha bất biến; không xóa cứng.
4. UUID là khóa nội bộ. Số lưu trữ duy nhất toàn ứng dụng; tự cấp `YYYY-MM-NNNNNN` theo tháng của ngày đầu lưu thực tế, hoặc nhập số nguồn hợp lệ. Không tái cấp số đã dùng; số có thể có khoảng trống sau các kiểm thử/giao dịch đã cam kết rồi dọn dữ liệu.
5. Một mã người bệnh có nhiều đợt điều trị. Không gộp theo tên. Cùng số bệnh án + mã người bệnh (kể cả cùng chưa có mã) + ngày vào/ra là nghi trùng và bị chặn nhập chính thức; lưu nháp để đối soát. Thao tác nhập/sửa chính thức được tuần tự hóa khi kiểm tra trùng và sức chứa.
6. Nháp cần họ tên và dữ liệu biểu mẫu hợp lệ, giữ NULL với thông tin chưa biết; chưa công bố cho Reader/Director, chưa tính hạn lưu. Khi nhập chính thức cần số bệnh án, năm sinh, khoa, loại, ngày vào/ra/đầu lưu, vị trí ngăn, chính sách hợp lệ và nguồn đối soát. Giới tính có thể UNKNOWN; mã người bệnh tùy nguồn.
7. Ngày vào ≤ ra ≤ đầu lưu ≤ ngày hiện tại theo Asia/Bangkok; năm sinh không sau năm vào viện. Sau nhập chính thức không đổi số lưu trữ/ngày đầu lưu; chuyển vị trí qua chức năng riêng.
8. Snapshot danh mục/chính sách được lưu cùng hồ sơ. Sửa thông tin người bệnh mà giữ nguyên phân loại sẽ giữ snapshot cũ, kể cả danh mục đã ngừng dùng. Chủ động đổi khoa/loại/nhóm bảo quản phải chọn lại tập chính sách hợp lệ; ghi lý do và trước/sau.
9. Tử vong phải khớp có nhóm DECEASED. Hạn lưu vẫn là quy tắc mô phỏng học tập của CR-004, không tự tiêu hủy.
10. Tách trạng thái bản ghi DRAFT/STORED, lưu chuyển NOT_STORED/IN_STORAGE/UNAVAILABLE/READING và chất lượng NORMAL/DAMAGED/INCOMPLETE. Hồ sơ thiếu chỉ được nhập lần đầu dưới dạng nháp; hồ sơ hư hỏng nhập kho ở UNAVAILABLE. Phục hồi chất lượng không tự phục hồi khả dụng; cần xác nhận riêng kèm lý do.
11. Reader/Director xem metadata cơ bản của hồ sơ STORED; không nhận vị trí chi tiết, ghi chú/nguồn đối soát nội bộ, snapshot chính sách hoặc nhật ký vận hành. Admin không được dữ liệu bệnh án. Chỉ Archivist nhập/sửa/quản lý kho/chuyển vị trí.
12. Tìm mã chính xác, tên chứa chuỗi có/không dấu/hoa thường; lọc năm sinh, khoa, loại, khoảng ngày ra viện, trạng thái; phân trang 1–100 dòng và sắp xếp theo danh sách cho phép.
13. Gửi lại cùng phiếu nhập/nội dung trả về hồ sơ cũ; gửi lại cùng thao tác chuyển không tạo lịch sử thứ hai. Sửa bằng biểu mẫu cũ bị từ chối, không âm thầm ghi đè.
14. Ghi audit và lịch sử vị trí cùng transaction với dữ liệu chính. Khóa ở database xử lý cạnh tranh; khóa chung kho phù hợp quy mô bài tập, chưa tối ưu thông lượng lớn.

## Tài liệu đối chiếu

- [Báo cáo triển khai, nguyên lý, hướng dẫn và kiểm thử](../verification/FIVE_FEATURE_COMPLETION_REPORT.md).
- [Checklist](../../PROJECT_CHECKLIST.md).
- Các tài liệu use case/ERD tổng thể trước đây là dự thảo cho phạm vi rộng hơn. Với năm chức năng đang chạy, ưu tiên nội dung CR-005 và báo cáo triển khai khi có khác biệt.
- Những đề xuất ngoài phạm vi này trong decision-log không tự được duyệt.
