# MRM-UC-05 — Tra cứu và xem chi tiết HSBA

Cập nhật hiện hành V17: bệnh án giấy/điện tử dùng một bản ghi, tra cứu chung, bản giấy mượn qua nút riêng. Metadata đã công bố được xem trực tiếp; khai báo chưa duyệt và nghiệp vụ nội bộ giữ phân quyền. Bổ sung bản giấy không tạo bệnh án mới. Xem [mô hình thống nhất](../../database/UNIFIED_MEDICAL_RECORDS.md) và [báo cáo kiểm thử](../../verification/UNIFIED_RECORDS_COMPLETION.md); các mô tả hai bảng/menu riêng bên dưới là lịch sử.

## Tra cứu nhánh điện tử — cập nhật

Hồ sơ do bác sĩ khai báo được tra cứu tại menu Hồ sơ điện tử, tìm tên/mã và lọc trạng thái. Reader chỉ thấy hồ sơ mình khai báo, Archivist/Director thấy để kiểm tra/duyệt. Danh sách hồ sơ giấy có liên kết sang trang mới; không trộn hồ sơ điện tử không có bản giấy vào danh sách đăng ký mượn/kho giấy. Công bố không tự mở quyền xem dữ liệu bệnh nhân cho bác sĩ khác.

**Bản triển khai:** xem [CR-005](../../changes/CR-005_five-feature-implementation.md) và [báo cáo năm chức năng](../../verification/FIVE_FEATURE_COMPLETION_REPORT.md) cho phạm vi đang chạy, hướng dẫn và kết quả kiểm thử. Nội dung dự thảo tổng thể dưới đây còn các nhánh mở rộng chưa triển khai.

**Trạng thái tài liệu gốc (không phải tiến độ code hiện tại):** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | READER, NVLT, Giám đốc theo quyền |
| Nguồn và quy tắc | FR-03/04; D-07; BR-01/03 |
| Tiền điều kiện | Có quyền tra cứu trong phạm vi đã cấp; bản ghi đã công bố. |
| Dữ liệu đầu vào | Từ khóa/mã, tên, khoa, khoảng ngày, loại, trạng thái; bộ lọc vị trí chỉ vai trò có quyền; page/size hợp lệ. |

## Kích hoạt và luồng chính

1. Người dùng nhập bộ lọc, hệ thống chuẩn hóa và kiểm tra khoảng.
2. Truy vấn trong phạm vi bản ghi, phân trang, sắp xếp ổn định.
3. Hiển thị metadata cơ bản; vị trí và lịch sử chỉ xuất hiện khi có quyền.
4. Mở chi tiết; kiểm tra quyền lại, ghi sự kiện xem theo D-20.
5. Nếu là HSBA thông thường và đủ quyền tạo yêu cầu, chuyển UC-07; tử vong chỉ hiển thị hướng dẫn liên hệ theo quy trình ngoài.

## Luồng thay thế và ngoại lệ

A1: Không có kết quả → danh sách rỗng kèm bộ lọc, không thông báo lỗi.

E1: ID ngoài phạm vi hoặc URL đoán → không trả dữ liệu, kể cả JSON/thuộc tính ẩn.

E2: Tìm tên không dấu → theo chính sách tìm kiếm đã xác nhận; bản đầu hỗ trợ có dấu/không phân biệt hoa thường, không tự cam kết tìm không dấu.

## Hậu điều kiện, giao dịch và audit

Không thay đổi nghiệp vụ; audit xem chi tiết chỉ chứa định danh cần thiết. Không thể suy ra vị trí từ báo lỗi hoặc danh sách bác sĩ.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-05-01:** Bác sĩ tìm/mở chi tiết → không có kho/tủ/kệ/ngăn trong dữ liệu trả.
- **AC-05-02:** Khoảng ngày đảo ngược → báo lỗi trước khi truy vấn.
- **AC-05-03:** Kết quả 100 bản ghi → phân trang không lặp/mất khi dữ liệu ổn định.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
