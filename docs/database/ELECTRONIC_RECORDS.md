# Hồ sơ điện tử bác sĩ khai báo — V15

**Đã được thay thế bởi V17:** bệnh án hiện lưu một lần trong medical_records, hai cờ thể hiện hình thức; electronic_records chỉ còn view tương thích chỉ đọc. Dùng [UNIFIED_MEDICAL_RECORDS.md](UNIFIED_MEDICAL_RECORDS.md) làm hướng dẫn hiện hành. Các mô tả bảng riêng dưới đây là lịch sử V15/V16.

Cập nhật V16,: electronic_records thêm department_id/record_type_id (FK), snapshot tên khoa/loại, deceased (mặc định false), admission_date/discharge_date, source_note (1000) và note (, tùy chọn). Dữ liệu V15 giữ nguyên; các trường mới null trừ deceased=false. Mỗi event mới tự chụp các trường bổ sung trong JSON snapshot. Kiểm tra ngày và danh mục ở backend; CHECK database chặn ra viện trước vào viện. Không thêm thuộc tính lưu trữ giấy vào nhánh điện tử.

Bổ sung. Quyết định người dùng: bản điện tử công bố ngay khi Giám đốc duyệt, bỏ qua bản giấy trong luồng này. Archivist vẫn có luồng nhập giấy hiện hữu. Không chuyển/mã hóa lại các hồ sơ cũ.

## Mã và phạm vi dữ liệu

`MRM-REC-000001`: MRM là ứng dụng, REC là record, năm cấp mã theo Asia/Bangkok, số thứ tự ít nhất 6 chữ số trong năm. UUID làm khóa kỹ thuật, record_code là mã người dùng thấy. Không dùng tên, khoa hoặc căn cước để tạo mã. PostgreSQL dùng bộ đếm theo năm với UPSERT nguyên tử, UNIQUE; gửi lại cùng token/nội dung trả về cùng hồ sơ. Không tạo mã chỉ vì mở form. Không cho sửa mã sau tạo; không có thao tác xóa/tái sử dụng mã.

Mã người bệnh patient_code tùy chọn, tách khỏi mã bệnh án. Không tự gộp người bệnh trùng tên hoặc tự coi mọi lần nhập cùng mã người bệnh là cùng đợt bệnh án. Form mới lưu họ tên, năm sinh, giới tính, mã người bệnh; chưa phải nội dung bệnh án điện tử lâm sàng đầy đủ. Không sinh số lưu trữ giấy, vị trí, ngày đầu lưu hoặc thời hạn bảo quản kho cho nhánh này.

## Bảng mới

| Bảng | Vai trò |
|---|---|
| electronic_record_counters | Bộ đếm mã theo năm |
| electronic_records | Thông tin bệnh nhân, mã tự động, trạng thái, người khai báo/kiểm tra/duyệt, ngày công bố và version |
| electronic_record_events | Lịch sử từng thao tác, người/thời điểm/version và JSON snapshot đầy đủ tại thời điểm thao tác |

Không thêm dòng vào medical_records (dữ liệu hồ sơ giấy cũ) khi tạo/công bố hồ sơ điện tử. Từ menu/Tra cứu giấy có đường dẫn sang danh sách Hồ sơ điện tử. Nhánh giấy tiếp tục có medical_record_number, archive_number, vị trí, chính sách như trước. Tra cứu/mượn hồ sơ giấy không trộn các hồ sơ điện tử không có bản giấy.

notifications thêm electronic_record_id/electronic_event_id. CHECK bắt buộc thông báo thuộc đúng một nhánh: yêu cầu mượn cũ hoặc hồ sơ điện tử. FK cặp bảo đảm sự kiện thuộc đúng hồ sơ điện tử; UNIQUE ngăn gửi trùng cùng sự kiện/người nhận. Chuông chưa đọc/đã đọc dùng chung, mở thông báo dựng URL phía server theo nhánh và vai trò đang có. Không gửi email.

## Trạng thái và quyền

SUBMITTED (Mới gửi) → NEEDS_INFO (Cần bổ sung) → bác sĩ cập nhật trở lại SUBMITTED; hoặc SUBMITTED → PENDING_APPROVAL (Chờ duyệt) → APPROVED (Đã công bố)/REJECTED (Từ chối). Bác sĩ được hủy trước quyết định thành CANCELLED (Đã hủy). Khi sửa gửi lại, xóa kết quả kiểm tra cũ ở bản hiện hành nhưng giữ trong event snapshot.

Reader chỉ tạo và xem hồ sơ mình khai báo; sửa khi SUBMITTED/NEEDS_INFO. Archivist xem, kiểm tra và trình, không công bố hồ sơ điện tử. Director xem, quyết định sau kiểm tra, phải xác thực cấp 2. Admin không có quyền lâm sàng qua chức năng mới. Hồ sơ giấy của vai trò khác không bị mở rộng quyền. Không phát sinh quyền đọc hồ sơ điện tử của bác sĩ khác chỉ vì đã được công bố; công bố thể hiện bản được duyệt trong hệ thống cho chủ khai báo và người có trách nhiệm kiểm tra/duyệt.

Mỗi thao tác khóa dòng FOR UPDATE, kiểm tra version/trạng thái thật; không tin state/code/published_at được tự gửi từ trình duyệt. Phê duyệt cập nhật trạng thái, người/thời gian duyệt, published_at, event/audit và thông báo trong một transaction. Từ chối hoặc sai mật khẩu không công bố. Thất bại mật khẩu ghi audit riêng để không mất khi rollback. Giới hạn 5 lần sai/15 phút dùng chung action APPROVAL_VERIFY với chức năng duyệt yêu cầu đọc.

## Cài đặt

Migration Java mới: `V15__CreateElectronicRecords.java`. Không sửa checksum V1–V14. Khi khởi động ứng dụng với dev, Flyway áp dụng V15 vào database được cấu hình. Cần sao lưu database theo quy trình đang dùng trước triển khai. Không nhập bệnh án thật để thử tính năng mới.

Đường dẫn mã: `src/main/java/mrm/electronic/`; giao diện: `src/main/resources/templates/electronic/`. Đây là chức năng bổ sung bên cạnh luồng kho giấy; không chuyển các bệnh án cũ vào bảng mới tự động.
