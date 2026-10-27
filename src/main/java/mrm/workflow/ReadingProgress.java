package mrm.workflow;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Read-only timeline: real events followed by the remaining approval steps. */
public final class ReadingProgress {
    private ReadingProgress() {}
    public record Node(String title,String actor,String time,String details,String state,String label,String pickupDates,String pickupRoom) {
        public Node(String title,String actor,String time,String details,String state,String label){
            this(title,actor,time,details,state,label,null,null);
        }
    }
    @SuppressWarnings("unchecked")
    public static List<Node> nodes(Map<String,Object> request) {
        var result=new ArrayList<Node>();
        var labels=new ReadingLabels();
        var events=(List<Map<String,Object>>)request.get("events");
        for(var event:events) {
            String action=(String)event.get("action");
            boolean stopped=Set.of("REJECT","ARCHIVIST_REJECT","CANCEL").contains(action);
            String actor=Objects.toString(event.get("actor_name"),Objects.toString(event.get("actor"),"Chưa xác định"));
            Object rawTime=event.get("occurred_at");
            String time=rawTime instanceof Timestamp timestamp
                ? timestamp.toInstant().atZone(ZoneId.of("Asia/Bangkok")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : Objects.toString(rawTime,"");
            // Session dates and confirmation_note belong to the current revision only.
            // Never attach a replacement schedule to an older confirmation after amendment.
            boolean confirmation="CONFIRM".equals(action) && Objects.equals(event.get("revision"),request.get("revision"));
            String pickupDates=null,pickupRoom=null;
            if(confirmation) {
                var sessions=(List<Map<String,Object>>)request.get("sessions");
                pickupDates=sessions.stream().map(session -> session.get("reading_on"))
                    .filter(Objects::nonNull)
                    .map(day -> day instanceof java.sql.Date date?date.toLocalDate():java.time.LocalDate.parse(day.toString()))
                    .distinct().sorted().map(day -> day.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .collect(java.util.stream.Collectors.joining(", "));
                if(pickupDates.isBlank())pickupDates="Chưa có thông tin";
                pickupRoom=Objects.toString(request.get("confirmation_note"),"").strip();
                if(pickupRoom.isBlank())pickupRoom="Chưa có thông tin";
            }
            result.add(new Node(labels.action(action),actor,time,Objects.toString(event.get("details"),""),stopped?"stopped":"done",stopped?(action.equals("CANCEL")?"Đã hủy":"Từ chối"):"Đã xử lý",pickupDates,pickupRoom));
        }
        switch(Objects.toString(request.get("state"),"")) {
            case "SUBMITTED" -> {
                waiting(result,"Kiểm tra yêu cầu","Nhân viên lưu trữ",true);
                if(Boolean.TRUE.equals(request.get("requires_director")))waiting(result,"Giám đốc xét duyệt","Giám đốc",false);
                waiting(result,"Xác nhận ngày nhận","Nhân viên lưu trữ",false);
            }
            case "NEEDS_INFO" -> {
                result.add(new Node("Cần bổ sung thông tin","Người đăng ký","","Xem nội dung yêu cầu bổ sung ở bước trên; liên hệ nhân viên lưu trữ hoặc hủy và đăng ký lại.","current","Đang chờ bổ sung"));
                if(Boolean.TRUE.equals(request.get("requires_director")))waiting(result,"Giám đốc xét duyệt","Giám đốc",false);
                waiting(result,"Xác nhận ngày nhận","Nhân viên lưu trữ",false);
            }
            case "PENDING_APPROVAL" -> {
                waiting(result,"Giám đốc xét duyệt","Giám đốc",true);
                waiting(result,"Xác nhận ngày nhận","Nhân viên lưu trữ",false);
            }
            case "APPROVED" -> waiting(result,"Xác nhận ngày nhận","Nhân viên lưu trữ",true);
            default -> { /* Confirmed or closed: no fictional future processing events. */ }
        }
        return result;
    }
    private static void waiting(List<Node> nodes,String title,String role,boolean current) {
        nodes.add(new Node(title,role,"","Chưa có người xử lý được ghi nhận.",current?"current":"upcoming",current?"Đang chờ xử lý":"Bước tiếp theo"));
    }
}
