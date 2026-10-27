# Hoàn thành gộp bệnh án giấy và điện tử — V17

Ngày.

## Kết quả

Đã triển khai một bảng vật lý medical_records cho cả hai hình thức, với has_paper_copy và has_electronic_copy. Migration chuyển điện tử giữ nguyên ID/token/mã/version/nội dung/người và thời điểm xử lý; liên kết sự kiện, thông báo về bảng chung. electronic_records trở thành view chỉ đọc, không lưu một bản dữ liệu khác. Không thay đổi các migration V1–V16.

Đã gộp giao diện vào Tra cứu hồ sơ bệnh án, bỏ mục Hồ sơ điện tử trên navigation và chuyển link dashboard/chuông sang /{role}/records. Có bộ lọc Hình thức/Trạng thái xét duyệt, cột Hình thức và cột Mượn hồ sơ cho Reader. Bấm mã xem thông tin bệnh nhân đã công bố trực tiếp. Chỉ bản giấy đủ điều kiện có nút mượn, backend cũng chặn UUID chỉ điện tử. Ghi chú nội bộ/vị trí/nhật ký không nằm trong DTO công khai.

Archivist có thao tác Bổ sung bản giấy trên hồ sơ điện tử đã công bố, dùng form kho để cập nhật cùng bản ghi, giữ mã và lịch sử duyệt. Nhập mới bệnh án giấy chưa có vẫn giữ như trước. Không tự gộp hai dòng khác ID chỉ dựa trên họ tên; dữ liệu cũ nghi trùng nội dung cần đối soát riêng, không bị tự xóa khi nâng cấp.

Các URL điện tử cũ được giữ làm alias/redirect để không đứt bookmark và thông báo; không có danh sách lưu bệnh án thứ hai. File template form/xử lý nguồn bác sĩ còn ở templates/electronic, còn danh sách chung ở templates/records/list.html.

## Kiểm thử

- Maven offline package: BUILD SUCCESS.
- Chạy riêng FunctionalRequirementsCheck: **483/483 đạt**. Bao gồm kiểm thử cũ theo quyền mới và kiểm thử gộp: dòng bác sĩ ghi trực tiếp vào bảng chung; hai cờ; tìm kiếm giấy/điện tử, quyền với hồ sơ chưa công bố; mở metadata công bố mà không có loan; không lộ ghi chú cho Reader khác; không có menu riêng; trạng thái/bộ lọc/pagination; nút mượn giấy; chặn mượn điện tử ở backend; giữ chỗ khi có yêu cầu; hủy mở lại; bổ sung giấy không tăng số bản ghi, giữ code và lịch sử duyệt, ghi vị trí; ngăn nhập giấy mới bằng mã tự động; view cũ không cho ghi; mã tự động bỏ qua mã giấy lịch sử đã chiếm; hai cờ cùng false bị database chặn.
- ElectronicUpgradeCheck: **10/10 đạt** với schema bắt đầu ở V14 có giấy/yêu cầu/thông báo, nâng lên V15/V16 có điện tử/sự kiện/thông báo rồi gộp V17. Kiểm tra xung đột mã cố ý: migration thất bại và rollback, cả hai dữ liệu gốc còn nguyên; sau đối soát giả lập nâng thành công và xác minh ID/FK/nội dung giữ nguyên.
- Đã xác minh trực quan bằng trình duyệt: bảng chung hiển thị hai hình thức, nút mượn chỉ trên giấy và bấm mã mở thông tin bệnh nhân. Ảnh dùng dữ liệu giả, bao gồm tên có chuỗi script để kiểm tra escape; không phải bệnh án thật.
- Phiên preview đã dừng và schema bị xóa. Bộ cuối chạy riêng trên schema mới cũng đã tự xóa. Không áp dụng V17 vào public trong lần làm việc này. Không đếm các bộ kiểm tra khác chưa chạy lại vào kết quả trên.
- Sau kiểm tra trực quan đã chỉnh nhỏ cách xuống dòng nút mượn và nhãn giới tính; các luồng backend được xác minh lại bằng bộ 483 kiểm tra.

Log và ảnh: [483 kiểm tra](evidence/unified-records-2026-10-25/functional.txt), [10 kiểm tra migration](evidence/unified-records-2026-10-25/upgrade.txt), [bảng chung](evidence/unified-records-2026-10-25/search.png), [bấm mã mở thông tin](evidence/unified-records-2026-10-25/patient-info.png).

## Tự chạy và nghiệm thu

Khởi động lại ứng dụng bằng cấu hình dev đang dùng; Flyway áp dụng V17. Nếu có xung đột kỹ thuật thật, migration dừng để đối soát thay vì bỏ dữ liệu. Ctrl+F5 để tải lại giao diện. Kiểm tra từng role theo [Reader](../user-guides/reader.md), [Archivist](../user-guides/archivist.md), [Director](../user-guides/director.md).

Reader: Tra cứu → Hình thức Chỉ điện tử (không nút mượn); Giấy và điện tử (có nút nếu đủ điều kiện). Bấm mã phải mở thông tin; bấm Mượn hồ sơ mới mở đăng ký. Archivist/Director lọc trạng thái để kiểm tra/trình/duyệt như trước. Archivist thử bổ sung bản giấy cho một mã đã công bố, xác nhận database vẫn chỉ một id. Admin vẫn không được xem bệnh án.

Mô hình dữ liệu/ràng buộc: [UNIFIED_MEDICAL_RECORDS.md](../database/UNIFIED_MEDICAL_RECORDS.md).
