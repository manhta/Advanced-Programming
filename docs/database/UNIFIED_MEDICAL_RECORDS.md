# Bảng bệnh án thống nhất — V17

Áp dụng yêu cầu: một bệnh án/một đợt điều trị có một bản ghi trong medical_records. Một bệnh nhân có thể có nhiều bệnh án ở các đợt điều trị khác nhau. Không tự gộp chỉ vì trùng tên hoặc năm sinh.

## Dữ liệu chính

| Trường | Ý nghĩa |
|---|---|
| id | UUID kỹ thuật, giữ nguyên cả khi chuyển từ bảng điện tử cũ |
| record_code | Mã chung UNIQUE/NOT NULL/bất biến, dùng để mở và tra cứu |
| archive_number | Số lưu trữ bản giấy, có thể trống với chỉ điện tử |
| medical_record_number | Số bệnh án nguồn hoặc mã tự động của bác sĩ; không đồng nhất với mã người bệnh |
| has_paper_copy | Có bản giấy |
| has_electronic_copy | Có thông tin điện tử trong hệ thống |
| record_state | DRAFT (chưa công bố/nháp), STORED (đã nhập kho giấy), PUBLISHED (đã công bố chỉ điện tử) |
| physical_status | NOT_STORED/IN_STORAGE/READING/UNAVAILABLE cho giấy; NOT_APPLICABLE cho chỉ điện tử |
| submission_state | Trạng thái khai báo/kiểm tra/duyệt của nguồn bác sĩ; null với luồng nhập giấy cũ |
| reviewer/approver/published_at | Người, thời điểm kiểm tra/duyệt/công bố; giữ nguyên khi bổ sung bản giấy |

Archivist nhập giấy: hai cờ true. Reader khai báo mới: giấy false, điện tử true. Không cho hai cờ cùng false. Chỉ điện tử không có số lưu trữ, ngăn, ngày đầu lưu hay hạn bảo quản giấy. Bản giấy đã nhập kho phải có đủ thông tin lưu trữ theo ràng buộc cũ.

Mã các bệnh án giấy cũ được lấy từ số lưu trữ hiện hữu; nháp chưa có số dùng MRM-PAPER-<UUID không dấu gạch>. Bác sĩ tạo mới dùng MRM-REC-YYYY-000001. Bộ đếm nguyên tử theo năm chuyển sang record_code_counters, bỏ qua mã đã tồn tại và có UNIQUE bảo vệ. Không sửa record_code khi sửa thông tin hoặc bổ sung bản giấy. Số lưu trữ mới hiển thị phụ nếu khác mã chung.

## Chuyển dữ liệu V17

1. Mở rộng medical_records và thêm các cột quy trình duyệt.
2. Gán mã/cờ cho dữ liệu giấy hiện hữu. Kiểm tra xung đột UUID/create token/mã trước khi chép điện tử; xung đột làm migration dừng và rollback.
3. Chép toàn bộ bản ghi điện tử giữ UUID, token, mã, thông tin bệnh nhân, khoa/loại/ngày/ghi chú, version, người và thời điểm tạo/sửa/duyệt. APPROVED chuyển record_state=PUBLISHED; trạng thái khác là DRAFT.
4. Đổi FK của electronic_record_events và thông báo về medical_records. Không đổi ID của sự kiện/thông báo/yêu cầu đọc.
5. Bỏ bảng vật lý electronic_records sau khi chép dữ liệu, tạo view chỉ đọc cùng tên để giữ tương thích các truy vấn cũ. View này không lưu dữ liệu thứ hai và không cho INSERT/UPDATE.

Sau V17, hãy truy vấn medical_records làm nguồn duy nhất; không UNION medical_records với view electronic_records vì sẽ đếm trùng các bản ghi nguồn bác sĩ. Các bảng event/snapshot là lịch sử có chủ đích, không phải danh mục bệnh án đang hoạt động thứ hai.

