package mrm.report;

import java.util.Map;
import static java.util.Map.entry;
import org.springframework.stereotype.Component;

/** Display labels only: SQL aliases and CSV machine-readable keys remain stable. */
@Component("reportLabels")
public class ReportLabels {
    private static final Map<String,String> COLUMNS=Map.ofEntries(
        entry("actor","Người thao tác"),entry("action","Hành động"),entry("entity_type","Loại đối tượng"),
        entry("entity_id","Mã đối tượng"),entry("outcome","Kết quả"),entry("details","Nội dung"),entry("at","Thời điểm"),
        entry("id","Mã"),entry("archive_number","Số lưu trữ"),entry("medical_record_number","Số bệnh án"),
        entry("patient_code","Mã người bệnh"),entry("patient_name","Người bệnh"),entry("birth_year","Năm sinh"),
        entry("department","Khoa"),entry("record_type","Loại bệnh án"),entry("discharge_date","Ngày ra viện"),
        entry("first_stored_on","Ngày đầu lưu kho"),entry("years","Số năm lưu"),entry("retention_due_on","Hạn lưu"),
        entry("deceased","Tử vong"),entry("state","Trạng thái"),entry("location_code","Mã vị trí"),entry("location_name","Tên vị trí"),
        entry("total","Tổng số"),entry("requester","Người yêu cầu"),entry("requester_name_snapshot","Tên đăng nhập khi đăng ký"),
        entry("purpose","Lý do mượn"),entry("revision","Phiên bản"),entry("reviewer","Người kiểm tra"),entry("approver","Người duyệt"),
        entry("decided_at","Thời điểm quyết định"),entry("decision_note","Căn cứ quyết định"),entry("confirmed_at","Thời điểm xác nhận"),
        entry("confirmation_note","Phòng nhận / hướng dẫn"),entry("request_id","Mã yêu cầu"),entry("reading_on","Ngày nhận"),
        entry("handed_by","Người giao"),entry("handed_at","Thời điểm giao"),entry("returned_by","Người nhận trả"),
        entry("returned_at","Thời điểm trả"),entry("hand_condition","Tình trạng khi giao"),entry("return_condition","Tình trạng khi trả"),
        entry("exception_note","Ghi chú ngoại lệ"),entry("from_path","Vị trí trước"),entry("to_path","Vị trí sau"),
        entry("reason","Lý do"),entry("name","Tên đợt"),entry("found","Tìm thấy"),entry("missing","Thiếu"),
        entry("wrong_location","Sai vị trí"),entry("damaged","Hư hỏng"),entry("created_on","Ngày tạo"),
        entry("avg_hours_submission_to_decision","Thời gian duyệt trung bình (giờ)")
    );
    public String column(String key){return COLUMNS.getOrDefault(key,key);}
}
