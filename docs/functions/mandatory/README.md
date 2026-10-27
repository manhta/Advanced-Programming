# Chức năng bắt buộc — phiên bản cơ sở đề xuất

Đọc [quy tắc chung](COMMON_RULES.md) cùng từng use case. Mã mới tránh xung đột UC nguồn. D-xx còn chờ rà soát; tiêu chí AC chưa chạy.

| Mã | Chức năng | Tác nhân |
|---|---|---|
| [MRM-UC-01](MRM-UC-01_authentication.md) | Đăng nhập, đăng xuất và phiên làm việc | Người dùng nội bộ |
| [MRM-UC-02](MRM-UC-02_account-and-role-management.md) | Quản lý tài khoản, vai trò và cấp lại thông tin xác thực | ADMIN kỹ thuật; Giám đốc xác nhận quyền nhạy cảm |
| [MRM-UC-03](MRM-UC-03_reference-data.md) | Quản lý danh mục khoa, loại hồ sơ và chính sách lưu | NVLT; Giám đốc xác nhận danh mục nghiệp vụ |
| [MRM-UC-04](MRM-UC-04_record-entry-and-initial-data.md) | Nhập kho, cập nhật metadata và đối soát dữ liệu ban đầu | NVLT; người đối soát được Giám đốc chỉ định |
| [MRM-UC-05](MRM-UC-05_record-search.md) | Tra cứu và xem chi tiết HSBA | READER, NVLT, Giám đốc theo quyền |
| [MRM-UC-06](MRM-UC-06_storage-and-location-history.md) | Quản lý vị trí và chuyển hồ sơ | NVLT |
| [MRM-UC-07](MRM-UC-07_submit-reading-request.md) | Tạo và gửi yêu cầu đọc | READER |
| [MRM-UC-08](MRM-UC-08_review-and-submit-for-approval.md) | NVLT kiểm tra, trả bổ sung và trình duyệt | NVLT |
| [MRM-UC-09](MRM-UC-09_amend-cancel-and-track.md) | Theo dõi, bổ sung, sửa và hủy yêu cầu/phiên | Chủ yêu cầu; NVLT hủy phiên theo quyền |
| [MRM-UC-10](MRM-UC-10_approval-and-step-up.md) | Phê duyệt/từ chối với xác thực cấp 2 | DIRECTOR đang hoạt động |
| [MRM-UC-11](MRM-UC-11_confirm-reading-schedule.md) | Xác nhận lịch và xem kết quả trong ứng dụng | NVLT; chủ yêu cầu xem kết quả |
| [MRM-UC-12](MRM-UC-12_handover-at-reading-room.md) | Giao HSBA tại phòng đọc | NVLT; người đọc nhận vật lý |
| [MRM-UC-13](MRM-UC-13_return-to-storage.md) | Nhận trả, cất lại và ghi chênh lệch | NVLT |
| [MRM-UC-14](MRM-UC-14_exceptions-and-reconciliation.md) | Đối soát chưa trả, không đến và bất thường | NVLT; Giám đốc giám sát |
| [MRM-UC-16](MRM-UC-16_history-and-audit.md) | Tra cứu lịch sử và nhật ký kiểm toán | NVLT/Giám đốc theo quyền audit riêng |
| [MRM-UC-17](MRM-UC-17_operational-reports.md) | Báo cáo vận hành tối thiểu | NVLT; Giám đốc theo phạm vi |
| [MRM-UC-18](MRM-UC-18_retention-and-record-classification.md) | Theo dõi nhóm và thời hạn lưu | NVLT; Giám đốc xem theo quyền |
| [MRM-UC-19](MRM-UC-19_backup-and-recovery.md) | Sao lưu, khôi phục và đối soát | CNTT/NVLT/người phụ trách |

## Thứ tự triển khai sau khi duyệt thiết kế

01–03 (nền tảng) → 04–06,18 (HSBA/kho) → 07–11 (yêu cầu/duyệt/lịch) → 12–14 (giao–trả/ngoại lệ) → 16–17 (tra cứu lịch sử/báo cáo). Audit phải tích hợp từ chức năng đầu tiên, không đợi tới UC-16. Backup/restore là yêu cầu vận hành bắt buộc trong tài liệu operations-plan, không cần dựng màn hình quản trị riêng.
