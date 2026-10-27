package mrm.report;

import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import mrm.account.AccountService;
import mrm.common.BusinessException;

@Service
public class ReportService {
    public static final Map<String,String> TYPES;
    static {var map=new LinkedHashMap<String,String>();map.put("records","Danh mục hồ sơ và thời hạn lưu");map.put("status","Tổng hợp kho theo khoa / loại / trạng thái");map.put("requests","Yêu cầu, quyết định và lịch xác nhận");map.put("sessions","Sổ giao nhận / chưa trả / ngoại lệ");map.put("locations","Lịch sử thay đổi vị trí");map.put("audit","Nhật ký kiểm toán");map.put("inventory","Kết quả kiểm kê");map.put("request-statistics","Thống kê xử lý yêu cầu theo ngày tạo");TYPES=Collections.unmodifiableMap(map);}
    public record Filter(String q,String actor,String action,String entity,LocalDate from,LocalDate to,String state) {
        public Filter {q=q==null?"":q.strip();actor=actor==null?"":actor.strip();action=action==null?"":action.strip();entity=entity==null?"":entity.strip();state=state==null?"":state.strip();}
    }
    private final JdbcTemplate jdbc;private final AccountService accounts;
    public ReportService(JdbcTemplate jdbc,AccountService accounts){this.jdbc=jdbc;this.accounts=accounts;}
    public List<Map<String,Object>> rows(String type,Filter filter,String by) {
        var actor=accounts.actor(by,"ARCHIVIST","DIRECTOR","ADMIN");boolean arch=actor.get("role").equals("ARCHIVIST"),admin=actor.get("role").equals("ADMIN");
        if(!TYPES.containsKey(type) || (admin && !type.equals("audit")) || (!arch && type.equals("locations")) || (actor.get("role").equals("DIRECTOR") && type.equals("inventory")))throw new org.springframework.security.access.AccessDeniedException("Không có quyền báo cáo này.");
        if(filter.from()!=null && filter.to()!=null && filter.from().isAfter(filter.to()))throw new BusinessException("Khoảng ngày báo cáo bị đảo.");
        if(filter.q().length()>200 || filter.actor().length()>64 || filter.action().length()>64 || filter.entity().length()>100 || filter.state().length()>32)throw new BusinessException("Bộ lọc quá dài.");
        String sql=switch(type){
            case "records" -> "SELECT r.id,r.archive_number,r.medical_record_number,r.patient_code,r.patient_name,r.birth_year,r.department_name_snapshot AS department,r.record_type_name_snapshot AS record_type,r.discharge_date,r.first_stored_on,r.retention_years_snapshot AS years,r.retention_due_on,r.deceased,r.physical_status AS state,r.updated_at AS at"+(arch?",l.code AS location_code,l.name AS location_name":"")+" FROM medical_records r LEFT JOIN storage_locations l ON l.id=r.current_location_id WHERE r.record_state='STORED'";
            case "status" -> "SELECT department_name_snapshot AS department,record_type_name_snapshot AS record_type,physical_status AS state,retention_years_snapshot AS years,COUNT(*) AS total,CURRENT_TIMESTAMP AS at FROM medical_records WHERE record_state='STORED' GROUP BY department_name_snapshot,record_type_name_snapshot,physical_status,retention_years_snapshot";
            case "requests" -> "SELECT q.id,r.archive_number,r.patient_name,u.username AS requester,q.requester_name_snapshot,q.purpose,q.state,q.revision,review.username AS reviewer,approve.username AS approver,q.decided_at,q.decision_note,q.confirmed_at,q.confirmation_note,q.created_at AS at FROM reading_requests q JOIN medical_records r ON r.id=q.record_id JOIN app_users u ON u.id=q.requester_id LEFT JOIN app_users review ON review.id=q.reviewer_id LEFT JOIN app_users approve ON approve.id=q.approver_id";
            case "sessions" -> "SELECT s.id,q.id AS request_id,r.archive_number,r.patient_name,u.username AS requester,q.requester_name_snapshot,s.reading_on,s.state,g.username AS handed_by,s.handed_at,t.username AS returned_by,s.returned_at,s.hand_condition,s.return_condition,s.exception_note,s.reading_on::timestamp AS at FROM reading_sessions s JOIN reading_requests q ON q.id=s.request_id JOIN medical_records r ON r.id=s.record_id JOIN app_users u ON u.id=q.requester_id LEFT JOIN app_users g ON g.id=s.handed_by LEFT JOIN app_users t ON t.id=s.returned_by";
            case "locations" -> "SELECT h.record_id AS id,r.archive_number,h.from_path,h.to_path,h.actor_username AS actor,h.reason,h.occurred_at AS at FROM record_location_history h JOIN medical_records r ON r.id=h.record_id";
            case "inventory" -> "SELECT b.id,b.name,b.state,COUNT(i.record_id) AS total,COUNT(*) FILTER(WHERE i.observed='FOUND') AS found,COUNT(*) FILTER(WHERE i.observed='MISSING') AS missing,COUNT(*) FILTER(WHERE i.observed='WRONG_LOCATION') AS wrong_location,COUNT(*) FILTER(WHERE i.observed='DAMAGED') AS damaged,b.created_at AS at FROM inventory_batches b LEFT JOIN inventory_items i ON i.batch_id=b.id GROUP BY b.id";
            case "request-statistics" -> "SELECT (created_at AT TIME ZONE 'Asia/Bangkok')::date AS created_on,state,COUNT(*) AS total,ROUND(AVG(EXTRACT(EPOCH FROM (decided_at-created_at))/3600)::numeric,2) AS avg_hours_submission_to_decision,MIN(created_at) AS at FROM reading_requests GROUP BY (created_at AT TIME ZONE 'Asia/Bangkok')::date,state";
            default -> admin ? "SELECT actor,action,entity_type,entity_id,outcome,details,occurred_at AS at FROM audit_events WHERE entity_type IN ('ACCOUNT','AUTH')" : !arch ? """
                SELECT actor,action,entity_type,entity_id,outcome,details,occurred_at AS at FROM audit_events WHERE entity_type IN ('AUTH','REQUEST','REPORT')
                UNION ALL SELECT actor_username,action,'RECORD',record_id::text,'SUCCESS','Chi tiết nội bộ thuộc Archivist',occurred_at FROM record_events
                UNION ALL SELECT actor_username,action,'CATALOG',target_id::text,'SUCCESS','Thay đổi danh mục',occurred_at FROM catalog_events
                """ : """
                SELECT actor,action,entity_type,entity_id,outcome,details,occurred_at AS at FROM audit_events
                UNION ALL SELECT actor_username,action,'RECORD',record_id::text,'SUCCESS',reason,occurred_at FROM record_events
                UNION ALL SELECT actor_username,action,'LOCATION',location_id::text,'SUCCESS',details,occurred_at FROM location_events
                UNION ALL SELECT actor_username,action,'CATALOG',target_id::text,'SUCCESS',details::text,occurred_at FROM catalog_events
                """;
        };
        // Wrap each fixed report in one parameterized filter. Never use request text as a column name or SQL expression.
        var args=new MapSqlParameterSource().addValue("q","%"+filter.q().replace("!","!!").replace("%","!%").replace("_","!_")+"%")
            .addValue("actor",filter.actor()).addValue("action",filter.action()).addValue("entity",filter.entity()).addValue("state",filter.state())
            .addValue("from",filter.from()).addValue("to",filter.to());
        String where=" WHERE (CAST(:from AS DATE) IS NULL OR at >= CAST(:from AS DATE)) AND (CAST(:to AS DATE) IS NULL OR at < CAST(:to AS DATE)+INTERVAL '1 day')";
        if(!filter.q().isBlank())where+=" AND row_to_json(report)::text ILIKE :q ESCAPE '!'";
        if(type.equals("audit"))where+=" AND (:actor='' OR actor=:actor) AND (:action='' OR action=:action) AND (:entity='' OR entity_id=:entity)";
        if(!List.of("audit","locations").contains(type))where+=" AND (:state='' OR state=:state)";
        return new NamedParameterJdbcTemplate(jdbc).queryForList("SELECT * FROM ("+sql+") report"+where+" ORDER BY at DESC",args);
    }
    public static String csv(List<Map<String,Object>> rows) {
        if(rows.isEmpty())return "\ufeffKhông có dữ liệu\r\n";
        var result=new StringBuilder("\ufeff");var columns=rows.getFirst().keySet();result.append(String.join(",",columns.stream().map(ReportService::cell).toList())).append("\r\n");
        for(var row:rows)result.append(String.join(",",columns.stream().map(c->cell(Objects.toString(row.get(c),""))).toList())).append("\r\n");return result.toString();
    }
    private static String cell(String value){String trimmed=value.stripLeading();if(!trimmed.isEmpty() && "=+@-".indexOf(trimmed.charAt(0))>=0)value="'"+value;return "\""+value.replace("\"","\"\"")+"\"";}
}
