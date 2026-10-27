# MRM-OPT-05 — Thống kê tổng hợp và phân tích xử lý

**Trạng thái:** dự thảo, chưa chọn triển khai. Nguồn: RP-08/09/12; FR-16.

## Điều kiện đưa vào phạm vi

Chốt chỉ số, công thức, khoảng ngày, timezone, scope và giá trị sử dụng; dữ liệu lịch sử đủ tin cậy.

## Tác nhân và đầu vào

Giám đốc theo quyền. Dữ liệu chỉ trong phạm vi nghiệp vụ được cấp; tài khoản/session hợp lệ. Áp dụng quyền, transaction, audit và retry trong [quy tắc chung](../mandatory/COMMON_RULES.md).

## Luồng chính

1. Chọn kỳ và khoa, hiển thị định nghĩa mỗi chỉ số.
2. Tính số HSBA distinct, request, session riêng; nhóm theo loại/khoa/năm/nhóm lưu.
3. Thời gian xử lý đề xuất submitted_at của revision được quyết định đến decided_at; request chưa quyết định không vào trung bình, trình bày riêng số chờ.
4. So sánh kỳ chỉ dùng dữ liệu đã đối soát, hiển thị missing/unknown và thời điểm cập nhật.

## Ngoại lệ và hậu điều kiện

Đăng ký lại nhiều revision không tự tính thành nhiều yêu cầu.

Hủy/từ chối/đang chờ tính vào chỉ số nào phải nêu rõ.

Không lấy trạng thái hiện tại để suy ra tồn kho tại ngày quá khứ khi chưa có mô hình sự kiện đầy đủ.

Thành công có kết quả tham chiếu và audit; thất bại không làm sai nghiệp vụ lõi. Không cấp thêm quyền ngoài ma trận.

## Tiêu chí chấp nhận đề xuất

- **AC-OPT-05-01:** Mẫu ba phiên một request → chỉ số không trộn đơn vị.
- **AC-OPT-05-02:** Khoảng không dữ liệu → 0 hoặc N/A phù hợp, không chia cho 0.
- **AC-OPT-05-03:** Hai kỳ khác số ngày làm việc → nêu định nghĩa so sánh, không suy luận sai.
