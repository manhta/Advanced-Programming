# MRM-UC-17 — Báo cáo vận hành tối thiểu

**Trạng thái:** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | NVLT; Giám đốc theo phạm vi |
| Nguồn và quy tắc | FR-16; RP-01/02/03/04/05/06/07/10/11; D-18; BR-20 |
| Tiền điều kiện | Danh mục báo cáo và phạm vi quyền được chốt; dữ liệu vận hành đã có. |
| Dữ liệu đầu vào | Loại báo cáo, khoa/trạng thái/HSBA/người/kho theo quyền, ngày nghiệp vụ hoặc thời điểm chốt phù hợp báo cáo. |

## Kích hoạt và luồng chính

1. Chọn báo cáo và bộ lọc; giao diện nêu đơn vị đếm, thời điểm dữ liệu và ý nghĩa trạng thái.
2. Máy chủ kiểm tra quyền báo cáo cùng quyền bản ghi/trường; không dùng quyền màn hình để suy ra quyền xuất.
3. Tổng hợp danh mục/vị trí hiện tại, hàng đợi, sổ giao–trả, phiên chưa trả hoặc lịch sử.
4. Hiển thị tổng, chi tiết phân trang và đường dẫn đối chiếu theo quyền.
5. Đối soát kết quả với các ca mẫu; nếu xuất tệp chưa được chọn thì chỉ xem trên màn hình.

## Luồng thay thế và ngoại lệ

A1: Không có số liệu → tổng 0/danh sách rỗng, không lỗi.

E1: RP-02 hiện tại không hỗ trợ chụp lại quá khứ → từ chối bộ lọc “tại ngày cũ” thay vì dùng current state giả.

E2: Phiên đang mở từ ngày trước → RP-11 vẫn liệt kê, không lọc mất vì không thuộc hôm nay.

E3: Bộ lọc có trường kho cho bác sĩ → loại hoặc từ chối đúng chính sách, không trả số tổng vượt phạm vi.

## Hậu điều kiện, giao dịch và audit

Báo cáo không thay nghiệp vụ, có thời điểm truy vấn. RP-04 ngày giao có các lượt trong kỳ và trường trả; RP-11 mọi lượt chưa đóng đến thời điểm báo cáo. RP-05/07/10 dùng thời điểm sự kiện.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-17-01:** Một HSBA, một request, ba phiên → ba phiên, một request, một HSBA distinct.
- **AC-17-02:** Có lượt giao hôm qua chưa trả → RP-11 hôm nay vẫn có.
- **AC-17-03:** Bộ lọc theo khoa → cả tổng và dòng chi tiết cùng scope.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.

## Định nghĩa từng báo cáo cơ sở đề xuất

| RP nguồn | Cột/chỉ số chính | Mốc và bộ lọc | Quyền |
|---|---|---|---|
| RP-01 | Mã HSBA, BASIC, vị trí nếu được cấp, nhóm lưu, trạng thái | Hiện tại; khoa, mã/tên, thời gian điều trị, nhóm, kho theo quyền | NVLT/Giám đốc có REPORT_RECORDS; vị trí cần LOCATION_VIEW |
| RP-02 | Số HSBA theo physical state và block, không coi đã trả là physical state | Thời điểm truy vấn, khoa/kho/loại | REPORT_RECORDS; không tự hỗ trợ tồn tại ngày cũ |
| RP-03 | Request code, owner/khoa, HSBA, revision, purpose, ngày, decision, tiến độ | Ngày gửi request, state, owner/khoa | REPORT_REQUESTS, scope phù hợp |
| RP-04 | Mỗi handover một dòng, ngày phiên, người giao/đọc, giờ giao/trả, tình trạng | Lọc theo ngày giao hoặc ngày phiên được chọn rõ, không trộn hai cách | REPORT_CIRCULATION |
| RP-05 | Sự kiện khai thác theo HSBA/người, revision/decision/session, actor/time/outcome | Thời gian sự kiện, object, actor | HISTORY_VIEW trong scope |
| RP-06 | SUBMITTED, NEEDS_INFORMATION, PENDING_APPROVAL; APPROVED còn PLANNED và ngày chưa qua | State, khoa, owner, thời gian chờ; ghi mốc chờ theo lần vào hàng đợi hiện hành | REPORT_REQUESTS |
| RP-07 | HSBA, từ/đến và snapshot đường dẫn, actor/time/reason | Ngày sự kiện, HSBA, kho | LOCATION_HISTORY_VIEW, LOCATION_VIEW |
| RP-10 | Actor/time/action/object/outcome/correlation, before/after phù hợp | Ngày sự kiện, actor, action, object | AUDIT_VIEW riêng |
| RP-11 | Mọi lượt chưa trả đến thời điểm báo cáo, ngày giao/đọc, người, giờ, ngoại lệ | Có cả ngày trước; không loại vì request đã đổi tiến độ | REPORT_CIRCULATION |

Tên permission trong bảng là mã thiết kế, cần đồng bộ catalog trước code. Tất cả báo cáo chỉ xem, không có nút thay trạng thái trực tiếp. Các cột purpose/lý do cần scope phù hợp, không dùng báo cáo để vượt quyền màn hình.
