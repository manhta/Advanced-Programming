# Checklist đọc và yêu cầu chỉnh sửa

Tài liệu đã soạn không đồng nghĩa nghiệp vụ đã được duyệt. Dùng checklist này để phản hồi trước code.

## Phạm vi và quyết định

- [ ] Đồng ý điểm bắt đầu nhập kho và nhóm mandatory/optional.
- [ ] Chốt D-02/03: một hay nhiều HSBA, duyệt cả yêu cầu hay từng dòng/.
- [ ] Kiểm tra khóa hồ sơ D-04 bằng dữ liệu thực tế đã được phép sử dụng.
- [ ] Chốt lịch D-05/06/14: số phiên/ngày, giới hạn ngày, giờ chốt, ngày nghỉ.
- [ ] Chốt quyền D-07/08/09 và xử lý tự duyệt/nhập thay/nhận thay.
- [x] Xác nhận bốn vai trò và hoãn ủy quyền trong bản đầu (D-24, ).
- [x] Chốt mỗi tài khoản đúng một vai trò D-25 (người dùng xác nhận ).
- [ ] Chốt xác thực cấp 2 D-10 và chính sách đăng nhập D-22 (ngưỡng vẫn đề xuất).
- [ ] Chốt sửa/hủy và biến động ngoài phần mềm D-13/15.
- [ ] Chọn email/nhập tệp/export/kiểm kê/thống kê/minh chứng có thuộc bản đầu không.
- [ ] Chốt báo cáo, căn cứ thời hạn, log, tải và RPO/RTO D-16–23.

## Đọc use case theo tuyến

- [ ] UC-01–03: tài khoản, quyền, danh mục.
- [ ] UC-04–06/18: metadata, tìm kiếm, vị trí, thời hạn.
- [ ] UC-07–11: đăng ký, kiểm tra, sửa/hủy, chỉ DIRECTOR duyệt và lịch; UC-15 hoãn.
- [ ] UC-12–14: giao–trả/ngoại lệ, đặc biệt hai NVLT đồng thời và chưa trả cuối ngày.
- [ ] UC-16–17: lịch sử/audit/báo cáo theo quyền.
- [ ] Kiểm tra không có luồng nào sửa/xóa quyết định hoặc giả nhận trả.

## Thiết kế và nghiệm thu

- [ ] Màn hình đủ trường/thao tác cho từng vai trò, thông báo dễ hiểu.
- [ ] ERD/dictionary phản ánh mô hình đã chọn, dữ liệu lịch sử thiếu có cách xử lý.
- [ ] Tiêu chí AC/TC có thể chạy bằng dữ liệu mẫu và quan sát kết quả.
- [ ] Người nghiệp vụ/CNTT xác nhận các phần họ chịu trách nhiệm.
- [ ] Cập nhật mọi tài liệu bị ảnh hưởng và mới chuyển sang giai đoạn code.

## Mẫu phản hồi

`File/mã UC hoặc D | Vấn đề | Nội dung muốn sửa | Ví dụ thực tế | Mức bắt buộc/bổ sung`

Ví dụ: `D-02, UC-07 | Một yêu cầu cần nhiều HSBA | Cho chọn tối đa 5 hồ sơ | Bác sĩ nghiên cứu cần cùng lúc 3 hồ sơ | Bắt buộc`.

Không ghi tên/phê duyệt thay người dùng. Những ô này để trống cho đến khi có phản hồi.