Migration dùng transaction. Không xóa hoặc tự hợp nhất hai dòng bệnh án khác ID dựa trên tên, năm sinh hay ngày điều trị; nếu dữ liệu cũ đã nhập hai lần cùng một bệnh án nhưng khác mã, cần đối soát riêng. Việc gộp nơi lưu không tự xác định danh tính bệnh nhân.

## Bổ sung bản giấy vào hồ sơ có sẵn

Archivist tra cứu mã đã có → mở bản điện tử đã công bố → Bổ sung bản giấy → điền ngày đầu lưu, số lưu trữ nếu cần, chính sách, ngăn, chất lượng, căn cứ và lý do. Hệ thống cập nhật cùng UUID, giữ mã chung/nguồn tạo và lịch sử duyệt, bật hai cờ, ghi lịch sử vị trí. Chỉ cho thao tác sau Director duyệt; không tạo bệnh án thứ hai. Nhập mới qua form giấy bằng mã MRM-REC dành cho bác sĩ bị chặn và hướng dẫn dùng hồ sơ hiện có.

## Quyền và mượn

Tra cứu chung: Archivist xem để kiểm tra/kho; Director xem hồ sơ công bố và khai báo cần xử lý; Reader xem hồ sơ công bố và khai báo chưa duyệt do mình tạo. Reader khác không xem được khai báo chưa công bố. Admin không có quyền bệnh án.

Bấm mã hồ sơ công bố mở thông tin nhận diện/đợt điều trị ngay, không yêu cầu được duyệt mượn trước. Ghi chú nội bộ, vị trí kho và lịch sử nghiệp vụ không nằm trong DTO công khai. Chủ khai báo vẫn xem nội dung của mình theo quyền cũ; cán bộ kiểm tra/duyệt dùng trang chi tiết tương ứng.

Nút Mượn hồ sơ chỉ dành Reader khi có bản giấy, STORED/IN_STORAGE, chất lượng NORMAL, không tử vong và không có yêu cầu mượn đang hoạt động. Chỉ điện tử không có nút. Backend khóa và kiểm tra lại, nên truyền UUID điện tử trực tiếp vào POST cũng không mượn được. Phê duyệt/xếp lịch/giao–trả giấy tiếp tục theo quy tắc cũ. Quyền xem metadata và quyền nhận bản giấy là hai việc riêng.

```mermaid
erDiagram
 medical_records ||-o{ reading_requests: "mượn bản giấy"
 medical_records ||-o{ electronic_record_events: "lịch sử khai báo và duyệt"
 medical_records ||-o{ record_events: "lịch sử nghiệp vụ"
 medical_records ||-o{ record_location_history: "vị trí bản giấy"
 reading_requests ||-o{ reading_sessions: "giao và trả"
```

## V18 — Ngày sinh đầy đủ ()

`medical_records.birth_date` là DATE nullable. Khai báo mới/gửi lại của bác sĩ nhập đủ ngày, tháng, năm; lưu đồng thời `birth_year` tương ứng để tương thích bộ lọc năm và dữ liệu giấy. Constraint kiểm tra năm khớp, ngày sinh không sau ngày điều trị. Backend kiểm tra ngày lịch thực, năm nhuận và ngày sinh không ở tương lai. View `electronic_records` bổ sung birth_date ở cuối projection, tiếp tục read-only; snapshot sự kiện mới giữ ngày đầy đủ.

Migration không suy diễn ngày/tháng từ năm sinh: hồ sơ cũ giữ birth_date NULL, màn hình ghi chỉ biết năm. Nâng cấp dữ liệu thật diễn ra khi khởi động lại ứng dụng. Form nhập bản giấy của Archivist vẫn dùng độ chính xác năm; khi bổ sung bản giấy cho hồ sơ đã có ngày sinh, ngày sinh đầy đủ được giữ và năm phải nhất quán.
