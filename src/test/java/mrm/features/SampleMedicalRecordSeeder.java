package mrm.features;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import mrm.MedicalRecordApplication;
import mrm.record.RecordForm;
import mrm.record.RecordService;
import mrm.storage.LocationForm;
import mrm.storage.StorageService;

/** Explicit one-time sample import. Never invoked on normal application startup. */
public class SampleMedicalRecordSeeder {
    private record Group(String type,String department,int count,String sex,int birthYear) { }
    private static final List<Group> GROUPS=List.of(
        new Group("INTERNAL_MEDICINE","INTERNAL_MEDICINE",3,"MALE",1970),
        new Group("SURGERY","GENERAL_SURGERY",3,"MALE",1980),
        new Group("PEDIATRICS","PEDIATRICS_RH",3,"FEMALE",2016),
        new Group("INFECTIOUS","INFECTIOUS_DISEASES",2,"MALE",1990),
        new Group("OBSTETRICS","PEDIATRICS_RH",2,"FEMALE",1995),
        new Group("GYNECOLOGY","PEDIATRICS_RH",2,"FEMALE",1985));

    public static void main(String[] args)throws Exception {
        String password=System.getenv("DB_PASSWORD");
        if(password==null || password.isBlank())password=Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$","");
        String url=System.getenv().getOrDefault("DB_URL","jdbc:postgresql://127.0.0.1:5433/mrm");
        try(var context=new SpringApplicationBuilder(MedicalRecordApplication.class).run(
                "--spring.profiles.active=dev","--server.port=0","--spring.datasource.url="+url,
                "--spring.datasource.username="+System.getenv().getOrDefault("DB_USERNAME","postgres"),
                "--spring.datasource.password="+password)) {
            var jdbc=context.getBean(JdbcTemplate.class);
            System.out.println("TARGET "+jdbc.queryForObject("SELECT current_database() || '/' || current_schema()",String.class));
            System.out.println("BEFORE_RECORDS "+jdbc.queryForObject("SELECT COUNT(*) FROM medical_records",Integer.class));
            jdbc.queryForList("SELECT code,status,active,effective_from,effective_to FROM retention_policies ORDER BY code,version_no")
                .forEach(row->System.out.println("POLICY "+row));
            if(args.length==0 || !args[0].equals("--apply"))return;
            new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
                // Same lock as intake, followed by a second empty-database check. Never append to user data.
                jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended('mrm:storage',0))",rs->{});
                if(jdbc.queryForObject("SELECT COUNT(*) FROM medical_records",Integer.class)!=0)
                    throw new IllegalStateException("Database already contains records; no sample data added.");
                LocalDate today=LocalDate.now(ZoneId.of("Asia/Bangkok"));
                if(java.util.Arrays.asList(args).contains("--reactivate-general")) {
                    // Explicit user-approved maintenance; retain the original Director approval.
                    jdbc.execute("LOCK TABLE retention_policies IN SHARE ROW EXCLUSIVE MODE");
                    var approved=jdbc.queryForList("""
                        SELECT id FROM retention_policies WHERE code='GENERAL' AND status='APPROVED' AND years=10
                        AND effective_from<=? AND (effective_to IS NULL OR effective_to>=?) ORDER BY version_no DESC LIMIT 1
                        """,UUID.class,today,today);
                    if(approved.isEmpty())throw new IllegalStateException("No approved 10-year GENERAL policy; no changes made.");
                    UUID policyId=approved.getFirst();
                    if(jdbc.queryForObject("""
                        SELECT COUNT(*) FROM retention_policies p JOIN retention_policies chosen ON chosen.id=?
                        WHERE p.code='GENERAL' AND p.active AND p.id<>chosen.id
                        AND p.effective_from<=COALESCE(chosen.effective_to,DATE '9999-12-31')
                        AND chosen.effective_from<=COALESCE(p.effective_to,DATE '9999-12-31')
                        """,Integer.class,policyId)>0)throw new IllegalStateException("Overlapping active GENERAL policy; no changes made.");
                    if(jdbc.update("UPDATE retention_policies SET active=TRUE WHERE id=? AND NOT active",policyId)==1) {
                        jdbc.update("""
                            INSERT INTO catalog_events(target_type,target_id,action,actor_username,details)
                            VALUES('RETENTION_POLICY',?,'REACTIVATE','SYSTEM:SAMPLE_IMPORT',?)
                            """,policyId,"User explicitly authorized reactivating approved GENERAL for 15 synthetic sample records on 2026-10-22; original approval preserved.");
                    }
                }
                var policies=jdbc.queryForList("""
                    SELECT id FROM retention_policies WHERE code='GENERAL' AND status='APPROVED' AND active
                    AND effective_from<=? AND (effective_to IS NULL OR effective_to>=?) ORDER BY version_no DESC
                    """,UUID.class,today,today);
                if(policies.isEmpty())throw new IllegalStateException("An approved active GENERAL policy is required. No data added.");
                String actor=jdbc.queryForObject("""
                    SELECT u.username FROM app_users u JOIN roles r ON r.id=u.role_id
                    WHERE r.code='ARCHIVIST' AND u.status='ACTIVE' ORDER BY u.username LIMIT 1
                    """,String.class);
                var storage=context.getBean(StorageService.class);
                var records=context.getBean(RecordService.class);
                UUID warehouse=storage.create(new LocationForm("DEMO_WAREHOUSE","Kho mẫu — dữ liệu giả lập","WAREHOUSE",null,null),actor);
                UUID shelf=storage.create(new LocationForm("DEMO_SHELF","Kệ hồ sơ mẫu","SHELF",warehouse,null),actor);
                int index=0;
                for(var group:GROUPS) {
                    UUID type=jdbc.queryForObject("SELECT id FROM record_types WHERE code=? AND active",UUID.class,group.type());
                    UUID department=jdbc.queryForObject("SELECT id FROM departments WHERE code=? AND active",UUID.class,group.department());
                    UUID slot=storage.create(new LocationForm("DEMO_"+group.type(),"Ngăn mẫu — "+group.type(),"COMPARTMENT",shelf,20),actor);
                    for(int i=0;i<group.count();i++) {
                        String number=String.format(java.util.Locale.ROOT,"%03d",++index);
                        var form=new RecordForm(UUID.randomUUID(),0L,"DEMO-MRM-"+number,"DEMO-BA-"+number,"DEMO-BN-"+number,
                            "Người bệnh giả lập "+number,group.birthYear()+i,group.sex(),department,type,false,
                            today.minusDays(10+i),today.minusDays(2),today,List.of(policies.getFirst()),slot,"NORMAL",
                            "DEMO_MRM_15: Dữ liệu tổng hợp để học tập, không phải bệnh nhân thật; nhập theo yêu cầu người dùng.",
                            "Hồ sơ mẫu để thử tra cứu, vị trí và đăng ký đọc. Không chứa bệnh án lâm sàng thật.",null);
                        records.create(form,"STORE",actor);
                    }
                }
                if(index!=15 || jdbc.queryForObject("SELECT COUNT(*) FROM medical_records WHERE record_state='STORED' AND physical_status='IN_STORAGE' AND retention_years_snapshot=10",Integer.class)!=15)
                    throw new IllegalStateException("Sample verification failed; rolling back all changes.");
                if(jdbc.queryForObject("SELECT COUNT(*) FROM record_location_history",Integer.class)!=15)
                    throw new IllegalStateException("Missing intake history; rolling back all changes.");
                if(records.search(new mrm.record.RecordFilter("Người bệnh giả lập",null,null,null,null,null,null,0,20,"newest"),false).total()!=15)
                    throw new IllegalStateException("Reader sample search failed; rolling back all changes.");
            });
            System.out.println("COMMITTED_RECORDS "+jdbc.queryForObject("SELECT COUNT(*) FROM medical_records",Integer.class));
            jdbc.queryForList("""
                SELECT t.code,t.name,COUNT(*) AS samples FROM medical_records r JOIN record_types t ON t.id=r.record_type_id
                WHERE r.source_note LIKE 'DEMO_MRM_15:%' GROUP BY t.code,t.name ORDER BY t.code
                """).forEach(row->System.out.println("SAMPLE_GROUP "+row));
            System.out.println("SAMPLE_IMPORT_COMPLETE; original catalog and account permissions preserved.");
        }
    }
}
