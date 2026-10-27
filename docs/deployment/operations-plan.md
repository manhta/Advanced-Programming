# Kế hoạch vận hành, kiểm thử và bàn giao — không thực thi trong đợt tài liệu

## Điều kiện trước giai đoạn 6

Chốt các P0 và scope; xác nhận metadata không nội dung, người vận hành, dữ liệu thử, môi trường LAN và database server 16.5. Tài liệu hiện không tự nâng cấp PostgreSQL hoặc cài dependency mới. Bước viết migration/kết nối và code chỉ bắt đầu khi người dùng chuyển sang triển khai.

## Danh sách chuẩn bị theo các giai đoạn còn lại

| Giai đoạn | Chuẩn bị tài liệu ngay bây giờ | Bằng chứng chỉ có sau triển khai |
|---|---|---|
| 6 | Kiến trúc, secrets, profile, migration, layout, dữ liệu giả | Build/kết nối/migration/CI thực chạy |
| 7–8 | UC-01–06/18, quyền và mẫu màn hình | Đăng nhập, metadata/kho chạy đúng |
| 9–11 | UC-07–15, trạng thái, giao dịch và ca đồng thời | Luồng xuyên suốt và ràng buộc thực chạy |
| 12 | UC-16/17 và optional được duyệt | Báo cáo/nhập/export đúng dữ liệu |
| 13 | Bộ TC, ma trận truy vết, UAT roles | Kết quả test và xác nhận người dùng |
| 14 | Runbook bên dưới | Restore, triển khai, giám sát thực tế |
| 15 | Danh mục bàn giao, thay đổi | Biên bản, tag/release, đào tạo |

## Sao lưu và khôi phục là bắt buộc về vận hành

Không nhất thiết làm màn hình trên website. CNTT chịu trách nhiệm lịch chạy, tài khoản riêng, nơi lưu và kiểm tra lỗi. Đề xuất RPO 24h/RTO 4h chỉ được dùng khi đơn vị chấp nhận đối soát các lượt giao–trả có thể mất; nếu không, chọn backup thường xuyên hơn hoặc phục hồi theo thời điểm và thiết kế lại quy trình.

1. Lập lịch sao lưu CSDL, cấu hình cần thiết, phiên bản ứng dụng/migration; file minh chứng chỉ khi được chọn.
2. Kiểm tra kết quả/size/checksum, cảnh báo khi không có bản mới; nơi sao lưu tách khỏi máy chủ, giới hạn truy cập, bảo vệ dữ liệu và chính sách giữ phiên bản.
3. Diễn tập restore sang nơi tách biệt, chặn gửi email và tránh cho user vận hành vào nhầm bản sao.
4. Kiểm tra số hồ sơ, revision/decision, lượt mở, vị trí và audit; đối chiếu biên nhận giấy nếu có.
5. Đo RPO thực tế và thời gian phục hồi; ghi lệnh, phiên bản công cụ server-compatible, cấu hình, người/ngày/kết quả.
6. Phê duyệt mở lại dịch vụ sau khi NVLT đối soát kho vật lý; có bản backup không đồng nghĩa phục hồi thành công.

## Gián đoạn lúc giao–trả

- Mất phản hồi: tra mã thao tác/request trước khi lặp; retry cùng khóa. Không đánh dấu đã giao/trả khi không có bằng chứng lưu.
- Nếu phải tiếp tục phục vụ ngoài hệ thống: dùng sổ tạm đã được đơn vị duyệt với mã HSBA, người, giờ thực tế, tình trạng và người ghi.
- Sau phục hồi: NVLT + người đối soát kiểm tra DB/receipt và sổ tạm để tránh nhập đôi. Ghi event phục hồi có occurred_at thực tế và recorded_at server; không tự sửa timestamp lịch sử.
- Nếu DB sau restore còn lượt mở nhưng thực đã trả, hoặc ngược lại, giữ cờ đối soát và block giao mới đến khi xác minh; không batch cập nhật “tất cả trong kho”.

## Triển khai và rollback

Chuẩn bị máy chủ, tài khoản dịch vụ, LAN/DNS/HTTPS, firewall, secrets ngoài Git và quyền DB. Chạy migration ở cửa sổ đã thống nhất sau backup, triển khai JAR có version, smoke test login/tìm/luồng mẫu, theo dõi log và dung lượng. Rollback phải đánh giá schema tương thích; không giả định chạy JAR cũ sẽ đảo migration. Thử trên bản sao trước môi trường vận hành.

## Khởi tạo khoảng 100 hồ sơ

Chọn nguồn và người chịu trách nhiệm, nhập staging, kiểm tra trùng/ngày/vị trí, đối soát vật lý, xác nhận PUBLISHED. Không tạo lịch sử giao–trả giả. HSBA đang được giữ ngoài kho cần trạng thái/căn cứ rõ trước đưa vào sử dụng, không mặc định khả dụng.

## Bàn giao

- SRS/phạm vi/quyết định đã xác nhận; use case/quyền/trạng thái/ERD/dictionary.
- Mã nguồn, dependency lock/config mẫu, migration và cách tái tạo môi trường (chỉ sau triển khai).
- Báo cáo test/UAT, dữ liệu mẫu, giới hạn biết trước và cách xử lý lỗi.
- Hướng dẫn theo từng vai trò, runbook deploy/backup/restore/gián đoạn, người liên hệ và chuyển giao tài khoản qua kênh phù hợp.
- Biên bản đào tạo, người chịu trách nhiệm, ngày nghiệm thu, tag/release notes. Không tạo hoặc ký thay các biên bản chưa xảy ra.

Yêu cầu mới ghi issue, nguồn/lý do/người quyết định, tác động dữ liệu/quyền/giao diện/test/triển khai; cập nhật tài liệu và phiên bản trước code.
