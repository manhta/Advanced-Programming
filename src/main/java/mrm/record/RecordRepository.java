package mrm.record;

import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import mrm.catalog.CatalogSnapshot;
import mrm.common.BusinessException;

@Repository
public class RecordRepository {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    public RecordRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; this.named=new NamedParameterJdbcTemplate(jdbc); }

    private static final String BASIC_COLUMNS="""
            r.id, r.archive_number, r.medical_record_number, r.patient_code, r.patient_name, r.birth_year, r.birth_date, r.sex,
            coalesce(r.department_name_snapshot,d.name) AS department_name,
            coalesce(r.record_type_name_snapshot,t.name) AS type_name,
            r.admission_date,r.discharge_date,r.deceased,r.record_state,r.physical_status,
            r.record_code,r.has_paper_copy,r.has_electronic_copy,r.submission_state,
            (r.has_paper_copy AND r.record_state='STORED' AND r.physical_status='IN_STORAGE' AND r.quality_status='NORMAL' AND
            """+BorrowAvailability.NO_ACTIVE_REQUEST.replace("mr.","r.")+"""
            ) AS borrowable,
            CASE WHEN r.physical_status='READING' OR EXISTS(SELECT 1 FROM reading_sessions bs WHERE bs.record_id=r.id AND bs.state='READING') THEN 'BORROWED'
                WHEN EXISTS(SELECT 1 FROM reading_requests br WHERE br.record_id=r.id AND br.state='CONFIRMED'
                    AND (NOT EXISTS(SELECT 1 FROM reading_sessions bs WHERE bs.request_id=br.id)
                        OR EXISTS(SELECT 1 FROM reading_sessions bs WHERE bs.request_id=br.id AND bs.state IN ('REQUESTED','CONFIRMED')))) THEN 'SCHEDULED'
                WHEN NOT (
            """+BorrowAvailability.NO_ACTIVE_REQUEST.replace("mr.","r.")+"""
                ) THEN 'RESERVED' ELSE 'UNAVAILABLE' END AS borrowing_status
            """;
    private static final String JOINS=" FROM medical_records r LEFT JOIN departments d ON d.id=r.department_id LEFT JOIN record_types t ON t.id=r.record_type_id ";
    private static final RowMapper<RecordSummary> BASIC=(rs,n) -> new RecordSummary(rs.getObject("id",UUID.class),rs.getString("archive_number"),
            rs.getString("medical_record_number"),rs.getString("patient_code"),rs.getString("patient_name"),rs.getObject("birth_year",Integer.class),
            rs.getString("sex"),rs.getString("department_name"),rs.getString("type_name"),rs.getObject("admission_date",LocalDate.class),
            rs.getObject("discharge_date",LocalDate.class),rs.getBoolean("deceased"),rs.getString("record_state"),rs.getString("physical_status"),rs.getString("record_code"),rs.getBoolean("has_paper_copy"),rs.getBoolean("has_electronic_copy"),rs.getString("submission_state"),rs.getBoolean("borrowable"),rs.getString("borrowing_status"),rs.getObject("birth_date",LocalDate.class));

    public RecordPage search(RecordFilter f,boolean archivist){return search(f,archivist,null,null);}
    private Map<String,Object> actor(String username){
        var rows=jdbc.queryForList("SELECT u.id,r.code AS role FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.username=? AND u.status='ACTIVE' AND r.code IN ('READER','ARCHIVIST','DIRECTOR')",username);
        if(rows.isEmpty())throw new org.springframework.security.access.AccessDeniedException("Không có quyền xem bệnh án.");return rows.getFirst();
    }
    public RecordPage search(RecordFilter f,String username){var a=actor(username);return search(f,a.get("role").equals("ARCHIVIST"),a.get("id"),(String)a.get("role"));}
    public Map<String,Object> identity(UUID id,String username){
        var a=actor(username);
        var rows=jdbc.queryForList("SELECT r.*,r.created_by=? AS owned FROM medical_records r WHERE r.id=? AND (r.record_state IN ('STORED','PUBLISHED') OR ?='ARCHIVIST' OR (?='DIRECTOR' AND r.submission_state IS NOT NULL) OR (r.created_by=? AND r.submission_state IS NOT NULL))",a.get("id"),id,a.get("role"),a.get("role"),a.get("id"));
        if(rows.isEmpty())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Không tìm thấy hồ sơ.");return rows.getFirst();
    }
    private RecordPage search(RecordFilter f,boolean archivist,Object actorId,String role) {
        if(f.page()<0 || f.page()>1000000 || f.size()<1 || f.size()>100 || f.q().length()>200) throw new BusinessException("Bộ lọc/phân trang không hợp lệ (1–100 dòng/trang).");
        if(f.from()!=null && f.to()!=null && f.from().isAfter(f.to())) throw new BusinessException("Khoảng ngày ra viện bị đảo.");
        if(f.birthYear()!=null && (f.birthYear()<1800 || f.birthYear()>9999)) throw new BusinessException("Năm sinh cần nằm trong khoảng 1800–9999.");
        var args=new MapSqlParameterSource(); var where=new StringBuilder(" WHERE TRUE ");
        if(!archivist) {
            if(actorId==null)where.append(" AND r.record_state IN ('STORED','PUBLISHED') ");
            else {where.append(" AND (r.record_state IN ('STORED','PUBLISHED') OR (r.submission_state IS NOT NULL AND (r.created_by=:actorId OR :role='DIRECTOR'))) ");args.addValue("actorId",actorId).addValue("role",role);}
        }
        if(actorId!=null || role!=null){
            switch(f.scope()){
                case "OFFICIAL" -> where.append(" AND r.record_state IN ('STORED','PUBLISHED') ");
                case "DECLARATIONS" -> {where.append(" AND r.submission_state IS NOT NULL ");if("READER".equals(role)){where.append(" AND r.created_by=:actorId ");args.addValue("actorId",actorId);}}
                case "DRAFTS" -> {if(!archivist)throw new org.springframework.security.access.AccessDeniedException("Chỉ nhân viên lưu trữ được xem nháp bản giấy.");where.append(" AND r.record_state='DRAFT' AND r.submission_state IS NULL ");}
                default -> throw new BusinessException("Danh sách hồ sơ không hợp lệ.");
            }
        }
        if(!f.format().isBlank()) {
            switch(f.format()){case "PAPER" -> where.append(" AND r.has_paper_copy ");case "PAPER_ONLY" -> where.append(" AND r.has_paper_copy AND NOT r.has_electronic_copy ");case "BOTH" -> where.append(" AND r.has_paper_copy AND r.has_electronic_copy ");case "ELECTRONIC_ONLY" -> where.append(" AND NOT r.has_paper_copy AND r.has_electronic_copy ");default -> throw new BusinessException("Hình thức hồ sơ không hợp lệ.");}
        }
        if(!f.submissionState().isBlank()) {
            if(!List.of("SUBMITTED","NEEDS_INFO","PENDING_APPROVAL","APPROVED","REJECTED","CANCELLED").contains(f.submissionState()))throw new BusinessException("Trạng thái xét duyệt không hợp lệ.");
            where.append(" AND r.submission_state=:submissionState ");args.addValue("submissionState",f.submissionState());
        }
        if(archivist && f.state()!=null && !f.state().isBlank()) {
            if(!List.of("DRAFT","STORED","PUBLISHED").contains(f.state())) throw new BusinessException("Trạng thái hồ sơ không hợp lệ.");
            where.append(" AND r.record_state=:state "); args.addValue("state",f.state());
        }
        if(!f.q().isEmpty()) {
            where.append(" AND (upper(r.record_code)=upper(:exact) OR upper(r.archive_number)=upper(:exact) OR upper(r.medical_record_number)=upper(:exact) OR upper(r.patient_code)=upper(:exact) OR mrm_search_key(r.patient_name) LIKE mrm_search_key(:name) ESCAPE '!') ");
            args.addValue("exact",f.q()).addValue("name","%"+f.q().replace("!","!!").replace("%","!%").replace("_","!_")+"%");
        }
        if(f.departmentId()!=null) { where.append(" AND r.department_id=:department "); args.addValue("department",f.departmentId()); }
        if(f.recordTypeId()!=null) { where.append(" AND r.record_type_id=:type "); args.addValue("type",f.recordTypeId()); }
        if(f.birthYear()!=null) { where.append(" AND r.birth_year=:birthYear "); args.addValue("birthYear",f.birthYear()); }
        if(f.from()!=null) { where.append(" AND r.discharge_date>=:fromDate "); args.addValue("fromDate",f.from()); }
        if(f.to()!=null) { where.append(" AND r.discharge_date<=:toDate "); args.addValue("toDate",f.to()); }
        if(f.physicalStatus()!=null && !f.physicalStatus().isBlank()) {
            if(!List.of("NOT_STORED","IN_STORAGE","UNAVAILABLE","READING").contains(f.physicalStatus())) throw new BusinessException("Trạng thái vật lý không hợp lệ.");
            where.append(" AND r.physical_status=:physical "); args.addValue("physical",f.physicalStatus());
        }
        String order=switch(f.sort()) { case "name" -> "mrm_search_key(r.patient_name),r.id"; case "archive" -> "coalesce(r.archive_number,r.record_code),r.id";
            case "discharge" -> "r.discharge_date DESC NULLS LAST,r.id"; case "newest" -> "r.created_at DESC,r.id";
            default -> throw new BusinessException("Cách sắp xếp không hợp lệ."); };
        long total=Objects.requireNonNull(named.queryForObject("SELECT COUNT(*)"+JOINS+where,args,Long.class));
        args.addValue("limit",f.size()).addValue("offset",(long)f.page()*f.size());
        var items=named.query("SELECT "+BASIC_COLUMNS+JOINS+where+" ORDER BY "+order+" LIMIT :limit OFFSET :offset",args,BASIC);
        return new RecordPage(items,total,f.page(),f.size());
    }
    public RecordSummary summary(UUID id,boolean archivist) {
        var items=jdbc.query("SELECT "+BASIC_COLUMNS+JOINS+" WHERE r.id=?"+(archivist ? "" : " AND r.record_state IN ('STORED','PUBLISHED')"),BASIC,id);
        if(items.isEmpty()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Không tìm thấy hồ sơ.");
        return items.getFirst();
    }
    public RecordDetails details(UUID id) {
        var policies=jdbc.query("SELECT * FROM medical_record_policies WHERE record_id=? ORDER BY code_snapshot,version_snapshot",
                (rs,n) -> new CatalogSnapshot.PolicySnapshot(rs.getObject("policy_id",UUID.class),rs.getString("code_snapshot"),rs.getString("name_snapshot"),
                rs.getInt("version_snapshot"),rs.getInt("years_snapshot"),rs.getObject("effective_from_snapshot",LocalDate.class),rs.getObject("effective_to_snapshot",LocalDate.class),rs.getString("basis_snapshot")),id);
        var rows=jdbc.query("""
                SELECT r.*, creator.username AS creator_name, updater.username AS updater_name,
                coalesce(r.department_name_snapshot,d.name) AS dept_name, coalesce(r.record_type_name_snapshot,t.name) AS type_name
                FROM medical_records r JOIN app_users creator ON creator.id=r.created_by JOIN app_users updater ON updater.id=r.updated_by
                LEFT JOIN departments d ON d.id=r.department_id LEFT JOIN record_types t ON t.id=r.record_type_id WHERE r.id=?
                """, (rs,n) -> {
            List<UUID> policyIds=policies.stream().map(CatalogSnapshot.PolicySnapshot::id).toList();
            if(rs.getString("record_state").equals("DRAFT")) policyIds=Arrays.asList((UUID[])rs.getArray("draft_policy_ids").getArray());
            var form=new RecordForm(rs.getObject("create_token",UUID.class),rs.getLong("version"),rs.getString("archive_number"),
                    rs.getString("medical_record_number"),rs.getString("patient_code"),rs.getString("patient_name"),rs.getObject("birth_year",Integer.class),
                    rs.getString("sex"),rs.getObject("department_id",UUID.class),rs.getObject("record_type_id",UUID.class),rs.getBoolean("deceased"),
                    rs.getObject("admission_date",LocalDate.class),rs.getObject("discharge_date",LocalDate.class),rs.getObject("first_stored_on",LocalDate.class),
                    policyIds,rs.getObject("current_location_id",UUID.class),rs.getString("quality_status"),rs.getString("source_note"),rs.getString("note"),null);
            return new RecordDetails(id,form,rs.getString("record_state"),rs.getString("physical_status"),rs.getString("dept_name"),rs.getString("type_name"),
                    null,rs.getObject("retention_years_snapshot",Integer.class),rs.getObject("retention_due_on",LocalDate.class),policies,
                    rs.getString("creator_name"),rs.getTimestamp("created_at").toInstant(),rs.getString("updater_name"),rs.getTimestamp("updated_at").toInstant());
        },id);
        if(rows.isEmpty()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Không tìm thấy hồ sơ.");
        return rows.getFirst();
    }
    public void lock(UUID id) {
        if(jdbc.queryForList("SELECT id FROM medical_records WHERE id=? FOR UPDATE",UUID.class,id).isEmpty()) throw new BusinessException("Hồ sơ không tồn tại.");
    }
    public UUID previousCreate(UUID token,String fingerprint,String username) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", rs -> { },"mrm:create:"+token);
        var rows=jdbc.queryForList("SELECT r.id,r.create_fingerprint,u.username FROM medical_records r JOIN app_users u ON u.id=r.created_by WHERE create_token=?",token);
        if(rows.isEmpty()) return null;
        var previous=rows.getFirst();
        if(!username.equals(previous.get("username")) || !fingerprint.equals(previous.get("create_fingerprint"))) throw new BusinessException("Phiếu nhập đã được dùng với nội dung khác. Hãy mở form mới.");
        return (UUID)previous.get("id");
    }
    public boolean suspectedDuplicate(UUID id,RecordForm f) {
        if(f.medicalRecordNumber()==null || f.admissionDate()==null || f.dischargeDate()==null) return false;
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM medical_records WHERE record_state='STORED' AND id<>?
                AND medical_record_number=? AND patient_code IS NOT DISTINCT FROM ? AND admission_date=? AND discharge_date=?)
                """,Boolean.class,id,f.medicalRecordNumber(),f.patientCode(),f.admissionDate(),f.dischargeDate()));
    }
    public String nextArchive(LocalDate date) {
        String period=String.format(java.util.Locale.ROOT,"%04d-%02d",date.getYear(),date.getMonthValue());
        while(true) {
            int value=Objects.requireNonNull(jdbc.queryForObject("""
                    INSERT INTO archive_counters(period,last_value) VALUES(?,1)
                    ON CONFLICT(period) DO UPDATE SET last_value=archive_counters.last_value+1 RETURNING last_value
                    """,Integer.class,period));
            String number=period+"-"+String.format(java.util.Locale.ROOT,"%06d",value);
            if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM medical_records WHERE archive_number=?)",Boolean.class,number))) return number;
        }
    }
    public void write(UUID id,RecordForm f,String archive,String state,String physical,CatalogSnapshot snapshot,String username,boolean insert,String fingerprint) {
        var p=new MapSqlParameterSource().addValue("id",id).addValue("token",f.operationKey()).addValue("fingerprint",fingerprint)
                .addValue("archive",archive).addValue("medical",f.medicalRecordNumber()).addValue("patientCode",f.patientCode()).addValue("patientName",f.patientName())
                .addValue("birth",f.birthYear()).addValue("sex",f.sex()).addValue("department",f.departmentId()).addValue("type",f.recordTypeId())
                .addValue("deceased",f.deceased()).addValue("admission",f.admissionDate()).addValue("discharge",f.dischargeDate()).addValue("stored",f.firstStoredOn())
                .addValue("location",state.equals("STORED") ? f.locationId() : null).addValue("quality",f.qualityStatus()).addValue("source",f.sourceNote())
                .addValue("note",f.note()).addValue("state",state).addValue("physical",physical).addValue("username",username).addValue("version",f.version())
                .addValue("deptSnapshot",snapshot==null ? null : snapshot.departmentName()).addValue("typeCode",snapshot==null ? null : snapshot.recordTypeCode())
                .addValue("typeName",snapshot==null ? null : snapshot.recordTypeName()).addValue("years",snapshot==null ? null : snapshot.retentionYears())
                .addValue("due",snapshot==null ? null : snapshot.dueOn()).addValue("draftPolicies","{"+String.join(",",f.policyIds().stream().map(UUID::toString).toList())+"}");
        if(insert) named.update("""
                INSERT INTO medical_records(id,create_token,create_fingerprint,archive_number,medical_record_number,patient_code,patient_name,birth_year,sex,
                department_id,record_type_id,deceased,admission_date,discharge_date,first_stored_on,current_location_id,quality_status,source_note,note,record_state,physical_status,
                department_name_snapshot,record_type_code_snapshot,record_type_name_snapshot,retention_years_snapshot,retention_due_on,draft_policy_ids,created_by,updated_by)
                VALUES(:id,:token,:fingerprint,:archive,:medical,:patientCode,:patientName,:birth,:sex,:department,:type,:deceased,:admission,:discharge,:stored,:location,:quality,:source,:note,:state,:physical,
                :deptSnapshot,:typeCode,:typeName,:years,:due,CAST(:draftPolicies AS UUID[]),(SELECT id FROM app_users WHERE username=:username),(SELECT id FROM app_users WHERE username=:username))
                """,p);
        else if(named.update("""
                UPDATE medical_records SET archive_number=:archive,medical_record_number=:medical,patient_code=:patientCode,patient_name=:patientName,birth_year=:birth,sex=:sex,
                department_id=:department,record_type_id=:type,deceased=:deceased,admission_date=:admission,discharge_date=:discharge,first_stored_on=:stored,
                current_location_id=:location,has_paper_copy=TRUE,has_electronic_copy=TRUE,quality_status=:quality,source_note=:source,note=:note,record_state=:state,physical_status=:physical,
                department_name_snapshot=:deptSnapshot,record_type_code_snapshot=:typeCode,record_type_name_snapshot=:typeName,retention_years_snapshot=:years,
                retention_due_on=:due,draft_policy_ids=CAST(:draftPolicies AS UUID[]),updated_by=(SELECT id FROM app_users WHERE username=:username),updated_at=CURRENT_TIMESTAMP,version=version+1
                WHERE id=:id AND version=:version
                """,p)!=1) throw new BusinessException("Hồ sơ đã thay đổi. Vui lòng tải lại.");
        if(snapshot!=null) {
            jdbc.update("DELETE FROM medical_record_policies WHERE record_id=?",id);
            for(var policy:snapshot.policies()) jdbc.update("""
                    INSERT INTO medical_record_policies(record_id,policy_id,code_snapshot,name_snapshot,version_snapshot,years_snapshot,effective_from_snapshot,effective_to_snapshot,basis_snapshot)
                    VALUES(?,?,?,?,?,?,?,?,?)
                    """,id,policy.id(),policy.code(),policy.name(),policy.version(),policy.years(),policy.effectiveFrom(),policy.effectiveTo(),policy.basis());
        }
    }
    public String snapshotJson(UUID id) {
        return jdbc.queryForObject("""
                SELECT jsonb_build_object('record',to_jsonb(r),'policies',coalesce((SELECT jsonb_agg(to_jsonb(p)) FROM medical_record_policies p WHERE p.record_id=r.id),'[]'::jsonb))::TEXT
                FROM medical_records r WHERE id=?
                """,String.class,id);
    }
    public void event(UUID id,String action,String username,String reason,String before,String after) {
        jdbc.update("INSERT INTO record_events(record_id,action,actor_username,reason,before_data,after_data) VALUES(?,?,?,?,CAST(? AS JSONB),CAST(? AS JSONB))",id,action,username,reason,before,after);
    }
    public void locationHistory(UUID id,UUID from,UUID to,String oldPath,String newPath,String username,String reason,UUID operationKey) {
        jdbc.update("INSERT INTO record_location_history(record_id,from_location_id,to_location_id,from_path,to_path,actor_username,reason,operation_key) VALUES(?,?,?,?,?,?,?,?)",id,from,to,oldPath,newPath,username,reason,operationKey);
    }
    public List<Map<String,Object>> history(UUID id) { return jdbc.queryForList("SELECT action,actor_username,reason,before_data::TEXT,after_data::TEXT,occurred_at FROM record_events WHERE record_id=? AND action<>'VIEW' ORDER BY occurred_at,id",id); }
    public List<Map<String,Object>> locationHistory(UUID id) { return jdbc.queryForList("SELECT from_path,to_path,actor_username,reason,occurred_at FROM record_location_history WHERE record_id=? ORDER BY occurred_at,id",id); }
    public void move(UUID id,UUID location,String username,long version) {
        if(jdbc.update("UPDATE medical_records SET current_location_id=?,version=version+1,updated_by=(SELECT id FROM app_users WHERE username=?),updated_at=CURRENT_TIMESTAMP WHERE id=? AND version=?",location,username,id,version)!=1)
            throw new BusinessException("Hồ sơ đã được thay đổi.");
    }
    public boolean alreadyMoved(UUID key,UUID id,UUID target,String username,String reason) {
        var rows=jdbc.queryForList("SELECT record_id,to_location_id,actor_username,reason FROM record_location_history WHERE operation_key=?",key);
        if(rows.isEmpty()) return false; var row=rows.getFirst();
        if(!id.equals(row.get("record_id")) || !target.equals(row.get("to_location_id")) || !username.equals(row.get("actor_username")) || !reason.equals(row.get("reason")))
            throw new BusinessException("Mã thao tác chuyển đã dùng với nội dung khác.");
        return true;
    }
    public void availability(UUID id,String status,String username,long version) {
        if(jdbc.update("UPDATE medical_records SET physical_status=?,version=version+1,updated_by=(SELECT id FROM app_users WHERE username=?),updated_at=CURRENT_TIMESTAMP WHERE id=? AND version=?",status,username,id,version)!=1)
            throw new BusinessException("Hồ sơ đã thay đổi.");
    }
}
