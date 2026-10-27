package mrm.record;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mrm.catalog.CatalogSelectionService;
import mrm.catalog.CatalogSnapshot;
import mrm.common.BusinessException;
import mrm.storage.StorageRepository;

@Service
public class RecordService {
    private final RecordRepository records;
    private final StorageRepository storage;
    private final CatalogSelectionService catalogs;
    private final JdbcTemplate jdbc;
    public RecordService(RecordRepository records,StorageRepository storage,CatalogSelectionService catalogs,JdbcTemplate jdbc) {
        this.records=records;this.storage=storage;this.catalogs=catalogs;this.jdbc=jdbc;
    }
    public RecordPage search(RecordFilter filter,boolean archivist) { return records.search(filter,archivist); }
    public RecordPage search(RecordFilter filter,String username){return records.search(filter,username);}
    public Map<String,Object> identity(UUID id,String username){return records.identity(id,username);}
    public RecordDetails details(UUID id) { return records.details(id); }

    public record RequestPreview(UUID id,String archiveNumber,String patientName,boolean deceased,
            String medicalRecordNumber,String departmentName,String recordTypeName,LocalDate admissionDate,LocalDate dischargeDate) { }
    public record BorrowCandidates(List<RequestPreview> items,long total,int page,int size) { }
    public BorrowCandidates borrowCandidates(String query,int page,String username) {
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.username=? AND u.status='ACTIVE' AND r.code='READER')",Boolean.class,username)))
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Reader được chọn hồ sơ mượn.");
        query=query==null?"":query.strip();
        if(query.length()>200 || page<0 || page>1000000)throw new BusinessException("Bộ lọc hồ sơ không hợp lệ.");
        String where="""
            FROM medical_records mr LEFT JOIN departments dep ON dep.id=mr.department_id LEFT JOIN record_types rt ON rt.id=mr.record_type_id
            WHERE mr.has_paper_copy AND mr.record_state='STORED' AND mr.physical_status='IN_STORAGE' AND mr.quality_status='NORMAL'
            AND (?='' OR upper(mr.archive_number)=upper(?) OR upper(mr.medical_record_number)=upper(?)
                OR upper(mr.patient_code)=upper(?) OR mrm_search_key(mr.patient_name) LIKE mrm_search_key(?) ESCAPE '!')
            AND
            """+BorrowAvailability.NO_ACTIVE_REQUEST;
        String pattern="%"+query.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        long total=jdbc.queryForObject("SELECT COUNT(*) "+where,Long.class,query,query,query,query,pattern);
        var items=jdbc.query("""
            SELECT mr.id,mr.archive_number,mr.patient_name,mr.deceased,mr.medical_record_number,mr.admission_date,mr.discharge_date,
                COALESCE(mr.department_name_snapshot,dep.name) AS department_name,
                COALESCE(mr.record_type_name_snapshot,rt.name) AS type_name
            """+where+" ORDER BY mr.archive_number,mr.id LIMIT 20 OFFSET ?",
            (rs,n)->new RequestPreview(rs.getObject("id",UUID.class),rs.getString("archive_number"),rs.getString("patient_name"),rs.getBoolean("deceased"),
                rs.getString("medical_record_number"),rs.getString("department_name"),rs.getString("type_name"),rs.getObject("admission_date",LocalDate.class),rs.getObject("discharge_date",LocalDate.class)),query,query,query,query,pattern,(long)page*20);
        return new BorrowCandidates(List.copyOf(items),total,page,20);
    }
    public RequestPreview requestPreview(UUID id,String username) {
        var summary=records.summary(id,false);
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.username=? AND u.status='ACTIVE' AND r.code='READER')",Boolean.class,username)))
            throw new org.springframework.security.access.AccessDeniedException("Chỉ Reader được xin phép đọc.");
        return new RequestPreview(id,summary.archiveNumber(),summary.patientName(),summary.deceased(),summary.medicalRecordNumber(),summary.departmentName(),summary.recordTypeName(),summary.admissionDate(),summary.dischargeDate());
    }
    public RequestPreview borrowPreview(UUID id,String username) {
        var preview=requestPreview(id,username);
        if(!Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM medical_records mr WHERE mr.id=? AND mr.has_paper_copy AND mr.record_state='STORED'
                AND mr.physical_status='IN_STORAGE' AND mr.quality_status='NORMAL' AND
            """+BorrowAvailability.NO_ACTIVE_REQUEST+")",Boolean.class,id)))
            throw new BusinessException("Hồ sơ không còn khả dụng hoặc đã có đăng ký mượn chưa hoàn tất. Vui lòng chọn hồ sơ khác.");
        return preview;
    }
    public boolean paperHandedToReader(UUID id,String username) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM reading_sessions s JOIN reading_requests q ON q.id=s.request_id
                JOIN app_users u ON u.id=q.requester_id JOIN roles role ON role.id=u.role_id
                WHERE s.record_id=? AND u.username=? AND u.status='ACTIVE' AND role.code='READER'
                AND s.state='READING' AND s.handed_at IS NOT NULL AND s.returned_at IS NULL)
            """,Boolean.class,id,username));
    }
    /** Existing borrowing grant; basic published metadata is now independently accessible via basicDetail. */
    public boolean readerMayView(UUID id,String username) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM reading_requests q JOIN app_users u ON u.id=q.requester_id
            JOIN roles role ON role.id=u.role_id JOIN medical_records r ON r.id=q.record_id
            WHERE q.record_id=? AND u.username=? AND u.status='ACTIVE' AND role.code='READER'
            AND r.record_state='STORED'
            AND q.state IN ('APPROVED','CONFIRMED') AND q.approver_id IS NOT NULL AND q.decided_at IS NOT NULL
            AND (EXISTS(SELECT 1 FROM reading_sessions s WHERE s.request_id=q.id AND s.state<>'CANCELLED' AND s.reading_on>=?)
                OR (q.scheduling_mode='ARCHIVIST' AND q.state='APPROVED' AND q.pickup_deadline>=?
                    AND NOT EXISTS(SELECT 1 FROM reading_sessions s WHERE s.request_id=q.id))))
            """,Boolean.class,id,username,LocalDate.now(ZoneId.of("Asia/Bangkok")),LocalDate.now(ZoneId.of("Asia/Bangkok"))));
    }

    @Transactional
    public RecordSummary basicDetail(UUID id,String username) {
        var summary=records.summary(id,false);
        records.identity(id,username); // Authenticate the clinical role; basic published metadata no longer requires a paper loan.
        records.event(id,"VIEW",username,"Xem metadata cơ bản",null,null); return summary;
    }
    @Transactional
    public RecordDetails operationalDetail(UUID id,String username) {
        var detail=records.details(id);
        records.event(id,"VIEW",username,"Xem hồ sơ nghiệp vụ kho",null,null); return detail;
    }
    public List<Map<String,Object>> history(UUID id) { return records.history(id); }
    public List<Map<String,Object>> locationHistory(UUID id) { return records.locationHistory(id); }

    @Transactional
    public UUID create(RecordForm form,String mode,String username) {
        requireArchivist(username); validate(form,mode,false);
        if((form.medicalRecordNumber()!=null && form.medicalRecordNumber().startsWith("MRM-REC-")) || (form.archiveNumber()!=null && form.archiveNumber().startsWith("MRM-REC-")))throw new BusinessException("Mã bệnh án tự động thuộc hồ sơ đã khai báo. Tìm mã và chọn Bổ sung bản giấy trên hồ sơ hiện có.");
        String fingerprint=fingerprint(form,mode);
        UUID previous=records.previousCreate(form.operationKey(),fingerprint,username);
        if(previous!=null) return previous;
        UUID id=UUID.randomUUID(); boolean publish=mode.equals("STORE");
        CatalogSnapshot snapshot=null; String archive=form.archiveNumber(); String physical="NOT_STORED";
        String path=null;
        if(publish) {
            storage.lockTree(); var location=storage.requireActive(form.locationId(),true); checkCapacity(location.capacity(),location.occupied());
            // Run after the intake/movement lock so concurrent forms see the first committed intake.
            if(records.suspectedDuplicate(id,form)) throw new BusinessException("Nghi trùng số bệnh án, mã người bệnh và đợt điều trị. Hãy lưu nháp để đối soát.");
            snapshot=catalogs.select(form.departmentId(),form.recordTypeId(),form.policyIds(),form.firstStoredOn()); checkDeceased(form,snapshot);
            if(archive==null) archive=records.nextArchive(form.firstStoredOn());
            path=location.path(); physical=form.qualityStatus().equals("NORMAL") ? "IN_STORAGE" : "UNAVAILABLE";
        } else validateDraftReferences(form);
        records.write(id,form,archive,publish ? "STORED" : "DRAFT",physical,snapshot,username,true,fingerprint);
        if(publish) records.locationHistory(id,null,form.locationId(),null,path,username,"Nhập kho lần đầu",form.operationKey());
        // Full source text is already in the snapshot; keep the event reason within its own column limit.
        records.event(id,publish ? "STORE" : "CREATE_DRAFT",username,"Tạo hồ sơ; nguồn dữ liệu được ghi trong bản chụp.",null,records.snapshotJson(id));
        return id;
    }

    @Transactional
    public void update(UUID id,RecordForm form,String mode,String username) {
        requireArchivist(username); records.lock(id); var old=records.details(id);
        if(old.form().version().longValue()!=form.version().longValue()) throw new BusinessException("Hồ sơ đã được người khác sửa. Vui lòng tải lại.");
        if(!old.form().operationKey().equals(form.operationKey())) throw new BusinessException("Phiếu sửa không hợp lệ.");
        if(old.physicalStatus().equals("READING")) throw new BusinessException("Hồ sơ đang được giao đọc, phải nhận trả trước khi sửa.");
        var identity=records.identity(id,username);
        if(!Boolean.TRUE.equals(identity.get("has_paper_copy")) && (!old.state().equals("PUBLISHED") || !mode.equals("STORE")))throw new BusinessException("Chỉ bổ sung bản giấy cho hồ sơ đã được Giám đốc công bố; không tạo bản ghi thứ hai.");
        boolean stored=old.state().equals("STORED");
        if(stored && !mode.equals("STORE")) throw new BusinessException("Hồ sơ đã nhập kho không thể chuyển thành nháp.");
        validate(form,mode,stored);
        if(form.reason()==null) throw new BusinessException("Cần lý do sửa/đối soát hồ sơ.");
        if(stored && (!Objects.equals(old.form().archiveNumber(),form.archiveNumber()) || !Objects.equals(old.form().firstStoredOn(),form.firstStoredOn())))
            throw new BusinessException("Không đổi số lưu trữ hoặc ngày đầu lưu sau nhập kho trong phiên bản này.");
        if(stored && !Objects.equals(old.form().locationId(),form.locationId())) throw new BusinessException("Đổi vị trí phải dùng chức năng chuyển hồ sơ.");
        boolean publish=mode.equals("STORE"); CatalogSnapshot snapshot=null;
        String physical="NOT_STORED",archive=form.archiveNumber(),path=null;
        String before=records.snapshotJson(id);
        if(publish) {
            storage.lockTree(); var target=storage.requireActive(form.locationId(),true);
            if(records.suspectedDuplicate(id,form)) throw new BusinessException("Nghi trùng đợt điều trị. Hãy đối soát thông tin nguồn trước khi nhập kho.");
            if(!stored) checkCapacity(target.capacity(),target.occupied());
            path=target.path();
            boolean same=stored && Objects.equals(old.form().departmentId(),form.departmentId()) && Objects.equals(old.form().recordTypeId(),form.recordTypeId())
                    && old.form().deceased()==form.deceased() && new HashSet<>(old.form().policyIds()).equals(new HashSet<>(form.policyIds()));
            if(same) snapshot=new CatalogSnapshot(form.departmentId(),old.departmentName(),form.recordTypeId(),
                    jdbc.queryForObject("SELECT record_type_code_snapshot FROM medical_records WHERE id=?",String.class,id),
                    old.recordTypeName(),old.policies(),old.retentionYears(),old.form().firstStoredOn(),old.retentionDueOn());
            else {
                snapshot=catalogs.select(form.departmentId(),form.recordTypeId(),form.policyIds(),form.firstStoredOn()); checkDeceased(form,snapshot);
            }
            if(archive==null) archive=records.nextArchive(form.firstStoredOn());
            physical=!form.qualityStatus().equals("NORMAL") ? "UNAVAILABLE" : stored ? old.physicalStatus() : "IN_STORAGE";
        } else validateDraftReferences(form);
        records.write(id,form,archive,publish ? "STORED" : "DRAFT",physical,snapshot,username,false,null);
        if(publish && !stored) records.locationHistory(id,null,form.locationId(),null,path,username,"Nhập kho từ bản nháp",UUID.randomUUID());
        records.event(id,publish && !stored ? "STORE" : "UPDATE",username,form.reason(),before,records.snapshotJson(id));
    }

    @Transactional
    public void move(UUID id,UUID destination,long version,UUID operationKey,String reason,String username) {
        requireArchivist(username); reason=requiredReason(reason);
        if(destination==null || operationKey==null) throw new BusinessException("Cần đích và mã thao tác chuyển.");
        records.lock(id);
        if(records.alreadyMoved(operationKey,id,destination,username,reason)) return;
        var record=records.details(id);
        if(record.form().version()!=version) throw new BusinessException("Hồ sơ đã thay đổi. Vui lòng tải lại trước khi chuyển.");
        if(!record.state().equals("STORED") || !record.physicalStatus().equals("IN_STORAGE")) throw new BusinessException("Chỉ chuyển hồ sơ đang trong kho và khả dụng.");
        if(destination.equals(record.form().locationId())) throw new BusinessException("Vị trí đích trùng vị trí hiện tại.");
        storage.lockTree(); var target=storage.requireActive(destination,true); checkCapacity(target.capacity(),target.occupied());
        String fromPath=storage.get(record.form().locationId()).path(),before=records.snapshotJson(id);
        records.move(id,destination,username,version);
        records.locationHistory(id,record.form().locationId(),destination,fromPath,target.path(),username,reason,operationKey);
        records.event(id,"MOVE",username,reason,before,records.snapshotJson(id));
    }

    @Transactional
    public void availability(UUID id,long version,String status,String reason,String username) {
        requireArchivist(username); reason=requiredReason(reason); records.lock(id); var old=records.details(id);
        if(old.form().version()!=version) throw new BusinessException("Hồ sơ đã thay đổi. Vui lòng tải lại.");
        if(!List.of("IN_STORAGE","UNAVAILABLE").contains(status) || !old.state().equals("STORED") || old.physicalStatus().equals("READING")) throw new BusinessException("Không thể đổi trạng thái hồ sơ này.");
        if(status.equals("IN_STORAGE") && !old.form().qualityStatus().equals("NORMAL")) throw new BusinessException("Cần hoàn thiện tình trạng hồ sơ trước khi xác nhận khả dụng.");
        String before=records.snapshotJson(id); records.availability(id,status,username,version);
        records.event(id,"AVAILABILITY",username,reason,before,records.snapshotJson(id));
    }

    private void requireArchivist(String username) {
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.username=? AND u.status='ACTIVE' AND r.code='ARCHIVIST')",Boolean.class,username)))
            throw new org.springframework.security.access.AccessDeniedException("Cần tài khoản Archivist đang hoạt động.");
    }
    private static void checkCapacity(Integer capacity,long occupied) { if(capacity!=null && occupied>=capacity) throw new BusinessException("Ngăn đã đầy."); }
    private static String requiredReason(String reason) {
        if(reason==null || reason.isBlank() || reason.strip().length()>1000) throw new BusinessException("Cần lý do từ 1 đến 1000 ký tự.");
        return reason.strip();
    }
    private void validateDraftReferences(RecordForm form) {
        if(form.departmentId()!=null && !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM departments WHERE id=?)",Boolean.class,form.departmentId()))) throw new BusinessException("Khoa không tồn tại.");
        if(form.recordTypeId()!=null && !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM record_types WHERE id=?)",Boolean.class,form.recordTypeId()))) throw new BusinessException("Loại bệnh án không tồn tại.");
        for(UUID id:form.policyIds()) if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM retention_policies WHERE id=?)",Boolean.class,id))) throw new BusinessException("Chính sách không tồn tại.");
    }
    private static void validate(RecordForm f,String mode,boolean alreadyStored) {
        if(!List.of("DRAFT","STORE").contains(mode)) throw new BusinessException("Thao tác lưu không hợp lệ.");
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Bangkok"));
        if(f.birthYear()!=null && f.birthYear()>today.getYear()) throw new BusinessException("Năm sinh không được ở tương lai.");
        for(LocalDate date:Arrays.asList(f.admissionDate(),f.dischargeDate(),f.firstStoredOn())) if(date!=null && date.isAfter(today)) throw new BusinessException("Ngày điều trị/đầu lưu không được ở tương lai.");
        if(f.birthYear()!=null && f.admissionDate()!=null && f.birthYear()>f.admissionDate().getYear()) throw new BusinessException("Năm sinh không được sau năm vào viện.");
        if(f.admissionDate()!=null && f.dischargeDate()!=null && f.dischargeDate().isBefore(f.admissionDate())) throw new BusinessException("Ngày ra viện phải từ ngày vào viện trở đi.");
        if(f.firstStoredOn()!=null && f.dischargeDate()!=null && f.firstStoredOn().isBefore(f.dischargeDate())) throw new BusinessException("Ngày đầu lưu phải từ ngày ra viện trở đi.");
        if(mode.equals("STORE")) {
            if(f.medicalRecordNumber()==null || f.birthYear()==null || f.departmentId()==null || f.recordTypeId()==null || f.admissionDate()==null
                    || f.dischargeDate()==null || f.firstStoredOn()==null || f.locationId()==null || f.policyIds().isEmpty() || f.sourceNote()==null)
                throw new BusinessException("Chưa đủ thông tin nhập kho: số bệnh án, năm sinh, khoa, loại, ngày vào/ra/đầu lưu, vị trí, chính sách và nguồn. Có thể lưu nháp để đối soát.");
            if(!alreadyStored && f.qualityStatus().equals("INCOMPLETE")) throw new BusinessException("Hồ sơ chưa đầy đủ chỉ được lưu nháp.");
        }
    }
    private static void checkDeceased(RecordForm form,CatalogSnapshot snapshot) {
        boolean deathPolicy=snapshot.policies().stream().anyMatch(p -> p.code().equals("DECEASED"));
        if(form.deceased()!=deathPolicy) throw new BusinessException("Cờ tử vong và nhóm chính sách DECEASED phải khớp nhau.");
    }
    private static String fingerprint(RecordForm form,String mode) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((form.toString()+"|"+mode).getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
