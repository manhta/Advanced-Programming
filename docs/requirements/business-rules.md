# Quy tắc nghiệp vụ MRM

BR bên dưới là mã nội bộ mới, không thay mã BR của SRS. Chi tiết phát triển thêm phụ thuộc D-xx còn đề xuất.

| Mã | Quy tắc có thể kiểm chứng | Nguồn |
|---|---|---|
| BR-01 | Kiểm tra quyền hành động, bản ghi và trường tại máy chủ mỗi lần đọc/ghi; mặc định từ chối | FR-04, D-07/08 |
| BR-02 | Một bệnh nhân nhiều HSBA; ID bất biến; không ghép theo tên/mã bệnh nhân | D-04 |
| BR-03 | Chỉ metadata, không nội dung; chặn workflow đọc tử vong/bên ngoài | S1 chương 3 |
| BR-04 | Cây vị trí không chu trình, mã duy nhất trong cha; ngừng dùng không gán mới | FR-05 |
| BR-05 | Một người/một HSBA/nhiều ngày duy nhất, ngày hợp lệ theo D-06 | FR-08, D-02 |
| BR-06 | Quyết định gắn revision bất biến; không sửa quyết định đã lưu | FR-10, D-03/13 |
| BR-07 | Vai trò DIRECTOR/tài khoản/xác thực hợp lệ lúc ghi, không tự duyệt | D-09/10/24 |
| BR-08 | Chỉ xác nhận ngày trong revision được duyệt; không đổi hồ sơ/người/ngày ngoài phạm vi qua màn hình lịch | FR-11 |
| BR-09 | Một HSBA/ngày tối đa một CONFIRMED/READING/RETURNED; đăng ký chưa xác nhận chưa giữ chỗ | D-05 |
| BR-10 | Một HSBA tối đa một lượt giao chưa trả; cần IN_STORAGE, không bị chặn, phiên CONFIRMED đúng ngày | FR-06/12 |
| BR-11 | Giao/trả cập nhật lượt, phiên, HSBA và audit nguyên tử hoặc rollback toàn bộ | NFR-08 |
| BR-12 | Retry cùng khóa/action/nội dung trả cùng kết quả; cùng khóa khác nội dung bị chặn | S3, thiết kế |
| BR-13 | Từng ngày có phiên/giao/trả riêng; quá ngày vẫn READING/AT_READING_ROOM và mở ngoại lệ | BR-08/11 nguồn |
| BR-14 | Không hủy READING/RETURNED; đã thực hiện một phần chỉ hủy phần chưa giao | D-13 |
| BR-15 | Trả muộn ghi giờ thực tế; sai sót sửa bằng bản điều chỉnh có căn cứ, không ghi đè | FR-12 |
| BR-16 | Chuyển vị trí thường chỉ IN_STORAGE, không lượt mở; nhận trả có thể chọn vị trí mới, có lịch sử | FR-05/12 |
| BR-17 | Ngày đầu lưu khác ngày nhập phần mềm; không tạo ngày/lịch sử giả | FR-07/17 |
| BR-18 | Thời hạn theo policy và ngày đầu lưu; không tạo lệnh tiêu hủy | FR-15, D-19 |
| BR-19 | Không xóa cứng đối tượng đã tham chiếu; vô hiệu hóa và giữ lịch sử | Thiết kế |
| BR-20 | Báo cáo phân biệt distinct HSBA/request/session; quyền xuất độc lập | FR-16/20 |
| BR-21 | Lỗi gửi thông báo không đảo lịch/quyết định; nội dung không lộ vị trí/toàn văn | FR-11 |
| BR-22 | Audit có người, giờ server, action, object, outcome, correlation; không password/token | FR-13 |
| BR-23 | Tạm không khả dụng chặn giao; căn cứ ngoài hệ thống không thay workflow duyệt ngoài | D-15 |
| BR-24 | NO_SHOW chỉ từ CONFIRMED sau giờ chốt; đã giao phải nhận trả | D-14 |

Timestamp lưu có múi giờ, ngày đọc theo Asia/Bangkok. Thời điểm giao/trả dùng đồng hồ server, điều chỉnh cần sự kiện riêng.

Theo D-24 đã xác nhận: chỉ bốn vai trò; DIRECTOR là vai trò duy nhất được duyệt, ADMIN không được tự nâng quyền. Nếu DIRECTOR không sẵn sàng thì chờ, không tự chuyển người duyệt. D-25 một vai trò/tài khoản đã xác nhận, không dùng quyền xem bổ sung để biến vai trò khác thành người duyệt.
