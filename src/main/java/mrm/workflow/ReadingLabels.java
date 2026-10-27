package mrm.workflow;

import org.springframework.stereotype.Component;

/** Presentation labels only; persisted workflow codes remain unchanged. */
@Component("readingLabels")
public class ReadingLabels {
    public String recordState(String state,String physical) {
        if("PUBLISHED".equals(state))return "Đã công bố";
        if("DRAFT".equals(state))return "Chưa nhập kho";
        return switch(physical){
            case "IN_STORAGE" -> "Trong kho";
            case "READING" -> "Đang giao đọc";
            case "UNAVAILABLE" -> "Tạm không khả dụng";
            default -> "Chưa nhập kho";
        };
    }
    public String electronicState(String state){return "APPROVED".equals(state)?"Đã công bố":requestState(state);}
    public boolean pending(String state) {
        return "SUBMITTED".equals(state) || "NEEDS_INFO".equals(state) || "PENDING_APPROVAL".equals(state);
    }

    public boolean hasBorrowReason(String purpose) {
        return purpose!=null && !purpose.isBlank()
            && !purpose.strip().equals("Không cung cấp lý do (đăng ký tối đa 5 hồ sơ).");
    }

    public String requestHint(String state) {
        return switch(state) {
            case "SUBMITTED" -> "Chờ nhân viên lưu trữ kiểm tra yêu cầu.";
            case "NEEDS_INFO" -> "Cần bổ sung thông tin; xem chi tiết trong tiến trình xử lý.";
            case "PENDING_APPROVAL" -> "Nhân viên lưu trữ đã kiểm tra, chờ Giám đốc xét duyệt.";
            case "APPROVED" -> "Đã được phê duyệt, chờ nhân viên lưu trữ xác nhận lịch nhận.";
            case "CONFIRMED" -> "Lịch đã được xác nhận; xem tiến trình để biết việc giao và nhận trả.";
            case "REJECTED" -> "Yêu cầu bị từ chối; xem căn cứ trong tiến trình xử lý.";
            case "CANCELLED" -> "Yêu cầu đã hủy.";
            default -> "Mở tiến trình để xem thông tin mới nhất.";
        };
    }

    public String requestState(String state) {
        return switch (state) {
            case "SUBMITTED" -> "Mới gửi";
            case "NEEDS_INFO" -> "Cần bổ sung";
            case "PENDING_APPROVAL" -> "Chờ duyệt";
            case "APPROVED" -> "Đã duyệt, chờ lịch";
            case "REJECTED" -> "Từ chối";
            case "CONFIRMED" -> "Đã xác nhận lịch";
            case "BORROWED" -> "Đang mượn";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            default -> "Không xác định";
        };
    }

    public String action(String action) {
        return switch (action) {
            case "SUBMIT" -> "Gửi yêu cầu";
            case "AMEND_PREVIOUS" -> "Lưu nội dung trước chỉnh sửa";
            case "AMEND" -> "Bổ sung yêu cầu";
            case "REVIEW_NEEDS_INFO" -> "Yêu cầu bổ sung";
            case "REVIEW_PENDING_APPROVAL" -> "Trình Giám đốc duyệt";
            case "ARCHIVIST_APPROVE" -> "Nhân viên lưu trữ phê duyệt";
            case "ARCHIVIST_REJECT" -> "Nhân viên lưu trữ từ chối";
            case "APPROVE" -> "Phê duyệt";
            case "REJECT" -> "Từ chối";
            case "CONFIRM" -> "Xác nhận lịch nhận";
            case "CANCEL" -> "Hủy yêu cầu";
            case "HANDOVER" -> "Giao hồ sơ";
            case "RETURN" -> "Nhận trả hồ sơ";
            case "NO_SHOW" -> "Không đến nhận";
            case "EXCEPTION" -> "Ghi nhận ngoại lệ";
            default -> "Ghi nhận xử lý";
        };
    }
}
