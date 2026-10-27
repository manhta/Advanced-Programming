# MRM-UC-19 — Sao lưu, khôi phục và đối soát sau sự cố

**Trạng thái:** dự thảo v0.1, bắt buộc về vận hành; có thể thực hiện bằng công cụ quản trị, không bắt buộc viết màn hình website.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | CNTT/người vận hành được chỉ định; NVLT đối soát; Giám đốc/người phụ trách cho phép mở lại |
| Nguồn | NFR sao lưu/khôi phục S1; UC-20 của S3; D-21; BR-11/13/22 |
| Tiền điều kiện | Có chính sách RPO/RTO được duyệt, tài khoản backup giới hạn, nơi lưu tách biệt và môi trường phục hồi thử |
| Đầu vào | Phiên bản ứng dụng/schema/PostgreSQL, bản backup và checksum, thời điểm cần phục hồi, căn cứ cho phép và sổ tạm |

## Luồng chính A — Sao lưu

1. Người vận hành cấu hình lịch theo RPO được chấp nhận, phạm vi DB và file bổ sung nếu có; giữ secrets ngoài repository.
2. Đến lịch, công cụ tạo bản sao, ghi thời gian bắt đầu/kết thúc và kết quả; không khóa lâu ảnh hưởng giao–trả ngoài khung cho phép.
3. Kiểm tra file, checksum, bản ghi manifest/schema version và quyền đọc; lưu bản sao ở nơi tách biệt.
4. Cảnh báo nếu thất bại hoặc không có bản mới; lưu lịch sử lần chạy. Không coi file rỗng hoặc chỉ “job đã chạy” là thành công.

## Luồng chính B — Khôi phục và mở lại

1. Xác nhận sự cố/phạm vi; hạn chế ghi mới và ghi chứng từ tạm nếu phải phục vụ. Chọn bản sao theo thời điểm và RPO, giữ nguyên bản phục vụ điều tra.
2. Khôi phục vào môi trường tách biệt bằng công cụ/phiên bản tương thích; chặn email và đường truy cập vận hành nhầm.
3. Kiểm tra schema, số hồ sơ, decision/revision, handover mở, physical/current location và audit; so với manifest.
4. NVLT đối chiếu DB với HSBA thực tế và sổ tạm, tìm giao/trả phát sinh sau thời điểm backup. Đánh dấu block những hồ sơ chưa xác minh.
5. Ghi các điều chỉnh có căn cứ và người xác nhận, phân biệt thời gian xảy ra thực với thời gian ghi sau phục hồi; không tạo lịch sử giả hoặc nhận trả hàng loạt.
6. Đo thời gian phục hồi/RPO thực tế, ghi phần dữ liệu mất và kết quả đối soát; người có trách nhiệm xác nhận mở lại, theo dõi sau mở.

## Ngoại lệ

- E1: Backup lỗi/checksum sai → không đưa vào danh sách bản tin cậy; cảnh báo và tạo lại, không xóa bản tốt gần nhất.
- E2: Restore không khớp phiên bản/schema → dừng và thử môi trường tương thích; không thử nghiệm trực tiếp trên DB duy nhất.
- E3: RPO/RTO vượt mục tiêu → báo người phụ trách và quyết định cách phục hồi, ghi sự cố; không đánh dấu đạt.
- E4: Không đối soát được lượt mở với vật lý → tiếp tục block hồ sơ liên quan; không mở lại giao bình thường cho hồ sơ đó.
- E5: Rollback JAR không tương thích migration → không tự chạy bản cũ; xử lý theo phương án đã thử trên bản sao.

## Hậu điều kiện và bảo toàn

Có bản sao tin cậy và lịch sử lần chạy, hoặc môi trường phục hồi đã kiểm chứng và được xác nhận; dữ liệu gốc không bị thử nghiệm ghi đè. Việc đọc backup cũng là quyền tiếp cận dữ liệu nhạy cảm, phải giới hạn và ghi dấu truy cập.

## Tiêu chí nghiệm thu đề xuất

- **AC-19-01:** Backup tạo được và restore vào môi trường tách biệt → schema, số liệu, lượt mở và audit đối chiếu đúng, có biên bản đo.
- **AC-19-02:** Mất giao dịch sau bản backup → nêu rõ khoảng mất, đối soát sổ tạm/vật lý, không tự đánh dấu mọi hồ sơ trong kho.
- **AC-19-03:** Một hồ sơ chưa xác minh sau restore → vẫn bị chặn giao dù dịch vụ đã mở lại cho hồ sơ khác.

## Điểm cần rà soát

D-21, lịch sao lưu, nơi giữ, thời gian lưu phiên bản backup, ai cho phép mở lại và ngưỡng chấp nhận mất dữ liệu. Đây là kế hoạch, chưa có bản sao/diễn tập mới trong đợt tài liệu.
