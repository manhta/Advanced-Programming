# Phạm vi và đối chiếu nguồn

## Nguồn

| Mã | Tài liệu | Ý nghĩa |
|---|---|---|
| S1 | SRS_Quan_ly_luu_tru_HSBA_v0.2_da_lam_ro.docx; | Phạm vi, FR-01–20, BR, NFR, RP; dự thảo |
| S2 | Nhận xét SRS và đặc tả use case – Quản lý lưu trữ HSBA.docx; | Nhận xét và đề xuất, không phải phê duyệt |
| S3 | Danh_gia_SRS_va_Use_Case_HSBA.docx; | Đối chiếu P0/P1, đề xuất mô hình và 20 UC |

## Phương án cơ sở

Ứng dụng nội bộ quản lý metadata và luân chuyển HSBA giấy từ thời điểm NVLT nhập kho. Một yêu cầu thuộc một người đọc và một HSBA, nhiều ngày; duyệt cả phiên bản; NVLT xác nhận từng ngày. Mỗi ngày một phiên, giao và trả riêng. Tên Medical Record Management (MRM), package `mrm`.

**D-01–D-05 đang đề xuất, chưa được phê duyệt.** Nếu đổi sang nhiều HSBA/yêu cầu hoặc duyệt từng dòng, phải sửa ERD, quyết định, màn hình và kiểm thử trước code.

| Nhóm | Nội dung | Nguồn |
|---|---|---|
| Bắt buộc | Tài khoản/quyền, chỉ DIRECTOR duyệt sau xác thực cấp 2 | FR-01/02/04/10 |
| Bắt buộc | Metadata, danh mục, nhập tay/đối soát, kho/vị trí, thời hạn | FR-05/06/07/14/15/17 |
| Bắt buộc | Tra cứu, đăng ký, trình, quyết định | FR-03/08/09/10 |
| Bắt buộc | Lịch, phiên ngày, xem kết quả trong ứng dụng | FR-08/11/12; kênh trong ứng dụng là bổ sung đề xuất |
| Bắt buộc | Giao–trả, ngoại lệ, lịch sử/audit | FR-06/12/13, BR-11 nguồn |
| Bắt buộc đề xuất | Báo cáo vận hành tối thiểu và backup/restore theo quy trình | FR-16, RP ưu tiên, NFR |
| Bổ sung có điều kiện | Email, nhập tệp, xuất Excel/PDF | FR-11/17/20; cần xác nhận phân kỳ |
| Bổ sung | Kiểm kê theo đợt, thống kê nâng cao, minh chứng hành chính | FR-19, RP-08/09/12 |
| Hoãn ngoài bản đầu | Cấp/thu hồi ủy quyền; không có MANAGER/DELEGATE | CR-001, D-24 thay phạm vi FR-10 nguồn |
| Ngoài phạm vi | Nội dung/bản quét HSBA, HIS, chữ ký số, tiêu hủy | S1 chương 3 |
| Ngoài workflow | Khai thác tử vong, yêu cầu bên ngoài | FR-14/18; vẫn quản lý metadata tử vong |
| Chưa đưa bản đầu | Soát/tiếp nhận từ khoa, SLA 24h, lịch thứ Năm | BR-01–03 nguồn là bối cảnh; chờ D-01 |

## Điểm khác biệt đã đối chiếu

| Điểm | Tài liệu khác nhau | Hướng thiết kế đề xuất |
|---|---|---|
| Cấu trúc yêu cầu | S2 nhiều HSBA ×; S3 một HSBA/nhiều ngày | D-02 chọn một HSBA |
| UC-07 | S1/S3 lịch sử; S2 đổi vị trí | Dùng MRM-UC-06 vị trí; MRM-UC-16 lịch sử |
| Trạng thái | Đã trả bị trộn với hồ sơ vật lý | Đã trả thuộc phiên, HSBA trở về kho |
| Xác thực cấp 2 | Trước mở chức năng hoặc ghi quyết định | D-10 kiểm tra tại lúc ghi |
| Email | Không tích hợp nhưng email 5 phút | D-16 chốt SMTP và đo, không tự bỏ FR-11 |
| PostgreSQL | Client 18.6, server 16.5 | Thiết kế theo server hiện có 16.5, không nâng cấp |

Giám đốc xác nhận phạm vi/quyền; KHNV/NVLT xác nhận dữ liệu/quy trình; CNTT xác nhận bảo mật/hạ tầng. Người dùng rà soát trước rồi ghi người phê duyệt, ngày, phiên bản. Chưa có phản hồi không phải đã đồng ý.

## Ánh xạ use case SRS nguồn sang bộ mới

| UC trong S1/S3 | Nội dung | Mã MRM |
|---|---|---|
| UC-01 | Tra cứu | UC-05 |
| UC-02 | Đăng ký đọc | UC-07/09 |
| UC-03 | Tổng hợp/trình | UC-08 |
| UC-04 | Phê duyệt | UC-10 (UC-15 hoãn) |
| UC-05 | Giao | UC-12 |
| UC-06 | Trả | UC-13/14 |
| UC-07 | Lịch sử | UC-16 |

UC-07 đổi vị trí trong S2 được ánh xạ MRM-UC-06. Những UC bổ sung S3 được ánh xạ theo ý nghĩa/FR trong traceability, không giả định cùng số là cùng nội dung.

## Thay đổi phạm vi đã xác nhận ngày

Theo yêu cầu người dùng cho dự án học tập, chỉ bốn vai trò READER/ARCHIVIST/DIRECTOR/ADMIN. MANAGER không còn là tài khoản đăng nhập, giám sát chuyển DIRECTOR; DELEGATE và ủy quyền không triển khai trong bản đầu. KHNV có thể vẫn là bên tư vấn/xác nhận dữ liệu ngoài phần mềm theo tài liệu nguồn, không phải vai trò thứ năm. SRS gốc nhắc người được ủy quyền được giữ nguyên làm nguồn; bản thiết kế này ghi rõ thu hẹp qua CR-001, không sửa tài liệu nguồn. Mô hình mỗi tài khoản một vai trò D-25 đã xác nhận.
