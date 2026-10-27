# Luồng nghiệp vụ và vòng đời trạng thái

D-02/03/05/12/13/14 đang chờ xác nhận. Quyền và transaction trong use case vẫn áp dụng.

## Luồng xuyên suốt

```mermaid
flowchart TD
 A[READER tra cứu metadata] -> B[Tạo request một HSBA nhiều ngày]
 B -> C[NVLT kiểm tra]
 C ->|Thiếu thông tin| D[Chủ yêu cầu bổ sung revision]
 D -> C
 C ->|Đủ thông tin| E[DIRECTOR xác thực cấp 2]
 E ->|Từ chối| F[Giữ quyết định và lý do]
 E ->|Duyệt| G[NVLT xác nhận từng ngày thuộc phạm vi]
 G -> H[Phiên đúng ngày đủ điều kiện]
 H -> I[NVLT giao tại phòng đọc]
 I -> J[NVLT nhận trả và cất kho]
 I ->|Qua giờ chốt chưa trả| K[Mở ngoại lệ và tiếp tục theo dõi]
 K -> J
 J -> L[Lịch sử và báo cáo]
```

## Yêu cầu và revision

```mermaid
stateDiagram-v2
 [*] -> DRAFT
 DRAFT -> SUBMITTED: Chủ yêu cầu gửi
 SUBMITTED -> NEEDS_INFORMATION: NVLT trả bổ sung
 NEEDS_INFORMATION -> SUBMITTED: Gửi revision mới
 SUBMITTED -> PENDING_APPROVAL: NVLT trình
 PENDING_APPROVAL -> APPROVED: Quyết định hợp lệ
 PENDING_APPROVAL -> REJECTED: Từ chối có lý do
 PENDING_APPROVAL -> SUBMITTED: Rút sửa bằng revision mới
 APPROVED -> SUBMITTED: Sửa quan trọng trước mọi giao
 DRAFT -> CANCELLED: Chủ hủy
 SUBMITTED -> CANCELLED: Chủ hủy
 NEEDS_INFORMATION -> CANCELLED: Chủ hủy
 PENDING_APPROVAL -> CANCELLED: Chủ hủy trước quyết định
 APPROVED -> CANCELLED: Hủy toàn bộ trước mọi giao
```

APPROVED/REJECTED nói về quyết định hiện hành; tiến độ không là trạng thái phê duyệt. Revision cũ không sửa: lưu owner, HSBA, mục đích, khoa snapshot và ngày. Bản nháp có thể sửa; khi gửi tạo snapshot. Sửa SUBMITTED/NEEDS_INFORMATION/PENDING_APPROVAL tạo revision mới, quyết định cũ không áp dụng. Nếu READING/RETURNED đã tồn tại, không thay revision đó; chỉ hủy phiên chưa giao, nội dung mới dùng request khác.

Tiến độ dẫn xuất khi APPROVED: WAITING_SCHEDULE nếu còn PLANNED; SCHEDULED nếu chỉ còn lịch chưa thực hiện; IN_PROGRESS khi có READING; FULFILLED khi mọi phiên RETURNED; PARTIALLY_FULFILLED khi có RETURNED và mọi phiên còn lại CANCELLED/NO_SHOW/EXPIRED; NOT_FULFILLED khi không RETURNED, tất cả phiên kết thúc không thực hiện. Có thể hiển thị số phiên từng trạng thái thay vì ép một nhãn nếu chưa kết thúc.

## Phiên đọc

| Từ → Đến | Ai | Điều kiện | Dữ liệu/kết quả |
|---|---|---|---|
| Chưa có → PLANNED | Hệ thống cùng quyết định duyệt | Ngày trong revision APPROVED | Một phiên/ngày/request_revision, decision_id |
| PLANNED → CONFIRMED | NVLT | Ngày còn hợp lệ, trong phạm vi duyệt, không trùng chỗ | Người/giờ xác nhận, lịch được giữ |
| PLANNED → EXPIRED | Hệ thống/NVLT khi đối soát | Ngày đã qua chưa xác nhận | Lý do, lịch sử; không chuyển ngày |
| CONFIRMED → READING | NVLT | Đúng ngày, HSBA trong kho không block, không lượt mở | Handover, physical chuyển cùng transaction |
| READING → RETURNED | NVLT | Thực nhận đúng HSBA, vị trí hợp lệ | Giờ/tình trạng/người trả/nhận, cất kho |
| PLANNED/CONFIRMED → CANCELLED | Chủ/NVLT theo quyền | Chưa giao, có lý do | Giải phóng chỗ, giữ lịch sử |
| CONFIRMED → NO_SHOW | NVLT | Sau giờ chốt, chưa có handover | Lý do, không đổi physical |

RETURNED/CANCELLED/NO_SHOW/EXPIRED là kết thúc; không hồi sinh để ghi lại một lượt khác. Sửa sai dùng adjustment. Qua giờ chốt READING vẫn READING, chỉ thêm OVERDUE. Không có transition READING → CANCELLED/NO_SHOW.

## HSBA vật lý và tình trạng

- IN_STORAGE → AT_READING_ROOM chỉ bởi giao hợp lệ.
- AT_READING_ROOM → IN_STORAGE chỉ bởi nhận trả thực tế.
- IN_STORAGE → TEMP_UNAVAILABLE chỉ khi không lượt mở và có căn cứ công việc ngoài hệ thống/đối soát.
- TEMP_UNAVAILABLE → IN_STORAGE sau kiểm tra thực tế, ghi người/căn cứ; không tự chuyển theo lịch.
- Cờ condition: NORMAL, DAMAGED, MISSING, NEEDS_RECONCILIATION; blocked_for_use tính từ cờ/nghiệp vụ đang mở, không tùy ý bỏ qua khi giao.
- Vị trí current_location khi AT_READING_ROOM là địa chỉ kho về dự kiến, không khẳng định hồ sơ ở đó. Giao diện phải thể hiện phòng đọc và trạng thái vật lý.

## Ngoại lệ và thông báo

Exception OPEN → IN_REVIEW → RESOLVED, diễn biến chỉ thêm. Chưa trả và hư hỏng có thể là hai exception khác nhau; trả giải quyết quá hạn nhưng chưa giải quyết hỏng. Không đóng MISSING bằng RETURNED giả.

Email nếu chọn: PENDING → ACCEPTED hoặc FAILED, FAILED → PENDING khi retry; ACCEPTED chỉ là SMTP chấp nhận, không bảo đảm thư đã vào hộp nhận. Không dùng trạng thái mail để thay quyết định/lịch.

## Bất biến cần đối chiếu

Mỗi READING có đúng một handover mở; mỗi handover mở có physical AT_READING_ROOM và đúng session; RETURNED có giờ trả; không có hai handover mở cùng HSBA. Nếu phát hiện sai phải đối soát có căn cứ, không đồng bộ bằng cách xóa lịch sử.
