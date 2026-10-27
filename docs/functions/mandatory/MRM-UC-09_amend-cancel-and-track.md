# MRM-UC-09 — Theo dõi, bổ sung, sửa và hủy yêu cầu/phiên

**Trạng thái:** dự thảo v0.1; ưu tiên bắt buộc.

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | Chủ yêu cầu; NVLT hủy phiên theo quyền |
| Nguồn và quy tắc | FR-08/12; D-13; BR-06/14 |
| Tiền điều kiện | Chủ tài khoản chỉ thao tác yêu cầu của mình; trạng thái hiện hành được kiểm tra. |
| Dữ liệu đầu vào | Nội dung bổ sung/đổi quan trọng, lý do hủy, version; danh sách phiên chưa giao nếu hủy từng phiên. |

## Kích hoạt và luồng chính

1. Chủ yêu cầu mở chi tiết, xem timeline và trạng thái phiên.
2. Ở DRAFT/SUBMITTED/NEEDS_INFORMATION: sửa hợp lệ; gửi tạo revision mới, state=SUBMITTED.
3. Ở PENDING_APPROVAL: nếu cần sửa, rút về và tạo revision mới bằng thao tác nguyên tử; quyết định đồng thời phải kiểm tra version.
4. Ở APPROVED chưa từng giao: sửa quan trọng tạo revision mới, hủy phiên chưa giao và yêu cầu duyệt lại; giữ quyết định cũ trong lịch sử.
5. Hủy khi chưa giao: ghi lý do, chuyển phiên chưa thực hiện CANCELLED; nếu hủy toàn phần trước mọi giao thì request=CANCELLED.

## Luồng thay thế và ngoại lệ

E1: Có READING/RETURNED → không thay nội dung revision đã thực hiện; chỉ hủy phần tương lai hoặc tạo request khác cho nội dung mới.

E2: Hủy phiên READING để coi trả → bị chặn; nhận trả qua UC-13.

A1: Đã thực hiện một phần và hủy hết ngày còn lại → request vẫn APPROVED với tiến độ PARTIALLY_FULFILLED, không đổi thành đã hủy toàn bộ.

E3: Người khác truy cập ID → bị chặn; quyết định từ chối không sửa, muốn đăng ký lại tạo request mới.

## Hậu điều kiện, giao dịch và audit

Lịch sử revision/quyết định không mất. Hủy giải phóng chỗ lịch tương lai, không thay physical của lượt đang mở.

Áp dụng [quy tắc chung](COMMON_RULES.md) về quyền, atomicity, version, retry và lỗi mạng. Thất bại không tạo trạng thái nghiệp vụ dở dang.

## Tiêu chí nghiệm thu đề xuất

- **AC-09-01:** Thay mục đích trước mọi giao sau duyệt → phải duyệt lại, quyết định cũ không áp cho revision mới.
- **AC-09-02:** Hủy sau một ngày đã trả → ngày cũ còn nguyên, ngày tương lai hủy.
- **AC-09-03:** Hủy và duyệt đồng thời → chỉ luồng hợp lệ theo version được ghi.

## Điểm cần người dùng rà soát

Xác nhận các D-xx dẫn ở đầu file, dữ liệu bắt buộc và quyền tác nhân trước khi chuyển đặc tả thành cam kết triển khai.
