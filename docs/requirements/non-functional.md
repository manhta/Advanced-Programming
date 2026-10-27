# Yêu cầu phi chức năng và phép đo đề xuất

Chưa có thử nghiệm hoặc cam kết chính thức. Ngưỡng trong bảng đều phải được CNTT/đơn vị xác nhận trước nghiệm thu.

| Mã | Mục tiêu đề xuất | Phép kiểm chứng |
|---|---|---|
| NFR-M01 | Quyền theo action/record/field trên mọi đường truy cập | READER đoán ID, ADMIN đọc HSBA, tài khoản mất vai trò DIRECTOR, report/export vượt scope đều bị chặn |
| NFR-M02 | Xác thực cấp 2 gắn người/revision/action một lần | Thay revision/actor, retry sau tiêu thụ, vượt hạn → không ghi quyết định mới |
| NFR-M03 | Không mất nhất quán giao–trả/duyệt | Gửi hai thao tác song song; tiêm lỗi giữa các lần ghi → một kết quả hoặc rollback |
| NFR-M04 | Tìm kiếm p95≤3 giây, 10 user đồng thời, 100 HSBA | Ghi CPU/RAM/OS/JDK/PostgreSQL, LAN/trình duyệt, kịch bản và dữ liệu; warm-up 1 phút, đo ≥5 phút; lưu p50/p95/lỗi; ngưỡng D-21 |
| NFR-M05 | Audit nghiệp vụ 10 năm | Kiểm tra trường/không bí mật, chống sửa qua UI/quyền DB, backup và khôi phục mẫu; kiểm thử ngắn không chứng minh đã lưu đủ 10 năm |
| NFR-M06 | RPO≤24h, RTO≤4h nếu chấp nhận | Restore vào nơi tách biệt, đo từ phát hiện đến sẵn sàng; liệt kê phần dữ liệu có thể mất và cách đối soát giấy |
| NFR-M07 | Trình duyệt nội bộ phổ biến, màn hình desktop/tablet | Chốt phiên bản Edge/Chrome thực tế; kiểm tra form/bảng/keyboard/nhãn/contrast/tiếng Việt |
| NFR-M08 | Mọi timestamp máy chủ nhất quán, ngày đọc Asia/Bangkok | Ca 23:59/00:00, proof cấp 2 hết hạn, chốt ngày, ngày nhuận |
| NFR-M09 | Có cách làm khi mất kết nối | Không tự coi giao/trả thành công; chứng từ tạm, kiểm tra operation receipt và đối soát sau phục hồi |
| NFR-M10 | Giới hạn dữ liệu, không nội dung bệnh án | Không upload bệnh án, audit/export không lộ dữ liệu ngoài quyền; rà soát cấu hình/log mẫu |
| NFR-M11 | Dễ dùng cho vai trò chính | Người dùng tự tìm–đăng ký–duyệt–giao–trả theo kịch bản với dữ liệu giả, ghi lỗi/nhầm và phản hồi |
| NFR-M12 | Khả năng cập nhật có kiểm soát | Tái tạo DB trống từ migration, chạy phiên bản mới trên bản sao, kiểm tra tương thích rollback |

## Sẵn sàng và hạ tầng

Chưa đặt tỷ lệ uptime khi chưa biết máy chủ/điện/mạng/người trực. Cần ghi giờ phục vụ, maintenance window, người nhận cảnh báo và thời gian phản hồi. Không suy ra 100 HSBA tương đương tải production. Dung lượng audit tăng 10 năm cần ước lượng số sự kiện/ngày × kích thước × thời gian + index/backup.

## Căn cứ nghiệp vụ và dữ liệu cá nhân

Đơn vị cung cấp danh mục văn bản/quy chế áp dụng, quyền truy cập, thời hạn, đối tượng và người phê duyệt. Bộ tài liệu không kết luận các nhóm lưu trong SRS tự động phù hợp pháp luật; chỉ phản ánh yêu cầu nguồn cần xác nhận. Hồ sơ thật chỉ sử dụng ở môi trường được phép, phát triển dùng dữ liệu giả.
