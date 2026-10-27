# Ngày sinh người bệnh —

## Kết quả

Form thêm/bổ sung hồ sơ của Reader thay ô năm sinh bằng Ngày / Tháng / Năm, ba input số trên một hàng ở vị trí cũ. Nhãn, placeholder, aria-label và giới hạn số tương ứng; chữ từ 15px. Xem trước kết hợp ba ô thành dd/MM/yyyy. Ghi chú vẫn tùy chọn, navigation giữ nguyên.

Backend ghép ngày lịch thật, bắt đủ ba thành phần, từ chối 31/4, 29/2 không nhuận, ngày sinh tương lai hoặc sau ngày điều trị. Sửa hồ sơ điền lại ba phần từ ngày đã lưu. Date và year được ghi cùng một câu lệnh để constraint không thấy trạng thái trung gian không nhất quán. Trang thông tin bệnh nhân, khai báo và chi tiết của nhân viên hiển thị ngày đầy đủ nếu có.

Migration Java V18 thêm `birth_date DATE` vào bảng thống nhất và view tương thích; giữ `birth_year` để tìm kiếm và dữ liệu chỉ biết năm. Không tự điền 01/01 cho dữ liệu cũ; vẫn NULL cho ngày đầy đủ. Bổ sung/gửi lại khai báo cũ cần ngày tháng chính xác. Form bản giấy chưa thay phạm vi nhập năm sinh.

## Kiểm thử

- Maven offline package: BUILD SUCCESS.
- **530/530** FunctionalRequirementsCheck đạt trong schema tạm, đã dừng preview và xóa schema. Ca bổ sung gồm lưu ngày nhuận, view/snapshot, từ chối ngày không tồn tại/tương lai/sau điều trị/thiếu phần, HTTP bỏ qua JS không tạo dữ liệu sai, mở lại ba ô, sửa năm/ngày đồng thời, hiển thị công khai dd/MM/yyyy, constraint không cho lệch năm, dữ liệu cũ giữ độ chính xác năm.
- **11/11** ElectronicUpgradeCheck đạt: V14/V15/V16 lên V18, giữ lịch sử/FK và year-only, kiểm tra rollback xung đột V17 vẫn đúng. ERROR xung đột merge trong log là ca rollback có chủ đích.
- Trình duyệt: ba ô cùng hàng, báo không hợp lệ; sửa sang cập nhật xem trước. Đã kiểm tra ở viewport mặc định.
- Không đổi schema/dữ liệu thật hoặc Note trong phiên này; V18 sẽ chạy khi user khởi động lại ứng dụng.

Minh chứng: [ảnh](evidence/patient-birth-date-2026-10-25/form.png), [functional](evidence/patient-birth-date-2026-10-25/functional.txt), [upgrade](evidence/patient-birth-date-2026-10-25/upgrade.txt).

## File

- `templates/electronic/form.html`, `static/js/record-form-preview.js`, `.patient-birth-inputs` trong `static/css/portal.css`.
- `ElectronicRecordForm`, `ElectronicRecordController`, `ElectronicRecordService`.
- `V18__AddPatientBirthDate`, `RecordSummary`, `RecordRepository`, `RecordController` và ba template chi tiết.
- Hướng dẫn Reader và mô hình dữ liệu thống nhất đã bổ sung V18.

Khởi động lại ứng dụng, đợi migration xong rồi Ctrl+F5. Không cần SQL thủ công.
