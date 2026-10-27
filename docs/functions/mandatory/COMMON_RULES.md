# Quy ước chung của use case bắt buộc

Tất cả đặc tả là bản 0.1 chờ rà soát. “Bắt buộc” chỉ mức ưu tiên của phiên bản cơ sở đề xuất; áp dụng [sổ quyết định](../../requirements/decision-log.md) và [quy tắc](../../requirements/business-rules.md).

## Tiền điều kiện chung

Trừ đăng nhập/đặt lại mật khẩu, tài khoản hoạt động, session còn hiệu lực. Máy chủ kiểm tra quyền thao tác, phạm vi bản ghi và trường dữ liệu tại cả lúc hiển thị lẫn lúc ghi. Không coi việc thấy nút là bằng chứng có quyền. Biểu mẫu có token chống giả mạo; ID, trạng thái và thời gian từ trình duyệt không được tin mặc định.

## Quy tắc lưu và lỗi

- Validation thất bại: giữ dữ liệu nhập không bí mật, chỉ rõ trường; không cập nhật nghiệp vụ.
- Không quyền: không trả dữ liệu nhạy cảm hay xác nhận sự tồn tại của bản ghi ngoài phạm vi.
- Hai người sửa cùng phiên bản: người lưu sau nhận xung đột, phải tải lại; không ghi đè lặng lẽ.
- Transaction: lưu thay đổi, lịch sử và sự kiện audit bắt buộc trong cùng đơn vị nhất quán; lỗi bước sau không để trạng thái dở dang. Audit thất bại của thao tác bị chặn dùng kênh riêng và correlation, không ghi mật khẩu/token.
- Idempotency: tạo yêu cầu/giao/trả/quyết định dùng khóa thao tác gắn tài khoản, loại hành động và nội dung. Gửi lặp cùng nội dung trả kết quả cũ; khác nội dung báo xung đột. Thiết kế giữ receipt suốt vòng đời giao dịch liên quan; không chỉ khóa nút trên trình duyệt.
- Lỗi mạng sau ghi: trạng thái có thể đã lưu. Hiển thị hướng dẫn tra cứu mã thao tác/tải lại; retry có cùng khóa. Không báo “chưa giao” chỉ vì chưa nhận phản hồi.
- Quyết định và giao–trả không sửa/xóa trực tiếp. Sai sót lập điều chỉnh có căn cứ và quyền, giữ bản gốc.
- Timestamp do máy chủ, ngày nghiệp vụ Asia/Bangkok; định dạng giao diện dd/MM/yyyy nhưng dữ liệu ngày tách thời điểm.

## Audit tối thiểu

actor, action, target type/id, server timestamp, outcome, correlation/operation key, request revision nếu liên quan; before/after chỉ trường cần thiết, quyền xem theo chính sách. Tra cứu chi tiết/log có quyền riêng. Lịch sử nghiệp vụ và log kỹ thuật là hai loại khác nhau.

## Mẫu tiêu chí

Các dòng AC trong mỗi file là tiêu chí nghiệm thu đề xuất, chưa chạy. Mã TC tương ứng AC được ánh xạ trong tài liệu kiểm thử. Hoàn thành tài liệu không đồng nghĩa chức năng đã được triển khai.
