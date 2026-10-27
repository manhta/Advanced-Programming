# MRM-OPT-06 — Tệp minh chứng hành chính giới hạn

**Trạng thái:** dự thảo, chưa chọn triển khai. Nguồn: S1 chương 6/7 còn mở; nhận xét S2/S3.

## Điều kiện đưa vào phạm vi

Chỉ bổ sung khi có nhu cầu được xác nhận: căn cứ cấp vai trò nhạy cảm/điều chỉnh hành chính. Không phải kho PDF/ảnh nội dung bệnh án. Chốt loại, dung lượng, thời hạn, quyền tải.

## Tác nhân và đầu vào

Người có quyền tạo căn cứ; người có quyền xem tương ứng. Dữ liệu chỉ trong phạm vi nghiệp vụ được cấp; tài khoản/session hợp lệ. Áp dụng quyền, transaction, audit và retry trong [quy tắc chung](../mandatory/COMMON_RULES.md).

## Luồng chính

1. Chọn đối tượng hành chính và loại minh chứng được phép, xác nhận không có nội dung HSBA.
2. Kiểm tra MIME thực, extension, size, tên và nội dung theo cơ chế kiểm tra đã chọn; cách ly nếu cần.
3. Lưu ngoài webroot bằng storage key, checksum và metadata, không dùng tên người dùng làm đường dẫn.
4. Tải qua kiểm tra quyền bản ghi và trường, ghi audit; xóa/lưu giữ theo chính sách có căn cứ.

## Ngoại lệ và hậu điều kiện

Loại/dung lượng sai hoặc file nghi nguy hiểm → từ chối/quarantine.

Không cho URL công khai đoán file hoặc path traversal.

Tệp chứa bản quét HSBA → không nhận trong sản phẩm này, hướng dẫn hệ thống khác.

Thành công có kết quả tham chiếu và audit; thất bại không làm sai nghiệp vụ lõi. Không cấp thêm quyền ngoài ma trận.

## Tiêu chí chấp nhận đề xuất

- **AC-OPT-06-01:** Không có quyền đối tượng → không tải được file dù biết ID.
- **AC-OPT-06-02:** Tệp giả extension được phát hiện theo kiểm tra định dạng thực.
- **AC-OPT-06-03:** Mất file storage → hiển thị lỗi có correlation, không coi minh chứng còn nguyên.
