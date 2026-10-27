package mrm.notification;

import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import mrm.audit.AuditService;
import mrm.common.BusinessException;

@Service
public class NotificationService {
    public record WorkInfo(String requesterName,String requesterUsername,String recordCode,String patientName,Instant requestedAt,String pickupDates,String pickupRoom) { }
    public record Notice(UUID id,UUID requestId,String title,String message,Instant createdAt,Instant readAt,boolean cancelled,WorkInfo workInfo,boolean pending) { }
    public record Inbox(List<Notice> items,long total,int page,int size,long pendingCount,long handledCount) {
        public long pages(){return (total+size-1)/size;}
    }
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    // An account that changes from Reader to Admin/another role must not retain access to clinical notices.
    private static final String OWNED_BASE = """
        FROM notifications n JOIN app_users u ON u.id=n.recipient_id JOIN roles r ON r.id=u.role_id
        LEFT JOIN reading_requests rq ON rq.id=n.request_id
        LEFT JOIN medical_records mr ON mr.id=rq.record_id
        LEFT JOIN medical_records er ON er.id=n.electronic_record_id
        LEFT JOIN electronic_record_events electronic_event ON electronic_event.id=n.electronic_event_id
        LEFT JOIN staff_change_requests staff ON staff.id=n.staff_request_id
        LEFT JOIN staff_change_events staff_event ON staff_event.id=n.staff_event_id
        LEFT JOIN app_users requester ON requester.id=COALESCE(rq.requester_id,er.created_by,staff.requester_id)
        LEFT JOIN employees employee ON employee.account_id=requester.id
        LEFT JOIN request_events notice_event ON notice_event.id=n.event_id
        WHERE u.username=? AND u.status='ACTIVE' AND r.code=n.recipient_role
        """;
    private static final String OWNED=OWNED_BASE+" AND n.deleted_at IS NULL "+"""
        AND NOT (n.staff_request_id IS NOT NULL AND staff.requester_id=u.id AND staff_event.action='APPROVE')
        AND NOT EXISTS(SELECT 1 FROM reading_requests closed JOIN request_events work ON work.id=n.event_id
            WHERE closed.id=n.request_id AND closed.state='CANCELLED'
            AND work.action IN ('SUBMIT','AMEND','REVIEW_PENDING_APPROVAL'))
        """;
    private static final String PENDING="""
        COALESCE(CASE WHEN n.staff_request_id IS NOT NULL THEN
            (n.recipient_role='DIRECTOR' AND staff_event.action='SUBMIT' AND staff.state='SUBMITTED')
            OR (n.recipient_role='ADMIN' AND staff_event.action='APPROVE' AND staff.state='APPROVED')
            OR (u.id=staff.requester_id AND staff_event.action IN ('APPLY','REJECT') AND n.read_at IS NULL)
            WHEN n.request_id IS NOT NULL THEN
            notice_event.revision=rq.revision AND (
                (n.recipient_role='ARCHIVIST' AND ((notice_event.action IN ('SUBMIT','AMEND') AND rq.state='SUBMITTED')
                    OR (notice_event.action IN ('APPROVE','ARCHIVIST_APPROVE') AND rq.state='APPROVED')))
                OR (n.recipient_role='DIRECTOR' AND notice_event.action='REVIEW_PENDING_APPROVAL' AND rq.state='PENDING_APPROVAL')
                OR (n.recipient_role='READER' AND ((notice_event.action='REVIEW_NEEDS_INFO' AND rq.state='NEEDS_INFO')
                    OR (notice_event.action='CONFIRM' AND rq.state='CONFIRMED' AND n.read_at IS NULL)
                    OR (notice_event.action IN ('APPROVE','ARCHIVIST_APPROVE') AND rq.state='APPROVED'))))
            ELSE electronic_event.version=er.version AND (
                (n.recipient_role='ARCHIVIST' AND electronic_event.action IN ('SUBMIT','AMEND') AND er.submission_state='SUBMITTED')
                OR (n.recipient_role='DIRECTOR' AND electronic_event.action='REVIEW_PENDING_APPROVAL' AND er.submission_state='PENDING_APPROVAL')
                OR (n.recipient_role='READER' AND electronic_event.action='REVIEW_NEEDS_INFO' AND er.submission_state='NEEDS_INFO')) END,FALSE)
        """;
    public NotificationService(JdbcTemplate jdbc,AuditService audit){this.jdbc=jdbc;this.audit=audit;}

