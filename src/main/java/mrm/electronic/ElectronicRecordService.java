package mrm.electronic;

import java.util.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import mrm.account.AccountService;
import mrm.audit.AuditService;
import mrm.common.BusinessException;
import mrm.notification.NotificationService;

@Service
public class ElectronicRecordService {
    private final JdbcTemplate jdbc; private final AccountService accounts;
    private final AuditService audit; private final NotificationService notifications;
    public ElectronicRecordService(JdbcTemplate jdbc,AccountService accounts,AuditService audit,NotificationService notifications){this.jdbc=jdbc;this.accounts=accounts;this.audit=audit;this.notifications=notifications;}
    private static String required(String text){if(text==null || text.isBlank() || text.strip().length()>2000)throw new BusinessException("Cần nội dung xử lý từ 1–2000 ký tự.");return text.strip();}
    private static LocalDate birthDate(ElectronicRecordForm f){
        if(f.birthYear()==null || f.birthMonth()==null || f.birthDay()==null)throw new BusinessException("Cần nhập đầy đủ ngày, tháng, năm sinh.");
        try{return LocalDate.of(f.birthYear(),f.birthMonth(),f.birthDay());}
        catch(DateTimeException ex){throw new BusinessException("Ngày sinh không hợp lệ. Kiểm tra ngày, tháng và năm (bao gồm năm nhuận).");}
    }
    private static void validate(ElectronicRecordForm f){
        if(f==null || f.token()==null || f.version()==null || f.version()<0 || f.patientName()==null || f.patientName().isBlank() || f.patientName().length()>200
            || f.birthYear()==null || f.birthYear()<1800 || f.birthYear()>LocalDate.now(ZoneId.of("Asia/Bangkok")).getYear()
            || f.sex()==null || !Set.of("MALE","FEMALE","UNKNOWN").contains(f.sex()) || (f.patientCode()!=null && f.patientCode().length()>64))throw new BusinessException("Thông tin người bệnh không hợp lệ; kiểm tra họ tên, năm sinh và giới tính.");
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Bangkok"));
        LocalDate birth=birthDate(f);
        if(birth.isAfter(today))throw new BusinessException("Ngày sinh không được ở tương lai.");
        if((f.admissionDate()!=null && birth.isAfter(f.admissionDate())) || (f.dischargeDate()!=null && birth.isAfter(f.dischargeDate())))throw new BusinessException("Ngày sinh không được sau ngày điều trị.");
        if((f.admissionDate()!=null && f.admissionDate().isAfter(today)) || (f.dischargeDate()!=null && f.dischargeDate().isAfter(today)))throw new BusinessException("Ngày điều trị không được ở tương lai.");
        if(f.admissionDate()!=null && f.dischargeDate()!=null && f.dischargeDate().isBefore(f.admissionDate()))throw new BusinessException("Ngày ra viện phải từ ngày vào viện trở đi.");
        if(f.admissionDate()!=null && f.birthYear()>f.admissionDate().getYear())throw new BusinessException("Năm sinh không được sau năm vào viện.");
        if((f.sourceNote()!=null && f.sourceNote().length()>1000) || (f.note()!=null && f.note().length()>2000))throw new BusinessException("Nguồn dữ liệu tối đa 1000 ký tự, ghi chú tối đa 2000 ký tự.");
    }
    private void writeDetails(UUID id,ElectronicRecordForm f){
        String department=null,type=null;
        if(f.departmentId()!=null){var rows=jdbc.queryForList("SELECT name FROM departments WHERE id=?",String.class,f.departmentId());if(rows.isEmpty())throw new BusinessException("Khoa không tồn tại.");department=rows.getFirst();}
        if(f.recordTypeId()!=null){var rows=jdbc.queryForList("SELECT name FROM record_types WHERE id=? AND active",String.class,f.recordTypeId());if(rows.isEmpty())throw new BusinessException("Loại bệnh án không tồn tại hoặc đã ngừng dùng.");type=rows.getFirst();}
        jdbc.update("UPDATE medical_records SET department_id=?,record_type_id=?,department_name_snapshot=?,record_type_name_snapshot=?,deceased=?,admission_date=?,discharge_date=?,source_note=?,note=? WHERE id=?",f.departmentId(),f.recordTypeId(),department,type,f.deceased(),f.admissionDate(),f.dischargeDate(),f.sourceNote(),f.note(),id);
    }
    public record Page(List<Map<String,Object>> items,long total,int page,String query,String state){public long pages(){return Math.max(1,(total+9)/10);}}
    public Page list(String by,String query,String state,int page){
        var actor=accounts.actor(by,"READER","ARCHIVIST","DIRECTOR");
        query=query==null?"":query.strip();state=state==null?"":state;
        if(query.length()>200 || (!state.isEmpty() && !Set.of("SUBMITTED","NEEDS_INFO","PENDING_APPROVAL","APPROVED","REJECTED","CANCELLED").contains(state)))throw new BusinessException("Bộ lọc không hợp lệ.");
        String pattern="%"+query.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        String from=" FROM electronic_records e JOIN app_users u ON u.id=e.created_by WHERE (?<>'READER' OR e.created_by=?) AND (?='' OR e.state=?) AND (mrm_search_key(e.patient_name) LIKE mrm_search_key(?) ESCAPE '!' OR e.record_code ILIKE ? ESCAPE '!')";
        var args=new ArrayList<Object>(Arrays.asList(actor.get("role"),actor.get("id"),state,state,pattern,pattern));
        long total=jdbc.queryForObject("SELECT COUNT(*)"+from,Long.class,args.toArray());
        int current=(int)Math.min(Math.max(page,0),Math.max(0,(total-1)/10));args.add(current*10);
        return new Page(jdbc.queryForList("SELECT e.*,u.username AS creator"+from+" ORDER BY e.created_at DESC,e.id LIMIT 10 OFFSET ?",args.toArray()),total,current,query,state);
    }
    public Map<String,Object> detail(UUID id,String by){
        var actor=accounts.actor(by,"READER","ARCHIVIST","DIRECTOR");
        var rows=jdbc.queryForList("SELECT e.*,u.username AS creator FROM electronic_records e JOIN app_users u ON u.id=e.created_by WHERE e.id=? AND (?<>'READER' OR e.created_by=?)",id,actor.get("role"),actor.get("id"));
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy hồ sơ.");
        var row=rows.getFirst();row.put("events",jdbc.queryForList("SELECT e.*,COALESCE(NULLIF(employee.full_name,''),u.display_name,e.actor) AS actor_name FROM electronic_record_events e LEFT JOIN app_users u ON u.username=e.actor LEFT JOIN employees employee ON employee.account_id=u.id WHERE e.record_id=? ORDER BY e.occurred_at,e.id",id));return row;
    }
    private Map<String,Object> lock(UUID id,long version){
        var rows=jdbc.queryForList("SELECT *,submission_state AS state FROM medical_records WHERE id=? AND submission_state IS NOT NULL FOR UPDATE",id);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy hồ sơ.");
        var row=rows.getFirst();if(((Number)row.get("version")).longValue()!=version)throw new BusinessException("Hồ sơ đã thay đổi. Tải lại trước khi xử lý.");return row;
    }
    private static void state(Map<String,Object> row,String... allowed){if(!Arrays.asList(allowed).contains(row.get("state")))throw new BusinessException("Không thể thực hiện ở trạng thái hiện tại.");}
    private void event(UUID id,String by,String action,String details){
        UUID event=UUID.randomUUID();
        jdbc.update("INSERT INTO electronic_record_events(id,record_id,actor,action,details,version,snapshot) SELECT ?,id,?,?,?,version,to_jsonb(e) FROM electronic_records e WHERE id=?",event,by,action,details,id);
        audit.log(by,action,"ELECTRONIC_RECORD",id,details);notifications.forElectronic(id,event,action);
    }
    @Transactional public UUID create(ElectronicRecordForm f,String by){
        var actor=accounts.actor(by,"READER");validate(f);
        String fingerprint;
        try{fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(f.toString().getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",rs->{},"mrm:electronic:"+f.token());
        var existing=jdbc.queryForList("SELECT id,created_by,create_fingerprint FROM electronic_records WHERE create_token=?",f.token());
        if(!existing.isEmpty()){
            var old=existing.getFirst();if(!old.get("created_by").equals(actor.get("id")) || !old.get("create_fingerprint").equals(fingerprint))throw new BusinessException("Phiếu đã được sử dụng với nội dung khác.");return (UUID)old.get("id");
        }
        int year=LocalDate.now(ZoneId.of("Asia/Bangkok")).getYear();
        String code;
        do {
            long sequence=jdbc.queryForObject("INSERT INTO record_code_counters(year,last_value) VALUES(?,1) ON CONFLICT(year) DO UPDATE SET last_value=record_code_counters.last_value+1 RETURNING last_value",Long.class,year);
            code="MRM-REC-"+year+"-"+String.format(Locale.ROOT,"%06d",sequence);
        }while(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM medical_records WHERE record_code=?)",Boolean.class,code)));
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO medical_records(id,create_token,create_fingerprint,record_code,medical_record_number,patient_name,birth_year,birth_date,sex,patient_code,record_state,physical_status,quality_status,has_paper_copy,has_electronic_copy,submission_state,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,'DRAFT','NOT_APPLICABLE','NORMAL',FALSE,TRUE,'SUBMITTED',?,?)",id,f.token(),fingerprint,code,code,f.patientName(),f.birthYear(),birthDate(f),f.sex(),f.patientCode(),actor.get("id"),actor.get("id"));
        writeDetails(id,f);
        event(id,by,"SUBMIT","Bác sĩ khai báo hồ sơ điện tử; chờ nhân viên lưu trữ kiểm tra.");return id;
    }
    @Transactional public void revise(UUID id,ElectronicRecordForm f,String by){
        var actor=accounts.actor(by,"READER");validate(f);var row=lock(id,f.version());
        if(!row.get("created_by").equals(actor.get("id")))throw new AccessDeniedException("Không phải hồ sơ của bạn.");state(row,"SUBMITTED","NEEDS_INFO");
        jdbc.update("UPDATE medical_records SET patient_name=?,birth_year=?,birth_date=?,sex=?,patient_code=?,submission_state='SUBMITTED',reviewer_id=NULL,reviewed_at=NULL,review_note=NULL,updated_by=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",f.patientName(),f.birthYear(),birthDate(f),f.sex(),f.patientCode(),actor.get("id"),id);
        writeDetails(id,f);
        event(id,by,"AMEND","Bác sĩ cập nhật và gửi lại; giữ nguyên mã hồ sơ.");
    }
    @Transactional public void review(UUID id,long version,boolean ready,String note,String by){
        var actor=accounts.actor(by,"ARCHIVIST");var row=lock(id,version);state(row,"SUBMITTED");note=required(note);
        jdbc.update("UPDATE medical_records SET submission_state=?,reviewer_id=?,reviewed_at=CURRENT_TIMESTAMP,review_note=?,updated_by=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",ready?"PENDING_APPROVAL":"NEEDS_INFO",actor.get("id"),note,actor.get("id"),id);
        event(id,by,ready?"REVIEW_PENDING_APPROVAL":"REVIEW_NEEDS_INFO",note);
    }
    @Transactional public void decide(UUID id,long version,boolean approve,String note,String secret,String by){
        var actor=accounts.actor(by,"DIRECTOR");var row=lock(id,version);state(row,"PENDING_APPROVAL");note=required(note);
        if(row.get("created_by").equals(actor.get("id")))throw new AccessDeniedException("Không tự duyệt hồ sơ của mình.");
        accounts.verifyApprovalSecret(by,secret,id);
        jdbc.update("UPDATE medical_records SET submission_state=?,record_state=CASE WHEN ? THEN 'PUBLISHED' ELSE 'DRAFT' END,approver_id=?,decided_at=CURRENT_TIMESTAMP,decision_note=?,published_at=CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END,updated_by=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",approve?"APPROVED":"REJECTED",approve,actor.get("id"),note,approve,actor.get("id"),id);
        event(id,by,approve?"APPROVE":"REJECT",note);
    }
    @Transactional public void cancel(UUID id,long version,String by){
        var actor=accounts.actor(by,"READER");var row=lock(id,version);if(!row.get("created_by").equals(actor.get("id")))throw new AccessDeniedException("Không phải hồ sơ của bạn.");state(row,"SUBMITTED","NEEDS_INFO","PENDING_APPROVAL");
        jdbc.update("UPDATE medical_records SET submission_state='CANCELLED',updated_by=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",actor.get("id"),id);event(id,by,"CANCEL","Bác sĩ hủy trước quyết định của Giám đốc.");
    }
}
