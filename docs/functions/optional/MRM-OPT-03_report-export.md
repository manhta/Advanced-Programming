# MRM-OPT-03 — Xuất Excel/PDF theo quyền

**Trạng thái:** dự thảo, chưa chọn triển khai. Nguồn: FR-20; D-18.

## Điều kiện đưa vào phạm vi

Chốt loại báo cáo, định dạng, mẫu, số dòng tối đa và quyền EXPORT riêng; không mặc định có quyền xem là được xuất.

## Tác nhân và đầu vào

NVLT/Giám đốc có EXPORT theo scope. Dữ liệu chỉ trong phạm vi nghiệp vụ được cấp; tài khoản/session hợp lệ. Áp dụng quyền, transaction, audit và retry trong [quy tắc chung](../mandatory/COMMON_RULES.md).

## Luồng chính

1. Chọn báo cáo và bộ lọc, hệ thống kiểm tra cả quyền báo cáo và xuất.
2. Tạo snapshot kết quả tại thời điểm xuất trong phạm vi bản ghi/trường.
3. Tạo file có tên báo cáo, ngày chốt, bộ lọc và đơn vị đếm; tên file tiếng Anh.
4. Cho tải qua đường dẫn xác thực, thời hạn ngắn; ghi actor/report/filter scope/time/count/checksum theo chính sách.

## Ngoại lệ và hậu điều kiện

Vượt giới hạn → yêu cầu thu hẹp hoặc tạo tác vụ có quyền kiểm tra lại khi tải.

Tên/ghi chú bắt đầu =,+,-,@ trong Excel → xử lý như text, tránh formula injection.

Thu hồi quyền trước khi tải → không cấp file cũ vượt quyền; file tạm có chính sách dọn.

Thành công có kết quả tham chiếu và audit; thất bại không làm sai nghiệp vụ lõi. Không cấp thêm quyền ngoài ma trận.

## Tiêu chí chấp nhận đề xuất

- **AC-OPT-03-01:** Người không EXPORT gọi URL trực tiếp → chặn.
- **AC-OPT-03-02:** Tổng/dòng/cột đúng bộ lọc và quyền giống báo cáo màn hình.
- **AC-OPT-03-03:** Tải PDF/Excel không chứa vị trí khi người dùng không có LOCATION_VIEW.