    /** Called inside the same transaction that records the request decision/schedule. No email or background job. */
    @Transactional(propagation=Propagation.MANDATORY)
    public void forRequest(UUID requestId,UUID eventId,String action,String details) {
        String title=switch(action){
            case "SUBMIT", "AMEND" -> "Có yêu cầu đọc cần kiểm tra";
            case "REVIEW_PENDING_APPROVAL" -> "Có yêu cầu đọc chờ phê duyệt";
            case "REVIEW_NEEDS_INFO" -> "Yêu cầu đọc cần bổ sung";
            case "APPROVE", "ARCHIVIST_APPROVE" -> "Yêu cầu đọc đã được phê duyệt";
            case "REJECT", "ARCHIVIST_REJECT" -> "Yêu cầu đọc bị từ chối";
            case "CONFIRM" -> "Lịch nhận hồ sơ đã được xác nhận";
            case "CANCEL" -> "Yêu cầu đã hủy";
            default -> null;
        };
        if(title==null)return;
        String days=jdbc.queryForObject("SELECT COALESCE(string_agg(reading_on::text,', ' ORDER BY reading_on),'Chờ nhân viên lưu trữ xếp lịch') FROM reading_sessions WHERE request_id=?",String.class,requestId);
        String message=action.equals("CANCEL")?"":"Yêu cầu "+requestId+". Ngày nhận / lịch: "+days+".\n"+details;
        if(action.equals("CANCEL")) {
            jdbc.update("""
                INSERT INTO notifications(recipient_id,recipient_role,request_id,event_id,title,message)
                SELECT u.id,'ARCHIVIST',q.id,?,'Yêu cầu với hồ sơ '||mr.record_code||' đã hủy',''
                FROM reading_requests q JOIN medical_records mr ON mr.id=q.record_id
                CROSS JOIN app_users u JOIN roles role ON role.id=u.role_id
                WHERE q.id=? AND u.status='ACTIVE' AND role.code='ARCHIVIST'
                ON CONFLICT(recipient_id,event_id) DO NOTHING
                """,eventId,requestId);
        }
        if(action.equals("CONFIRM")) {
            jdbc.update("""
                INSERT INTO notifications(recipient_id,recipient_role,request_id,event_id,title,message)
                SELECT q.requester_id,'READER',q.id,?,'Lịch nhận hồ sơ đã được xác nhận',''
                FROM reading_requests q WHERE q.id=? AND q.state='CONFIRMED'
                ON CONFLICT(recipient_id,event_id) DO NOTHING
                """,eventId,requestId);
            return;
        }
        if(action.equals("APPROVE") || action.equals("ARCHIVIST_APPROVE")) {
            jdbc.update("""
                INSERT INTO notifications(recipient_id,recipient_role,request_id,event_id,title,message)
                SELECT u.id,'ARCHIVIST',?,?,'Có yêu cầu mượn cần xác nhận lịch',''
                FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.status='ACTIVE' AND r.code='ARCHIVIST'
                ON CONFLICT(recipient_id,event_id) DO NOTHING
                """,requestId,eventId);
        }
        String staffRole=switch(action){case "SUBMIT","AMEND" -> "ARCHIVIST";case "REVIEW_PENDING_APPROVAL" -> "DIRECTOR";default -> null;};
        if(staffRole!=null) {
            if(staffRole.equals("ARCHIVIST"))message=jdbc.queryForObject("""
                SELECT 'Người yêu cầu: '||COALESCE(NULLIF(e.full_name,''),u.display_name)||' ('||u.username||')'
                    ||E'\\nHồ sơ yêu cầu: '||mr.record_code||E'\\nNgày giờ yêu cầu: '
                    ||to_char(q.created_at AT TIME ZONE 'Asia/Bangkok','DD/MM/YYYY HH24:MI')
                FROM reading_requests q JOIN app_users u ON u.id=q.requester_id
                LEFT JOIN employees e ON e.account_id=u.id JOIN medical_records mr ON mr.id=q.record_id WHERE q.id=?
                """,String.class,requestId);
            jdbc.update("""
                INSERT INTO notifications(recipient_id,recipient_role,request_id,event_id,title,message)
                SELECT u.id,r.code,?,?,?,? FROM app_users u JOIN roles r ON r.id=u.role_id
                WHERE u.status='ACTIVE' AND r.code=? ON CONFLICT(recipient_id,event_id) DO NOTHING
                """,requestId,eventId,title,message,staffRole);
            return;
        }
        jdbc.update("""
            INSERT INTO notifications(recipient_id,request_id,event_id,title,message)
            SELECT requester_id,id,?,?,? FROM reading_requests WHERE id=?
            ON CONFLICT(recipient_id,event_id) DO NOTHING
            """,eventId,title,message,requestId);
    }
    public long unread(String username){return jdbc.queryForObject("SELECT COUNT(*) "+OWNED+" AND n.read_at IS NULL AND ("+PENDING+")",Long.class,username);}
    @Transactional(propagation=Propagation.MANDATORY)
    public void forElectronic(UUID id,UUID event,String action) {
        String title=switch(action){
            case "SUBMIT","AMEND" -> "Hồ sơ điện tử mới cần kiểm tra";
            case "REVIEW_PENDING_APPROVAL" -> "Hồ sơ điện tử chờ Giám đốc duyệt";
            case "REVIEW_NEEDS_INFO" -> "Hồ sơ điện tử cần bổ sung";
            case "APPROVE" -> "Hồ sơ điện tử đã được công bố";
            case "REJECT" -> "Hồ sơ điện tử bị từ chối";
            case "CANCEL" -> "Hồ sơ điện tử đã hủy";
            default -> throw new IllegalArgumentException("Unknown electronic record event");
        };
        String role=switch(action){case "SUBMIT","AMEND" -> "ARCHIVIST";case "REVIEW_PENDING_APPROVAL" -> "DIRECTOR";default -> "READER";};
        jdbc.update("""
            INSERT INTO notifications(recipient_id,recipient_role,electronic_record_id,electronic_event_id,title,message)
            SELECT u.id,r.code,e.id,?,?,e.record_code FROM electronic_records e
            JOIN app_users u ON TRUE JOIN roles r ON r.id=u.role_id
            WHERE e.id=? AND u.status='ACTIVE' AND r.code=? AND (?<>'READER' OR u.id=e.created_by)
            ON CONFLICT(recipient_id,electronic_event_id) DO NOTHING
            """,event,title,id,role,role);
    }
    public static String scope(String value){
        if(!List.of("pending","handled","all").contains(value))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bộ lọc thông báo không hợp lệ.");
        return value;
    }
    public Inbox inbox(String username,int page,boolean unreadOnly){return inbox(username,page,unreadOnly,"all");}
    public Inbox inbox(String username,int page,boolean unreadOnly,String selectedScope){
        scope(selectedScope);
        if(page<0 || page>1000000)throw new BusinessException("Trang thông báo không hợp lệ.");
        String condition=OWNED+(unreadOnly?" AND n.read_at IS NULL":"")+switch(selectedScope){
            case "pending" -> " AND ("+PENDING+")";case "handled" -> " AND NOT ("+PENDING+")";default -> "";};
        long total=jdbc.queryForObject("SELECT COUNT(*) "+condition,Long.class,username);
        int current=(int)Math.min(page,Math.max(0,(total-1)/20));
        long pendingCount=jdbc.queryForObject("SELECT COUNT(*) "+OWNED+" AND ("+PENDING+")",Long.class,username);
        long handledCount=jdbc.queryForObject("SELECT COUNT(*) "+OWNED+" AND NOT ("+PENDING+")",Long.class,username);
        var rows=jdbc.query("""
            SELECT n.*,COALESCE(rq.created_at,er.created_at,staff.created_at) AS requested_at,
                COALESCE(NULLIF(employee.full_name,''),requester.display_name) AS requester_name,
                requester.username AS requester_username,COALESCE(mr.record_code,er.record_code) AS requested_record_code,
                COALESCE(mr.patient_name,er.patient_name) AS requested_patient_name,
                (notice_event.action='CANCEL') AS cancellation,
                (rq.state='CANCELLED' OR er.submission_state='CANCELLED') AS cancelled_request,
                CASE WHEN notice_event.revision=rq.revision AND notice_event.action='CONFIRM' THEN
                    (SELECT string_agg(to_char(s.reading_on,'DD/MM/YYYY'),', ' ORDER BY s.reading_on) FROM reading_sessions s WHERE s.request_id=rq.id) END AS pickup_dates,
                CASE WHEN notice_event.revision=rq.revision AND notice_event.action='CONFIRM' THEN rq.confirmation_note END AS pickup_room,
                CASE WHEN n.staff_request_id IS NOT NULL THEN n.message
                    WHEN notice_event.action IN ('REVIEW_NEEDS_INFO','REJECT','ARCHIVIST_REJECT') THEN notice_event.details
                    WHEN electronic_event.action IN ('REVIEW_NEEDS_INFO','REJECT') THEN electronic_event.details ELSE '' END AS result_note,
            """+PENDING+" AS pending "+condition+" ORDER BY n.created_at DESC,n.id LIMIT 20 OFFSET ?",(rs,n)->new Notice(
                rs.getObject("id",UUID.class),rs.getObject("staff_request_id",UUID.class)!=null?rs.getObject("staff_request_id",UUID.class):(rs.getObject("request_id",UUID.class)==null?rs.getObject("electronic_record_id",UUID.class):rs.getObject("request_id",UUID.class)),
                rs.getBoolean("cancellation") && rs.getString("recipient_role").equals("READER")?"Yêu cầu đã hủy":rs.getString("title"),
                rs.getString("result_note"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("read_at")==null?null:rs.getTimestamp("read_at").toInstant(),
                rs.getBoolean("cancelled_request") || rs.getBoolean("cancellation"),
                rs.getObject("staff_request_id")==null?new WorkInfo(rs.getString("requester_name"),rs.getString("requester_username"),rs.getString("requested_record_code"),rs.getString("requested_patient_name"),
                    rs.getTimestamp("requested_at").toInstant(),rs.getString("pickup_dates"),rs.getString("pickup_room")):null,rs.getBoolean("pending")),username,(long)current*20);
        return new Inbox(List.copyOf(rows),total,current,20,pendingCount,handledCount);
    }
    @Transactional public boolean deleteHandled(UUID id,String username){
        var rows=jdbc.queryForList("SELECT n.deleted_at,"+PENDING+" AS pending "+OWNED_BASE+" AND n.id=?",username,id);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy thông báo.");
        if(rows.getFirst().get("deleted_at")!=null)return false;
        if(Boolean.TRUE.equals(rows.getFirst().get("pending")))throw new BusinessException("Thông báo còn chờ phê duyệt hoặc xác nhận, chưa thể xóa.");
        int count=jdbc.update("UPDATE notifications SET deleted_at=GREATEST(clock_timestamp(),created_at) WHERE id=? AND deleted_at IS NULL",id);
        if(count>0)audit.log(username,"NOTIFICATION_DELETE","NOTIFICATION",id,"Xóa thông báo đã xử lý khỏi hộp thư cá nhân.");
        return count>0;
    }
    @Transactional public int deleteHandled(String username){
        int count=jdbc.update("UPDATE notifications SET deleted_at=GREATEST(clock_timestamp(),created_at) WHERE deleted_at IS NULL AND id IN (SELECT n.id "+OWNED_BASE+" AND NOT ("+PENDING+"))",username);
        if(count>0)audit.log(username,"NOTIFICATION_DELETE_HANDLED","NOTIFICATION",username,"Xóa "+count+" thông báo đã xử lý khỏi hộp thư cá nhân.");
        return count;
    }
    private UUID ownedRequest(UUID id,String username){
        var rows=jdbc.queryForList("SELECT COALESCE(n.request_id,n.electronic_record_id,n.staff_request_id) "+OWNED+" AND n.id=?",UUID.class,username,id);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy thông báo.");return rows.getFirst();
    }
    public String openPath(UUID notice,String username) {
        ownedRequest(notice,username);
        var staff=jdbc.queryForList("SELECT n.staff_request_id,r.code "+OWNED+" AND n.id=? AND n.staff_request_id IS NOT NULL",username,notice);
        if(!staff.isEmpty()) {
            var n=staff.getFirst();String role=(String)n.get("code");
            return (role.equals("ADMIN")?"/admin/accounts/changes/":role.equals("DIRECTOR")?"/director/staff-change-requests/":"/account/change-requests/")+n.get("staff_request_id");
        }
        if(Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM notifications n LEFT JOIN reading_requests q ON q.id=n.request_id
                LEFT JOIN electronic_records e ON e.id=n.electronic_record_id WHERE n.id=? AND (q.state='CANCELLED' OR e.state='CANCELLED'))
            """,Boolean.class,notice)))return "/notifications";
        return jdbc.queryForObject("SELECT '/' || lower(r.code) || CASE WHEN n.electronic_record_id IS NOT NULL THEN '/records/' ELSE '/requests/' END || COALESCE(n.electronic_record_id,n.request_id)::text "+OWNED+" AND n.id=?",String.class,username,notice);
    }
    @Transactional public UUID read(UUID id,String username){
        UUID request=ownedRequest(id,username);
        int changed=jdbc.update("UPDATE notifications SET read_at=GREATEST(clock_timestamp(),created_at) WHERE id=? AND read_at IS NULL",id);
        if(changed==1)audit.log(username,"NOTIFICATION_READ","NOTIFICATION",id,"Đánh dấu đã đọc thông báo của mình.");
        return request;
    }
    @Transactional public void readAll(String username){
        int count=jdbc.update("""
            UPDATE notifications SET read_at=GREATEST(clock_timestamp(),created_at) WHERE read_at IS NULL AND id IN
            (SELECT n.id
            """+OWNED+")",username);
        if(count>0)audit.log(username,"NOTIFICATIONS_READ_ALL","NOTIFICATION",username,"Số thông báo đánh dấu đã đọc: "+count);
    }
}
