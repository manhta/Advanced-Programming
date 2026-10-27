package mrm.features;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import mrm.MedicalRecordApplication;
import mrm.record.*;

/** Explicit development-data maintenance only; never runs on ordinary startup. */
public class DeceasedSampleMaintenance {
    public static void main(String[] args)throws Exception {
        String password=System.getenv("DB_PASSWORD");
        if(password==null || password.isBlank())password=Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$","");
        try(var app=new SpringApplicationBuilder(MedicalRecordApplication.class).run(
                "--spring.profiles.active=dev","--server.port=0",
                "--spring.datasource.url="+System.getenv().getOrDefault("DB_URL","jdbc:postgresql://127.0.0.1:5433/mrm"),
                "--spring.datasource.username="+System.getenv().getOrDefault("DB_USERNAME","postgres"),"--spring.datasource.password="+password)) {
            var jdbc=app.getBean(JdbcTemplate.class);var records=app.getBean(RecordService.class);var repository=app.getBean(RecordRepository.class);
            System.out.println("TARGET "+jdbc.queryForObject("SELECT current_database()||'/'||current_schema()",String.class));
            int count=jdbc.queryForObject("SELECT COUNT(*) FROM medical_records WHERE deceased AND has_electronic_copy",Integer.class);
            System.out.println("DECEASED_BEFORE "+count);
            if(!Arrays.asList(args).contains("--apply"))return;
            new TransactionTemplate(app.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
                jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended('mrm:storage',0))",rs->{});
                List<UUID> ids=jdbc.queryForList("SELECT id FROM medical_records WHERE deceased AND has_electronic_copy ORDER BY id",UUID.class);
                boolean convert=ids.isEmpty();
                if(convert) {
                    ids=jdbc.queryForList("SELECT mr.id FROM medical_records mr WHERE mr.has_electronic_copy AND mr.record_code LIKE 'DEMO-MRM-%' AND mr.record_state IN ('STORED','PUBLISHED') AND "+BorrowAvailability.NO_ACTIVE_REQUEST+" ORDER BY random() LIMIT 3",UUID.class);
                    if(ids.size()!=3)throw new IllegalStateException("Need three available DEMO records; no other patient records will be converted.");
                }
                ids=ids.stream().sorted().toList();
                String actor=jdbc.queryForObject("SELECT u.username FROM app_users u JOIN roles r ON r.id=u.role_id WHERE r.code='ARCHIVIST' AND u.status='ACTIVE' ORDER BY u.username LIMIT 1",String.class);
                for(UUID id:ids)repository.lock(id);
                try {
                    Path folder=Path.of("target/verification/deceased-backups");Files.createDirectories(folder);
                    String stamp=Instant.now().toString().replace(":","-");
                    for(UUID id:ids)Files.writeString(folder.resolve(stamp+"-"+id+".json"),repository.snapshotJson(id),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
                }catch(java.io.IOException ex){throw new IllegalStateException("Backup failed; no record changed",ex);}
                for(UUID id:ids) {
                    var f=records.details(id).form();
                    if(convert && !Boolean.TRUE.equals(jdbc.queryForObject("SELECT "+BorrowAvailability.NO_ACTIVE_REQUEST+" FROM medical_records mr WHERE mr.id=?",Boolean.class,id)))throw new IllegalStateException("Record is now reserved; retry maintenance later.");
                    String note=f.note()==null?"Bệnh án tử":f.note().contains("Bệnh án tử")?f.note():f.note()+"\nBệnh án tử";
                    boolean paper=jdbc.queryForObject("SELECT has_paper_copy FROM medical_records WHERE id=?",Boolean.class,id);
                    if(convert && paper) {
                        var policies=new ArrayList<>(f.policyIds());
                        UUID deathPolicy=jdbc.queryForObject("SELECT id FROM retention_policies WHERE code='DECEASED' AND active AND status='APPROVED' ORDER BY version_no DESC LIMIT 1",UUID.class);
                        if(!policies.contains(deathPolicy))policies.add(deathPolicy);
                        records.update(id,new RecordForm(f.operationKey(),f.version(),f.archiveNumber(),f.medicalRecordNumber(),f.patientCode(),f.patientName(),f.birthYear(),f.sex(),f.departmentId(),f.recordTypeId(),true,f.admissionDate(),f.dischargeDate(),f.firstStoredOn(),policies,f.locationId(),f.qualityStatus(),f.sourceNote(),note,"Chuyển hồ sơ mẫu sang bệnh án tử theo yêu cầu người dùng"),"STORE",actor);
                    }else if(convert || !Objects.equals(note,f.note())) {
                        String before=repository.snapshotJson(id);
                        jdbc.update("UPDATE medical_records SET deceased=TRUE,note=?,version=version+1,updated_at=CURRENT_TIMESTAMP,updated_by=(SELECT id FROM app_users WHERE username=?) WHERE id=?",note,actor,id);
                        repository.event(id,"UPDATE",actor,"Đánh dấu bệnh án tử theo yêu cầu người dùng",before,repository.snapshotJson(id));
                    }
                    System.out.println("DECEASED_RECORD "+jdbc.queryForObject("SELECT record_code FROM medical_records WHERE id=?",String.class,id)+" | paper="+paper+" | note marker saved");
                }
            });
            System.out.println("DECEASED_AFTER "+jdbc.queryForObject("SELECT COUNT(*) FROM medical_records WHERE deceased AND has_electronic_copy",Integer.class));
        }
    }
}
