package mrm.account;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import mrm.audit.AuditService;
import mrm.common.BusinessException;

/** Staff proposes privileges; a separate Director decides; an Admin applies the approved proposal. */
@Service
public class StaffChangeService {
    private final JdbcTemplate jdbc;
    private final AccountService accounts;
    private final AuditService audit;
    private static final String SELECT="""
        SELECT q.*,u.username,u.display_name AS requester_name,
            old_dep.name AS original_department_name,new_dep.name AS requested_department_name,
            director.display_name AS decided_by_name,admin.display_name AS applied_by_name
        FROM staff_change_requests q JOIN app_users u ON u.id=q.requester_id
        LEFT JOIN departments old_dep ON old_dep.id=q.original_department_id
        JOIN departments new_dep ON new_dep.id=q.requested_department_id
        LEFT JOIN app_users director ON director.id=q.decided_by
        LEFT JOIN app_users admin ON admin.id=q.applied_by
        """;
    public StaffChangeService(JdbcTemplate jdbc,AccountService accounts,AuditService audit){this.jdbc=jdbc;this.accounts=accounts;this.audit=audit;}
    public static String roleName(String role){return switch(role){case "READER"->"Bác sĩ";case "ARCHIVIST"->"Nhân viên lưu trữ";case "DIRECTOR"->"Giám đốc";default->"Quản trị viên";};}
    public static String stateName(String state){return switch(state){case "SUBMITTED"->"Chờ Giám đốc duyệt";case "APPROVED"->"Đã duyệt, chờ Admin cập nhật";case "REJECTED"->"Từ chối";case "APPLIED"->"Đã cập nhật";default->state;};}
    public List<Map<String,Object>> mine(String by){var user=accounts.actor(by,"READER","ARCHIVIST","DIRECTOR");return jdbc.queryForList(SELECT+" WHERE q.requester_id=? ORDER BY q.created_at DESC,q.id",user.get("id"));}
    public List<Map<String,Object>> queue(String by){
        var user=accounts.actor(by,"DIRECTOR","ADMIN");
        return jdbc.queryForList(SELECT+(user.get("role").equals("DIRECTOR")?" WHERE q.requester_id<>? ORDER BY (q.state='SUBMITTED') DESC,q.created_at DESC":" WHERE ?::uuid IS NOT NULL AND q.state IN ('APPROVED','APPLIED') ORDER BY (q.state='APPROVED') DESC,q.created_at DESC"),user.get("id"));
    }
    public Map<String,Object> detail(UUID id,String by){
        var actor=accounts.actor(by,"READER","ARCHIVIST","DIRECTOR","ADMIN");
        var rows=jdbc.queryForList(SELECT+" WHERE q.id=?",id);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var q=rows.getFirst();String role=(String)actor.get("role");
        boolean own=q.get("requester_id").equals(actor.get("id"));
        if(!own && !role.equals("DIRECTOR") && !role.equals("ADMIN"))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if(role.equals("ADMIN") && !List.of("APPROVED","APPLIED").contains(q.get("state")))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return q;
    }
    @Transactional public UUID submit(String by,String requestedRole,UUID department,String reason,UUID token){
        var actor=accounts.actor(by,"READER","ARCHIVIST","DIRECTOR");
        if(!List.of("READER","ARCHIVIST").contains(requestedRole))throw new BusinessException("Chỉ được đề nghị vai trò Bác sĩ hoặc Nhân viên lưu trữ.");
        if(department==null || !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM departments WHERE id=? AND active)",Boolean.class,department)))throw new BusinessException("Cần chọn khoa hợp lệ.");
        if(reason==null || reason.isBlank() || reason.strip().length()>2000)throw new BusinessException("Cần nhập lý do đề nghị, tối đa 2000 ký tự.");
        if(token==null)throw new BusinessException("Thiếu mã gửi đơn. Tải lại trang.");
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",rs->{},"mrm:staff-change:"+actor.get("id"));
        var existing=jdbc.queryForList("SELECT * FROM staff_change_requests WHERE submit_token=?",token);
        if(!existing.isEmpty()) {
            var old=existing.getFirst();
            if(!old.get("requester_id").equals(actor.get("id")) || !old.get("requested_role").equals(requestedRole) || !old.get("requested_department_id").equals(department) || !old.get("reason").equals(reason.strip()))throw new BusinessException("Mã gửi đơn đã được sử dụng cho nội dung khác.");
            return (UUID)old.get("id");
        }
        var current=jdbc.queryForMap("SELECT role_id,department_id FROM app_users WHERE id=? FOR UPDATE",actor.get("id"));
        if(requestedRole.equals(actor.get("role")) && department.equals(current.get("department_id")))throw new BusinessException("Vai trò và khoa đề nghị không thay đổi so với hiện tại.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM staff_change_requests WHERE requester_id=? AND state IN ('SUBMITTED','APPROVED'))",Boolean.class,actor.get("id"))))throw new BusinessException("Bạn đã có đề nghị đang chờ duyệt hoặc chờ Admin áp dụng.");
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app_users u JOIN roles r ON r.id=u.role_id WHERE r.code='DIRECTOR' AND u.status='ACTIVE' AND u.id<>?)",Boolean.class,actor.get("id"))))throw new BusinessException("Chưa có Giám đốc khác hoạt động để duyệt đề nghị. Không được tự duyệt.");
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO staff_change_requests(id,submit_token,requester_id,original_role,original_department_id,requested_role,requested_department_id,reason) VALUES(?,?,?,?,?,?,?,?)",id,token,actor.get("id"),actor.get("role"),current.get("department_id"),requestedRole,department,reason.strip());
        UUID event=event(id,(UUID)actor.get("id"),"SUBMIT","Gửi đề nghị thay đổi vai trò/khoa.");
        notifyStaff(id,event,"DIRECTOR","Có đề nghị thay đổi vai trò/khoa cần duyệt");
        audit.log(by,"STAFF_CHANGE_SUBMIT","STAFF_CHANGE",id,"Nhân viên gửi đề nghị; chưa thay đổi quyền hoặc khoa.");return id;
    }
    @Transactional public void decide(UUID id,long version,boolean approve,String note,String approvalSecret,String by){
        var actor=accounts.actor(by,"DIRECTOR");var q=lock(id);
        if(q.get("requester_id").equals(actor.get("id")))throw new BusinessException("Giám đốc không được tự xét duyệt đề nghị của mình.");
        if(!"SUBMITTED".equals(q.get("state")) || ((Number)q.get("version")).longValue()!=version)throw new BusinessException("Đề nghị đã được xử lý. Tải lại trang.");
        note=approve?"":(note==null?"":note.strip());
        if(note.length()>2000 || (!approve && note.isBlank()))throw new BusinessException("Từ chối phải nêu lý do, tối đa 2000 ký tự.");
        accounts.verifyApprovalSecret(by,approvalSecret,id);
        jdbc.update("UPDATE staff_change_requests SET state=?,decided_by=?,decided_at=CURRENT_TIMESTAMP,decision_note=?,version=version+1 WHERE id=?",approve?"APPROVED":"REJECTED",actor.get("id"),note,id);
        UUID event=event(id,(UUID)actor.get("id"),approve?"APPROVE":"REJECT",note);
        if(approve)notifyStaff(id,event,"ADMIN","Đề nghị thay đổi vai trò/khoa đã được duyệt, cần cập nhật");
        if(!approve)notifyRequester(id,event,(UUID)q.get("requester_id"),false,note);
        audit.log(by,approve?"STAFF_CHANGE_APPROVE":"STAFF_CHANGE_REJECT","STAFF_CHANGE",id,"Giám đốc xét duyệt; chưa áp dụng quyền/khoa.");
    }
    @Transactional public void apply(UUID id,long version,String by){
        var actor=accounts.actor(by,"ADMIN");var q=lock(id);
        if(!"APPROVED".equals(q.get("state")) || ((Number)q.get("version")).longValue()!=version)throw new BusinessException("Chỉ áp dụng đề nghị đã duyệt và chưa được cập nhật.");
        var user=jdbc.queryForMap("SELECT u.*,r.code AS role FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.id=? FOR UPDATE OF u",q.get("requester_id"));
        if(!"ACTIVE".equals(user.get("status")))throw new BusinessException("Tài khoản nhân viên không hoạt động.");
        if(!Objects.equals(user.get("role"),q.get("original_role")) || !Objects.equals(user.get("department_id"),q.get("original_department_id")))throw new BusinessException("Vai trò/khoa hiện tại đã thay đổi so với đơn. Cần đối chiếu lại trước khi áp dụng, không ghi đè dữ liệu mới.");
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT active FROM departments WHERE id=?",Boolean.class,q.get("requested_department_id"))))throw new BusinessException("Khoa đề nghị không còn hoạt động.");
        if(user.get("role").equals("DIRECTOR")) {
            jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended('mrm:active-director',0))",rs->{});
            if(jdbc.queryForObject("SELECT count(*) FROM app_users u JOIN roles r ON r.id=u.role_id WHERE r.code='DIRECTOR' AND u.status='ACTIVE'",Integer.class)<=1)throw new BusinessException("Cần giữ ít nhất một Giám đốc hoạt động.");
        }
        jdbc.update("UPDATE app_users SET role_id=(SELECT id FROM roles WHERE code=?),department_id=?,approval_secret_hash=NULL,version=version+1 WHERE id=?",q.get("requested_role"),q.get("requested_department_id"),q.get("requester_id"));
        jdbc.update("UPDATE staff_change_requests SET state='APPLIED',applied_by=?,applied_at=CURRENT_TIMESTAMP,version=version+1 WHERE id=?",actor.get("id"),id);
        jdbc.update("UPDATE notifications n SET recipient_role=? WHERE n.recipient_id=? AND EXISTS(SELECT 1 FROM staff_change_requests owned WHERE owned.id=n.staff_request_id AND owned.requester_id=?)",q.get("requested_role"),q.get("requester_id"),q.get("requester_id"));
        UUID appliedEvent=event(id,(UUID)actor.get("id"),"APPLY","Admin áp dụng đúng vai trò/khoa đã được Giám đốc duyệt.");
        notifyRequester(id,appliedEvent,(UUID)q.get("requester_id"),true,"");
        audit.log(by,"STAFF_CHANGE_APPLY","STAFF_CHANGE",id,"Áp dụng đề nghị đã duyệt; thu hồi phiên cũ ở lần truy cập tiếp theo.");
    }
    private Map<String,Object> lock(UUID id){var rows=jdbc.queryForList("SELECT * FROM staff_change_requests WHERE id=? FOR UPDATE",id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);return rows.getFirst();}
    private UUID event(UUID id,UUID actor,String action,String details){UUID event=UUID.randomUUID();jdbc.update("INSERT INTO staff_change_events(id,request_id,actor_id,action,details) VALUES(?,?,?,?,?)",event,id,actor,action,details);return event;}
    private void notifyRequester(UUID id,UUID event,UUID requester,boolean approved,String reason) {
        jdbc.update("""
            INSERT INTO notifications(recipient_id,recipient_role,staff_request_id,staff_event_id,title,message)
            SELECT u.id,r.code,?,?,?,? FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.id=?
            """,id,event,approved?"Đề nghị thay đổi đã được phê duyệt":"Đề nghị thay đổi bị từ chối",approved?"":reason,requester);
    }
    private void notifyStaff(UUID id,UUID event,String role,String title){
        jdbc.update("""
            INSERT INTO notifications(recipient_id,recipient_role,staff_request_id,staff_event_id,title,message)
            SELECT u.id,r.code,q.id,?,?,requester.display_name||' đề nghị đổi vai trò/khoa. Mở đề nghị để xem chi tiết.'
            FROM app_users u JOIN roles r ON r.id=u.role_id CROSS JOIN staff_change_requests q
            JOIN app_users requester ON requester.id=q.requester_id
            WHERE q.id=? AND u.status='ACTIVE' AND r.code=? AND u.id<>q.requester_id
            """,event,title,id,role);
    }
}
