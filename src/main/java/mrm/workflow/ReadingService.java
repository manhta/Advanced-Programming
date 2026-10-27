package mrm.workflow;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mrm.account.AccountService;
import mrm.audit.AuditService;
import mrm.common.BusinessException;
import mrm.record.RecordRepository;
import mrm.storage.StorageRepository;

/** One request concerns one record; each reading date has its own handover/return. */
@Service
public class ReadingService {
    private final JdbcTemplate jdbc;
    private final AccountService accounts;
    private final AuditService audit;
    private final PasswordEncoder encoder;
    private final RecordRepository records;
    private final StorageRepository storage;
    private final mrm.notification.NotificationService notifications;
    public ReadingService(JdbcTemplate jdbc,AccountService accounts,AuditService audit,PasswordEncoder encoder,RecordRepository records,StorageRepository storage,mrm.notification.NotificationService notifications) {
        this.jdbc=jdbc;this.accounts=accounts;this.audit=audit;this.encoder=encoder;this.records=records;this.storage=storage;
        this.notifications=notifications;
    }
    public static LocalDate today(){return LocalDate.now(ZoneId.of("Asia/Bangkok"));}
    private static String required(String text,String field) {
        if(text==null || text.isBlank() || text.strip().length()>2000)throw new BusinessException(field+" cần 1–2000 ký tự.");return text.strip();
    }
    private static final String REQUEST_JOINS="""
        FROM reading_requests q JOIN medical_records r ON r.id=q.record_id JOIN app_users u ON u.id=q.requester_id
        LEFT JOIN LATERAL (SELECT
            count(*) FILTER(WHERE s.state='READING') AS reading_count,
            count(*) FILTER(WHERE s.state='RETURNED' AND s.returned_at IS NOT NULL) AS returned_count,
            count(*) FILTER(WHERE s.state NOT IN ('RETURNED','CANCELLED') OR (s.state='RETURNED' AND s.returned_at IS NULL)) AS unfinished_count,
            string_agg(s.reading_on::text,', ' ORDER BY s.reading_on) FILTER(WHERE s.state<>'CANCELLED') AS dates,
            to_char(max(s.returned_at) AT TIME ZONE 'Asia/Bangkok','DD/MM/YYYY') AS returned_on
            FROM reading_sessions s WHERE s.request_id=q.id) session_stats ON TRUE
        """;
    private static final String DISPLAY_STATE="""
        CASE WHEN q.state='CANCELLED' THEN 'CANCELLED'
            WHEN q.state='CONFIRMED' AND session_stats.reading_count>0 THEN 'BORROWED'
            WHEN q.state='CONFIRMED' AND session_stats.returned_count>0 AND session_stats.unfinished_count=0 THEN 'COMPLETED'
            ELSE q.state END
        """;
    private static final List<String> REQUEST_STATES=List.of("SUBMITTED","NEEDS_INFO","PENDING_APPROVAL","APPROVED","CONFIRMED","BORROWED","COMPLETED","REJECTED","CANCELLED");
    public List<Map<String,Object>> list(String username) {
        var actor=accounts.actor(username,"READER","ARCHIVIST","DIRECTOR");
        return jdbc.queryForList("""
            SELECT q.id,q.version,q.requires_director,q.record_id,q.batch_id,q.registered_on,q.pickup_deadline,q.scheduling_mode,q.requester_name_snapshot,q.state,q.revision,q.created_at,q.updated_at,q.purpose,r.archive_number,r.patient_name,u.username AS requester,
                session_stats.dates,session_stats.returned_on,
            """+DISPLAY_STATE+" AS display_state "+REQUEST_JOINS+" WHERE (?<>'READER' OR q.requester_id=?) AND (?<>'DIRECTOR' OR q.requires_director=TRUE) ORDER BY q.registered_on DESC,q.created_at DESC,q.id",actor.get("role"),actor.get("id"),actor.get("role"));
    }
    public record RequestPage(List<Map<String,Object>> items,long total,int page,LocalDate from,LocalDate to,String query,String status,String sort,List<String> availableStates) {
        public long pages(){return Math.max(1,(total+9)/10);}
    }
    public RequestPage readerHistory(String username,String query,LocalDate from,LocalDate to,int page) {
        return readerHistory(username,query,from,to,page,"","registered");
    }
    public RequestPage readerHistory(String username,String query,LocalDate from,LocalDate to,int page,String status,String sort) {
        var actor=accounts.actor(username,"READER");
        status=status==null?"":status.strip();sort=sort==null?"registered":sort;
        if(!status.isEmpty() && !REQUEST_STATES.contains(status))throw new BusinessException("Trạng thái đăng ký không hợp lệ.");
        if(!List.of("registered","registered_asc","status","status_desc").contains(sort))throw new BusinessException("Cách sắp xếp không hợp lệ.");
        var ownedStates=jdbc.queryForList("SELECT DISTINCT "+DISPLAY_STATE+" AS display_state "+REQUEST_JOINS+" WHERE q.requester_id=?",String.class,actor.get("id"));
        var availableStates=REQUEST_STATES.stream().filter(ownedStates::contains).toList();
        LocalDate end=to==null?today():to, start=from==null?end.minusDays(6):from;
        if(start.isAfter(end))throw new BusinessException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        String text=query==null?"":query.strip();
        if(text.length()>200)throw new BusinessException("Tên hoặc số lưu trữ tối đa 200 ký tự.");
        String match="%"+text.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        String where=REQUEST_JOINS+" WHERE q.requester_id=? AND q.registered_on BETWEEN ? AND ? AND (r.patient_name ILIKE ? ESCAPE '!' OR r.archive_number ILIKE ? ESCAPE '!') AND (?='' OR ("+DISPLAY_STATE+")=?)";
        Object[] args={actor.get("id"),start,end,match,match,status,status};
        long total=jdbc.queryForObject("SELECT COUNT(*)"+where,Long.class,args);
        int current=(int)Math.min(Math.max(0,page),Math.max(0,(total-1)/10));
        var params=new ArrayList<Object>(Arrays.asList(args));params.add(current*10);
        String order=switch(sort) {
            case "status","status_desc" -> "array_position(ARRAY['SUBMITTED','NEEDS_INFO','PENDING_APPROVAL','APPROVED','CONFIRMED','BORROWED','COMPLETED','REJECTED','CANCELLED'],("+DISPLAY_STATE+")) "+(sort.equals("status")?"ASC":"DESC")+",q.registered_on ASC,q.created_at ASC,q.id";
            case "registered_asc" -> "q.registered_on ASC,q.created_at ASC,q.id";
            default -> "q.registered_on DESC,q.created_at DESC,q.id";
        };
        var items=jdbc.queryForList("SELECT q.id,q.version,q.record_id,q.registered_on,q.state,q.purpose,u.username AS requester,r.archive_number,session_stats.dates,session_stats.returned_on,"+DISPLAY_STATE+" AS display_state "+where+" ORDER BY "+order+" LIMIT 10 OFFSET ?",params.toArray());
        return new RequestPage(items,total,current,start,end,text,status,sort,availableStates);
    }
    public Map<String,Object> detail(UUID id,String username) {
        var actor=accounts.actor(username,"READER","ARCHIVIST","DIRECTOR");
        var rows=jdbc.queryForList("""
            SELECT q.*,r.archive_number,r.patient_name,u.username AS requester FROM reading_requests q
            JOIN medical_records r ON r.id=q.record_id JOIN app_users u ON u.id=q.requester_id
            WHERE q.id=? AND (?<>'READER' OR q.requester_id=?)
            """,id,actor.get("role"),actor.get("id"));
        if(rows.isEmpty())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Không tìm thấy yêu cầu.");
        var result=rows.getFirst();
        // Explicit session projection: Reader never receives a shelf/compartment UUID or path.
        result.put("sessions",jdbc.queryForList("SELECT id,reading_on,state,handed_at,returned_at,exception_note FROM reading_sessions WHERE request_id=? ORDER BY reading_on",id));
        result.put("events",jdbc.queryForList("""
            SELECT e.actor,e.action,e.revision,e.details,e.occurred_at,
                COALESCE(NULLIF(employee.full_name,''),NULLIF(u.display_name,''),e.actor) AS actor_name
            FROM request_events e LEFT JOIN app_users u ON u.username=e.actor
            LEFT JOIN employees employee ON employee.account_id=u.id
            WHERE e.request_id=? ORDER BY e.occurred_at,e.id
            """,id));
        return result;
    }
    private Map<String,Object> lock(UUID id) {
        var list=jdbc.queryForList("SELECT * FROM reading_requests WHERE id=? FOR UPDATE",id);
        if(list.isEmpty())throw new BusinessException("Yêu cầu không tồn tại.");return list.getFirst();
    }
    private void version(Map<String,Object> q,long version) {
        if(((Number)q.get("version")).longValue()!=version)throw new BusinessException("Yêu cầu đã thay đổi. Tải lại trang trước khi thao tác.");
    }
    private void state(Map<String,Object> q,String... states) {if(!Arrays.asList(states).contains(q.get("state")))throw new BusinessException("Thao tác không hợp lệ ở trạng thái "+q.get("state")+".");}
    private void event(UUID id,String by,String action,String details) {
        UUID eventId=jdbc.queryForObject("INSERT INTO request_events(request_id,actor,action,revision,details) SELECT id,?,?,revision,? FROM reading_requests WHERE id=? RETURNING id",UUID.class,by,action,details,id);
        audit.log(by,action,"REQUEST",id,details);
        notifications.forRequest(id,eventId,action,details);
    }
    private void eligible(UUID recordId) {
        records.lock(recordId);
        var r=records.details(recordId);
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT has_paper_copy FROM medical_records WHERE id=?",Boolean.class,recordId)))throw new BusinessException("Chỉ được mượn hồ sơ có bản giấy.");
        if(!r.state().equals("STORED"))throw new BusinessException("Chỉ đăng ký hồ sơ chính thức đã nhập kho.");
        if(!r.physicalStatus().equals("IN_STORAGE") || !r.form().qualityStatus().equals("NORMAL"))throw new BusinessException("Hồ sơ hiện không đủ điều kiện khai thác.");
    }
    @Transactional public UUID submit(UUID recordId,String purpose,UUID token,String by) {
        return submitBatch(List.of(recordId),purpose,token,by).getFirst();
    }
    @Transactional public List<UUID> submitBatch(List<UUID> recordIds,String purpose,UUID token,String by) {
        var actor=accounts.actor(by,"READER");
        if(token==null)throw new BusinessException("Thiếu mã đăng ký.");
        if(recordIds==null || recordIds.isEmpty() || recordIds.size()>20 || recordIds.stream().anyMatch(Objects::isNull)
                || new HashSet<>(recordIds).size()!=recordIds.size())throw new BusinessException("Chọn từ 1 đến 20 hồ sơ khác nhau.");
        var sorted=recordIds.stream().sorted().toList();
        if(purpose==null || purpose.isBlank()) {
            if(sorted.size()>5)throw new BusinessException("Vui lòng nhập lý do khi mượn trên 5 hồ sơ.");
            purpose="Không cung cấp lý do (đăng ký tối đa 5 hồ sơ).";
        } else purpose=required(purpose,"Lý do mượn");
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",rs->{},"mrm:request:"+token);
        var old=jdbc.queryForList("SELECT * FROM reading_request_batches WHERE submit_token=?",token);
        if(!old.isEmpty()) {
            var batch=old.getFirst();
            var previous=jdbc.queryForList("SELECT record_id FROM reading_requests WHERE batch_id=? ORDER BY record_id",UUID.class,batch.get("id"));
            if(!batch.get("requester_id").equals(actor.get("id")) || !batch.get("purpose").equals(purpose) || !new HashSet<>(previous).equals(new HashSet<>(sorted)))
                throw new BusinessException("Phiếu đã dùng với nội dung khác. Mở đăng ký mới.");
            return jdbc.queryForList("SELECT id FROM reading_requests WHERE batch_id=?",UUID.class,batch.get("id")).stream().sorted().toList();
        }
        for(UUID record:sorted) {
            eligible(record); // Locks stay held through commit; another registration must recheck after this one commits.
            if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT "+mrm.record.BorrowAvailability.NO_ACTIVE_REQUEST+" FROM medical_records mr WHERE mr.id=?",Boolean.class,record)))
                throw new BusinessException("Hồ sơ đã có đăng ký mượn chưa hoàn tất. Vui lòng tải lại danh sách và chọn hồ sơ khác.");
        }
        boolean requiresDirector=sorted.stream().anyMatch(record -> Boolean.TRUE.equals(jdbc.queryForObject("SELECT deceased FROM medical_records WHERE id=?",Boolean.class,record)));
        if(requiresDirector && !new ReadingLabels().hasBorrowReason(purpose))throw new BusinessException("Lượt đăng ký có hồ sơ tử vong: bắt buộc nhập lý do mượn cho toàn bộ lượt.");
        UUID batch=UUID.randomUUID();LocalDate registered=today();
        jdbc.update("INSERT INTO reading_request_batches(id,submit_token,requester_id,purpose,registered_on,requires_director) VALUES(?,?,?,?,?,?)",batch,token,actor.get("id"),purpose,registered,requiresDirector);
        var result=new ArrayList<UUID>();
        for(UUID record:sorted) {
            UUID id=UUID.randomUUID(),childToken=UUID.nameUUIDFromBytes((token+":"+record).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            jdbc.update("""
                INSERT INTO reading_requests(id,submit_token,record_id,requester_id,requester_name_snapshot,purpose,state,batch_id,registered_on,scheduling_mode,requires_director)
                SELECT ?,?,?,id,username,?,'SUBMITTED',?,?,'ARCHIVIST',? FROM app_users WHERE id=?
                """,id,childToken,record,purpose,batch,registered,requiresDirector,actor.get("id"));
            event(id,by,"SUBMIT","Mục đích: "+purpose+"; đăng ký "+registered+"; hạn nhận "+registered.plusDays(2)+"; "+(requiresDirector?"Cả lượt có hồ sơ tử vong, cần Giám đốc duyệt.":"Nhân viên lưu trữ phê duyệt.")+" Nhân viên lưu trữ xếp lịch.");
            result.add(id);
        }
        return result.stream().sorted().toList();
    }
    @Transactional public void amend(UUID id,long version,String purpose,String by) {
        var actor=accounts.actor(by,"READER");var q=lock(id);version(q,version);
        if(!q.get("requester_id").equals(actor.get("id")))throw new org.springframework.security.access.AccessDeniedException("Chỉ sửa yêu cầu của mình.");
        state(q,"SUBMITTED","NEEDS_INFO","PENDING_APPROVAL","APPROVED","CONFIRMED");
        if(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM reading_sessions WHERE request_id=? AND handed_at IS NOT NULL)",Boolean.class,id))throw new BusinessException("Đã giao hồ sơ; hủy phần chưa thực hiện và đăng ký mới thay vì sửa.");
        purpose=required(purpose,"Mục đích");eligible((UUID)q.get("record_id"));
        event(id,by,"AMEND_PREVIOUS","Phiên bản trước: "+q.get("purpose")+"; lịch: "+jdbc.queryForList("SELECT reading_on FROM reading_sessions WHERE request_id=? ORDER BY reading_on",LocalDate.class,id));
        jdbc.update("DELETE FROM reading_sessions WHERE request_id=?",id);
        jdbc.update("UPDATE reading_requests SET purpose=?,state='SUBMITTED',scheduling_mode='ARCHIVIST',revision=revision+1,version=version+1,reviewer_id=NULL,reviewed_at=NULL,review_note=NULL,approver_id=NULL,decided_at=NULL,decision_note=NULL,confirmation_note=NULL,confirmed_by=NULL,confirmed_at=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=?",purpose,id);
        event(id,by,"AMEND","Nội dung mới: "+purpose+"; nhân viên lưu trữ xếp lịch lại; hạn nhận không thay đổi.");
    }
    private void pickupWindow(Map<String,Object> q,LocalDate day) {
        LocalDate registered=((java.sql.Date)q.get("registered_on")).toLocalDate();
        LocalDate deadline=((java.sql.Date)q.get("pickup_deadline")).toLocalDate();
        if(day==null || day.isBefore(today()) || day.isBefore(registered) || day.isAfter(deadline))
            throw new BusinessException("Ngày nhận phải từ hôm nay đến "+deadline+" (không quá 2 ngày lịch kể từ đăng ký). Nếu hết hạn, cần đăng ký mới.");
    }
    @Transactional public void review(UUID id,long version,String action,String note,String by) {
        var actor=accounts.actor(by,"ARCHIVIST");var q=lock(id);version(q,version);state(q,"SUBMITTED");
        if(List.of("NEEDS_INFO","REJECTED").contains(action))note=required(note,action.equals("NEEDS_INFO")?"Cần bổ sung":"Lý do hủy / từ chối");
        else if(note==null || note.isBlank())note=action.equals("APPROVED")?"Nhân viên lưu trữ phê duyệt cho mượn.":"Đủ điều kiện, trình Giám đốc phê duyệt.";
        else note=required(note,"Ghi chú kiểm tra");
        boolean director=Boolean.TRUE.equals(q.get("requires_director"));
        if(!(director?List.of("NEEDS_INFO","PENDING_APPROVAL"):List.of("NEEDS_INFO","APPROVED","REJECTED")).contains(action))
            throw new BusinessException(director?"Cả lượt phải trình Giám đốc; nhân viên lưu trữ không được tự duyệt.":"Nhân viên lưu trữ phê duyệt hoặc từ chối yêu cầu thông thường, không trình Giám đốc.");
        if(!action.equals("NEEDS_INFO") && !action.equals("REJECTED")){eligible((UUID)q.get("record_id"));ensureRoute(q);}
        if(action.equals("APPROVED") && q.get("scheduling_mode").equals("ARCHIVIST"))pickupWindow(q,today());
        jdbc.update("UPDATE reading_requests SET state=?,review_note=?,reviewer_id=?,reviewed_at=CURRENT_TIMESTAMP,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",action,note,actor.get("id"),id);
        if(action.equals("APPROVED") || action.equals("REJECTED")) {
            jdbc.update("UPDATE reading_requests SET approver_id=?,decided_at=CURRENT_TIMESTAMP,decision_note=? WHERE id=?",actor.get("id"),note,id);
            event(id,by,action.equals("APPROVED")?"ARCHIVIST_APPROVE":"ARCHIVIST_REJECT",note);
        } else event(id,by,"REVIEW_"+action,note);
    }
    /** One web submission: review and schedule either both commit, or neither does. */
    @Transactional public void reviewAndSchedule(UUID id,long version,String action,String note,LocalDate pickupOn,String room,boolean confirmed,String by) {
        accounts.actor(by,"ARCHIVIST");
        if("APPROVED".equals(action)) {
            if(!confirmed)throw new BusinessException("Vui lòng tích ô xác nhận phê duyệt cho mượn.");
            if(pickupOn==null)throw new BusinessException("Vui lòng chọn ngày nhận.");
            room=required(room,"Phòng nhận");
        }else if(pickupOn!=null || (room!=null && !room.isBlank()) || confirmed)
            throw new BusinessException("Chỉ nhập lịch nhận khi nhân viên lưu trữ phê duyệt cho mượn.");
        review(id,version,action,note,by);
        if("APPROVED".equals(action))confirm(id,version+1,pickupOn,room,by);
    }
    private void ensureRoute(Map<String,Object> q) {
        if(!Boolean.TRUE.equals(q.get("requires_director")) && Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM reading_requests child JOIN medical_records r ON r.id=child.record_id
                WHERE (child.id=? OR child.batch_id=?) AND r.deceased)
            """,Boolean.class,q.get("id"),q.get("batch_id"))))
            throw new BusinessException("Lượt đăng ký có hồ sơ vừa được cập nhật tử vong. Hủy và đăng ký lại để trình Giám đốc.");
    }
    private void secret(String by,String value,UUID requestId) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",rs->{},"mrm:approval:"+by);
        int failed=jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE actor=? AND action='APPROVAL_VERIFY' AND outcome='FAILURE' AND occurred_at>CURRENT_TIMESTAMP-INTERVAL '15 minutes'",Integer.class,by);
        if(failed>=5)throw new BusinessException("Xác thực cấp 2 tạm khóa sau 5 lần sai; thử lại sau cửa sổ 15 phút.");
        String hash=jdbc.queryForObject("SELECT approval_secret_hash FROM app_users WHERE username=?",String.class,by);
        if(hash==null)throw new BusinessException("Thiết lập mật khẩu cấp 2 trước khi phê duyệt.");
        if(value==null || value.length()>128 || !encoder.matches(value,hash)) {
            audit.failure(by,"APPROVAL_VERIFY","REQUEST",requestId,"Mật khẩu cấp 2 không hợp lệ.");throw new BusinessException("Mật khẩu cấp 2 không đúng.");
        }
    }
    @Transactional public void decide(UUID id,long version,boolean approve,String note,String password,String by) {
        var actor=accounts.actor(by,"DIRECTOR");var q=lock(id);version(q,version);state(q,"PENDING_APPROVAL");note=required(note,"Căn cứ quyết định");
        if(!Boolean.TRUE.equals(q.get("requires_director")))throw new BusinessException("Yêu cầu này thuộc thẩm quyền nhân viên lưu trữ.");
        if(q.get("requester_id").equals(actor.get("id")))throw new BusinessException("Không tự duyệt yêu cầu của mình.");
        secret(by,password,id);if(approve){if(q.get("scheduling_mode").equals("ARCHIVIST"))pickupWindow(q,today());eligible((UUID)q.get("record_id"));}
        jdbc.update("UPDATE reading_requests SET state=?,approver_id=?,decided_at=CURRENT_TIMESTAMP,decision_note=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",approve?"APPROVED":"REJECTED",actor.get("id"),note,id);
        event(id,by,approve?"APPROVE":"REJECT",note+"; xác thực cấp 2 tại thời điểm ghi quyết định.");
    }
    @Transactional public void confirm(UUID id,long version,LocalDate pickupOn,String note,String by) {
        var actor=accounts.actor(by,"ARCHIVIST");var q=lock(id);version(q,version);state(q,"APPROVED");
        note=required(note,"Thông tin lịch/phòng đọc");eligible((UUID)q.get("record_id"));ensureRoute(q);
        boolean legacy=q.get("scheduling_mode").equals("LEGACY");
        if(legacy && pickupOn==null) {
            if(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM reading_sessions WHERE request_id=? AND reading_on<?)",Boolean.class,id,today()))
                throw new BusinessException("Lịch cũ đã qua. Hủy và đăng ký mới theo quy trình hiện tại.");
        } else {
            pickupWindow(q,pickupOn);
            if(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM reading_sessions WHERE request_id=? AND handed_at IS NOT NULL)",Boolean.class,id))throw new BusinessException("Đã giao hồ sơ, không được xếp lại lịch.");
            jdbc.update("DELETE FROM reading_sessions WHERE request_id=?",id);
            jdbc.update("INSERT INTO reading_sessions(request_id,record_id,reading_on,state) VALUES(?,?,?,'REQUESTED')",id,q.get("record_id"),pickupOn);
            jdbc.update("UPDATE reading_requests SET scheduling_mode='ARCHIVIST' WHERE id=?",id);
        }
        if(jdbc.queryForObject("SELECT COUNT(*) FROM reading_sessions WHERE request_id=?",Integer.class,id)==0)throw new BusinessException("Cần chỉ định ngày nhận hồ sơ.");
        if(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM reading_sessions s JOIN reading_sessions x ON x.record_id=s.record_id AND x.reading_on=s.reading_on WHERE s.request_id=? AND x.request_id<>s.request_id AND x.state IN ('CONFIRMED','READING','RETURNED'))",Boolean.class,id))throw new BusinessException("Hồ sơ đã có lịch khác trong ngày được chọn.");
        jdbc.update("UPDATE reading_sessions SET state='CONFIRMED' WHERE request_id=? AND state='REQUESTED'",id);
        jdbc.update("UPDATE reading_requests SET state='CONFIRMED',confirmation_note=?,confirmed_by=?,confirmed_at=CURRENT_TIMESTAMP,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",note,actor.get("id"),id);
        event(id,by,"CONFIRM",note+"; lịch nhận do nhân viên lưu trữ chỉ định; xem ngày nhận tại yêu cầu.");
    }
    @Transactional public void cancel(UUID id,long version,String note,String by) {
        var actor=accounts.actor(by,"READER","ARCHIVIST");var q=lock(id);version(q,version);
        if(actor.get("role").equals("READER") && !q.get("requester_id").equals(actor.get("id")))throw new org.springframework.security.access.AccessDeniedException("Không phải yêu cầu của bạn.");
        if(actor.get("role").equals("READER") && !List.of("SUBMITTED","NEEDS_INFO","PENDING_APPROVAL").contains(q.get("state")))throw new BusinessException("Chỉ được hủy yêu cầu chưa được phê duyệt hoặc từ chối.");
        state(q,"SUBMITTED","NEEDS_INFO","PENDING_APPROVAL","APPROVED","CONFIRMED");note=required(note,"Lý do hủy");
        if(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM reading_sessions WHERE request_id=? AND state='READING')",Boolean.class,id))throw new BusinessException("Phải nhận trả phiên đang đọc trước khi hủy phần còn lại.");
        jdbc.update("UPDATE reading_sessions SET state='CANCELLED' WHERE request_id=? AND state IN ('REQUESTED','CONFIRMED')",id);
        jdbc.update("UPDATE reading_requests SET state='CANCELLED',version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",id);event(id,by,"CANCEL",note);
    }
    @Transactional public void handover(UUID sessionId,long version,String condition,String by) {
        var actor=accounts.actor(by,"ARCHIVIST");var raw=session(sessionId);UUID id=(UUID)raw.get("request_id");var q=lock(id);version(q,version);state(q,"CONFIRMED");if(q.get("scheduling_mode").equals("ARCHIVIST"))pickupWindow(q,today());var s=session(sessionId);
        if(!s.get("state").equals("CONFIRMED") || !((java.sql.Date)s.get("reading_on")).toLocalDate().equals(today()))throw new BusinessException("Chỉ giao phiên đã xác nhận của hôm nay.");
        accounts.actor(jdbc.queryForObject("SELECT username FROM app_users WHERE id=?",String.class,q.get("requester_id")),"READER");
        eligible((UUID)q.get("record_id"));ensureRoute(q);condition=required(condition,"Tình trạng khi giao");if(condition.length()>1000)throw new BusinessException("Tình trạng tối đa 1000 ký tự.");
        String before=records.snapshotJson((UUID)q.get("record_id"));
        jdbc.update("UPDATE reading_sessions SET state='READING',handed_at=CURRENT_TIMESTAMP,handed_by=?,received_by=?,hand_condition=? WHERE id=?",actor.get("id"),q.get("requester_id"),condition,sessionId);
        jdbc.update("UPDATE medical_records SET physical_status='READING',version=version+1,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",actor.get("id"),q.get("record_id"));
        jdbc.update("UPDATE reading_requests SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",id);
        records.event((UUID)q.get("record_id"),"HANDOVER",by,"Giao tại phòng đọc; phiên "+sessionId,before,records.snapshotJson((UUID)q.get("record_id")));
        event(id,by,"HANDOVER","Phiên "+sessionId+"; tình trạng: "+condition);
    }
    @Transactional public void receive(UUID sessionId,long version,UUID destination,String quality,String condition,String by) {
        var actor=accounts.actor(by,"ARCHIVIST");var s=session(sessionId);UUID id=(UUID)s.get("request_id");var q=lock(id);version(q,version);s=session(sessionId);
        if(!s.get("state").equals("READING"))throw new BusinessException("Phiên không có hồ sơ đang giao đọc.");
        records.lock((UUID)q.get("record_id"));var old=records.details((UUID)q.get("record_id"));
        if(!old.physicalStatus().equals("READING"))throw new BusinessException("Trạng thái hồ sơ không khớp, cần đối soát.");
        condition=required(condition,"Tình trạng nhận trả");if(condition.length()>1000)throw new BusinessException("Tình trạng tối đa 1000 ký tự.");
        if(!List.of("NORMAL","DAMAGED","INCOMPLETE").contains(quality))throw new BusinessException("Chất lượng không hợp lệ.");
        boolean late=!((java.sql.Date)s.get("reading_on")).toLocalDate().equals(today());
        if(late && s.get("exception_note")==null)throw new BusinessException("Trả khác ngày phải ghi ngoại lệ trước, không tự sửa thời điểm trả.");
        storage.lockTree();var target=storage.requireActive(destination,true);
        if(!destination.equals(old.form().locationId()) && target.capacity()!=null && target.occupied()>=target.capacity())throw new BusinessException("Ngăn trả đã đầy.");
        String before=records.snapshotJson((UUID)q.get("record_id"));
        jdbc.update("UPDATE reading_sessions SET state='RETURNED',returned_at=CURRENT_TIMESTAMP,returned_by=?,return_condition=?,return_location_id=? WHERE id=?",actor.get("id"),condition,destination,sessionId);
        jdbc.update("UPDATE medical_records SET physical_status=?,quality_status=?,current_location_id=?,version=version+1,updated_by=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",quality.equals("NORMAL")?"IN_STORAGE":"UNAVAILABLE",quality,destination,actor.get("id"),q.get("record_id"));
        if(!destination.equals(old.form().locationId()))records.locationHistory((UUID)q.get("record_id"),old.form().locationId(),destination,storage.get(old.form().locationId()).path(),target.path(),by,"Cất lại sau đọc; xem tình trạng trong phiên "+sessionId,UUID.randomUUID());
        jdbc.update("UPDATE reading_requests SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",id);
        records.event((UUID)q.get("record_id"),"RETURN",by,"Nhận trả phiên "+sessionId,before,records.snapshotJson((UUID)q.get("record_id")));
        event(id,by,"RETURN","Phiên "+sessionId+"; chất lượng "+quality+"; tình trạng: "+condition+(late?"; trả khác ngày, đã có ngoại lệ":""));
    }
    @Transactional public void exception(UUID sessionId,long version,String note,boolean noShow,String by) {
        accounts.actor(by,"ARCHIVIST");var s=session(sessionId);UUID id=(UUID)s.get("request_id");var q=lock(id);version(q,version);
        if(!List.of("CONFIRMED","READING").contains(s.get("state")))throw new BusinessException("Chỉ ghi ngoại lệ phiên xác nhận/đang đọc.");
        if(((java.sql.Date)s.get("reading_on")).toLocalDate().isAfter(today()))throw new BusinessException("Chưa đến ngày đọc.");
        if(noShow && !s.get("state").equals("CONFIRMED"))throw new BusinessException("Không đánh dấu vắng khi đã giao hồ sơ.");
        note=required(note,"Nội dung ngoại lệ");
        jdbc.update("UPDATE reading_sessions SET state=CASE WHEN ? THEN 'NO_SHOW' ELSE state END,exception_note=?,exception_at=CURRENT_TIMESTAMP WHERE id=?",noShow,note,sessionId);
        jdbc.update("UPDATE reading_requests SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",id);event(id,by,noShow?"NO_SHOW":"EXCEPTION","Phiên "+sessionId+": "+note);
    }
    private Map<String,Object> session(UUID id) {
        var rows=jdbc.queryForList("SELECT * FROM reading_sessions WHERE id=?",id);if(rows.isEmpty())throw new BusinessException("Không có phiên đọc.");return rows.getFirst();
    }
}
