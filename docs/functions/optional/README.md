# Chức năng bổ sung hoặc có điều kiện

Không triển khai trước nhóm mandatory trừ khi người có thẩm quyền chọn vào phạm vi. Email FR-11 là yêu cầu nguồn có điều kiện, cần quyết định phân kỳ rõ ràng trước nghiệm thu.

| Mã | Chức năng | Nguồn |
|---|---|---|
| [MRM-OPT-01](MRM-OPT-01_email-notifications.md) | Email kết quả và lịch đọc | FR-11; D-16 |
| [MRM-OPT-02](MRM-OPT-02_file-import.md) | Nhập metadata từ tệp có kiểm soát | FR-17; D-17 |
| [MRM-OPT-03](MRM-OPT-03_report-export.md) | Xuất Excel/PDF theo quyền | FR-20; D-18 |
| [MRM-OPT-04](MRM-OPT-04_inventory.md) | Kiểm kê kho theo đợt | FR-19 |
| [MRM-OPT-05](MRM-OPT-05_advanced-statistics.md) | Thống kê tổng hợp và phân tích xử lý | RP-08/09/12; FR-16 |
| [MRM-OPT-06](MRM-OPT-06_administrative-evidence.md) | Tệp minh chứng hành chính giới hạn | S1 chương 6/7 còn mở; nhận xét S2/S3 |

Không xếp HIS, lưu toàn văn, chữ ký số, tiêu hủy hoặc workflow cơ quan ngoài vào “optional” để âm thầm mở rộng. Các hạng mục này đang ngoài phạm vi, cần đợt phân tích/thay đổi riêng nếu có yêu cầu mới.

## Chức năng hoãn do thu hẹp vai trò

[MRM-UC-15 — Ủy quyền duyệt](MRM-UC-15_approval-delegation.md): **DEFERRED**, không thuộc phiên bản đầu bốn vai trò. Đây là mã cũ được giữ lại, không đánh lại thành OPT-07 hoặc coi đã chọn. Chỉ quay lại khi có yêu cầu mở rộng vai trò và thiết kế lại.
