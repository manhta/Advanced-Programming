# MRM-UC-04 — Nhập kho, cập nhật metadata và đối soát dữ liệu ban đầu

Cập nhật hiện hành V17: bệnh án giấy/điện tử dùng một bản ghi, tra cứu chung, bản giấy mượn qua nút riêng. Metadata đã công bố được xem trực tiếp; khai báo chưa duyệt và nghiệp vụ nội bộ giữ phân quyền. Bổ sung bản giấy không tạo bệnh án mới. Xem [mô hình thống nhất](../../database/UNIFIED_MEDICAL_RECORDS.md) và [báo cáo kiểm thử](../../verification/UNIFIED_RECORDS_COMPLETION.md); các mô tả hai bảng/menu riêng bên dưới là lịch sử.

## Nhánh bổ sung CR-012 — Hồ sơ điện tử do Reader khai báo

Theo yêu cầu, Reader được nhập thông tin nhận diện người bệnh; mã bệnh án tự sinh. Archivist xác nhận/trả bổ sung rồi trình Director. Director duyệt thì công bố ngay, không có bước nhập kho giấy. Nhánh giấy hiện hữu vẫn do Archivist nhập và không tự chuyển sang nhánh điện tử. Chi tiết trạng thái, quyền, bảng mới và kiểm thử tại [ELECTRONIC_RECORDS.md](../../database/ELECTRONIC_RECORDS.md) và [báo cáo triển khai](../../verification/ELECTRONIC_RECORD_INTAKE.md). Quy tắc này bổ sung phạm vi cũ của UC-04, không chuyển quyền nhập hồ sơ giấy sang Reader.

**Bản triển khai:** xem [CR-005](../../changes/CR-005_five-feature-implementation.md) và [báo cáo năm chức năng](../../verification/FIVE_FEATURE_COMPLETION_REPORT.md) cho phạm vi đang chạy, hướng dẫn và kết quả kiểm thử. Nội dung dự thảo tổng thể dưới đây còn các nhánh mở rộng chưa triển khai.

**Trạng thái tài liệu gốc (không phải tiến độ code hiện tại):** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | NVLT; người đối soát được Giám đốc chỉ định |
| Nguồn và quy tắc | FR-07/14/15/17; D-04/17/19; BR-02/03/17/18 |
| Tiền điều kiện | Danh mục và vị trí tồn tại; khóa nghiệp vụ đã được xác nhận trước nhập chính thức; dữ liệu thử có nguồn. |
| Dữ liệu đầu vào | Số lưu trữ, năm đầu lưu/kho gốc, số bệnh án nếu có, mã bệnh nhân, tên, năm sinh/giới, khoa, ngày vào/ra viện, ngày đầu lưu, loại/tử vong, policy, vị trí; nguồn và ghi chú thiếu. |

## Kích hoạt và luồng chính

1. NVLT chọn nhập mới hoặc bản ghi hiện có; hiển thị form theo quyền.
2. Nhập thông tin từ nguồn đã đối soát; kiểm tra ngày ra≥vào, năm sinh hợp lý, ngày đầu lưu không phải ngày nhập giả.
3. Hệ thống kiểm tra khóa nghiệp vụ và báo nghi trùng; không tự gộp theo mã bệnh nhân.
4. NVLT xác nhận nhập kho sau kiểm tra vật lý, chọn vị trí hoạt động và ngày đầu lưu thực tế.
5. Lưu metadata, receipt nhập kho/lịch sử vị trí, policy snapshot, physical=IN_STORAGE và audit; lô ban đầu ghi người nhập/đối soát.

## Luồng thay thế và ngoại lệ

A1: Dữ liệu lịch sử thiếu trường không thiết yếu → đánh dấu MISSING/NEEDS_REVIEW; không dùng ngày hiện tại thay thế. Thiếu khóa, ngày đầu lưu hoặc vị trí → giữ staging, chưa công bố khả dụng.

E1: Có lượt giao mở → chặn sửa định danh, loại tử vong, ngày gốc và vị trí; nhận trả trước rồi điều chỉnh có căn cứ.

E2: Đổi loại thành tử vong → chặn khi có phiên CONFIRMED chưa xử lý; NVLT phải hủy/đối soát phiên, không bỏ chặn bằng metadata.

E3: Nghi trùng → giữ bản nháp/phiếu đối soát; không nhập chính thức hàng loạt.

## Hậu điều kiện, giao dịch và audit

Bản ghi hợp lệ được công bố; dữ liệu chưa đối soát không dùng cho giao. Sửa quan trọng ghi trước/sau, lý do. Vị trí hiện hữu đổi qua UC-06.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-04-01:** Hai HSBA cùng mã bệnh nhân nhưng khác đợt/khóa hợp lệ → lưu hai bản ghi.
- **AC-04-02:** Hai thao tác cùng tạo khóa nghiệp vụ → chỉ một bản ghi chính thức.
- **AC-04-03:** Dữ liệu cũ thiếu ngày đầu lưu → không tự tính thời hạn hoặc coi đã đối soát.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
