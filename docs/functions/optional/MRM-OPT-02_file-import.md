# MRM-OPT-02 — Nhập metadata từ tệp có kiểm soát

**Trạng thái:** dự thảo, chưa chọn triển khai. Nguồn: FR-17; D-17.

## Điều kiện đưa vào phạm vi

Chọn định dạng CSV UTF-8 hoặc XLSX, mẫu cột/size/số dòng, cơ chế nhập lại và người đối soát. Không tích hợp HIS; không upload nội dung HSBA.

## Tác nhân và đầu vào

NVLT nhập; người đối soát được chỉ định xác nhận. Dữ liệu chỉ trong phạm vi nghiệp vụ được cấp; tài khoản/session hợp lệ. Áp dụng quyền, transaction, audit và retry trong [quy tắc chung](../mandatory/COMMON_RULES.md).

## Luồng chính

1. Tải mẫu với mã danh mục và hướng dẫn, chọn file metadata.
2. Kiểm tra loại thực/size và đọc vào staging, không ghi bản chính ngay.
3. Chuẩn hóa từng dòng, kiểm tra khóa/danh mục/ngày/duplicate trong tệp và DB; hiển thị tổng hợp cùng lỗi theo dòng.
4. Đề xuất mỗi lô commit toàn bộ các dòng được chọn hợp lệ sau xác nhận; dòng lỗi không tự bỏ âm thầm.
5. Lưu batch hash, người xác nhận, kết quả và ánh xạ record; đối soát vật lý trước PUBLISHED.

## Ngoại lệ và hậu điều kiện

Retry lô đã commit → trả kết quả cũ; cùng file không đồng nghĩa được phép cập nhật dữ liệu hiện hữu.

Bản ghi trùng không tự ghi đè; chuyển đối soát.

Lỗi giữa commit → rollback tập đã chọn, lô giữ lỗi rõ ràng, không báo thành công một phần khi chưa chọn chính sách.

Thành công có kết quả tham chiếu và audit; thất bại không làm sai nghiệp vụ lõi. Không cấp thêm quyền ngoài ma trận.

## Tiêu chí chấp nhận đề xuất

- **AC-OPT-02-01:** Tệp có 10 dòng, 2 lỗi → preview chỉ rõ 2 dòng; chưa có record mới trước xác nhận.
- **AC-OPT-02-02:** Ngày lịch sử thiếu không được điền ngày hiện tại.
- **AC-OPT-02-03:** Lô nhập lại không tạo hồ sơ trùng hoặc lịch sử mượn/trả giả.
