# MRM-UC-18 — Theo dõi nhóm và thời hạn lưu

**Trạng thái:** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | NVLT; Giám đốc xem theo quyền |
| Nguồn và quy tắc | FR-14/15; D-19; BR-03/17/18 |
| Tiền điều kiện | Policy và căn cứ đã được đơn vị xác nhận trước dùng chính thức; hồ sơ có ngày đầu lưu đáng tin. |
| Dữ liệu đầu vào | Nhóm lưu 10/15/20/30 theo nguồn, policy version, ngày đầu lưu, các nhóm cùng áp dụng, lý do điều chỉnh. |

## Kích hoạt và luồng chính

1. NVLT phân loại khi nhập hoặc điều chỉnh có căn cứ.
2. Nếu nhiều nhóm, đề xuất lấy thời hạn dài nhất theo D-19; hiển thị nhóm và policy dùng tính.
3. Tính mốc dự kiến bằng cộng năm lịch; 29/2 sang năm không nhuận đề xuất ngày cuối tháng 2, chờ xác nhận.
4. Hiển thị mốc và danh sách đến mốc để theo dõi, không tạo hành động tiêu hủy.
5. Đổi ngày gốc/policy đã áp dụng phải có lý do và lịch sử trước/sau.

## Luồng thay thế và ngoại lệ

E1: Thiếu ngày đầu lưu → mốc UNKNOWN, không dùng ngày created_at.

A1: Chưa xác nhận căn cứ → cho đánh dấu phân loại tạm, không kết luận tuân thủ pháp luật.

E2: Đổi thành tử vong có lịch tương lai → xử lý lịch trước theo UC-04/09, không cho tiếp tục workflow.

## Hậu điều kiện, giao dịch và audit

Giữ dữ liệu phân loại, căn cứ, mốc và trạng thái xác minh. Không suy ra quyền tiếp cận hoặc tiêu hủy từ nhóm lưu.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-18-01:** Ngày đầu lưu khác ngày nhập → tính từ ngày đầu lưu.
- **AC-18-02:** Nhiều nhóm → kết quả theo quy tắc đã duyệt, có thể truy vết phiên bản.
- **AC-18-03:** Đến mốc lưu → không xóa hồ sơ hoặc tạo lệnh tiêu hủy.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
