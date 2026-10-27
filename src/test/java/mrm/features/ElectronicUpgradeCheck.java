package mrm.features;

import java.nio.file.*;
import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Upgrade coverage with actual V14 reading data, never the user's public schema. */
public class ElectronicUpgradeCheck {
    private static int checks;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;System.out.println("PASS: "+message);}
    public static void main(String[] args)throws Exception {
        String password=System.getenv("DB_PASSWORD");if(password==null || password.isBlank())password=Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$","");
        String url=System.getenv().getOrDefault("DB_URL","jdbc:postgresql://127.0.0.1:5433/mrm"),user=System.getenv().getOrDefault("DB_USERNAME","postgres");
        String schema="e_upgrade_"+UUID.randomUUID().toString().replace("-","");
        try(var c=DriverManager.getConnection(url,user,password)) {
            try(var s=c.createStatement()){s.execute("CREATE SCHEMA "+schema);}
            try {
                var source=new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema,user,password);
                Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).locations("classpath:mrm/database/migration").target("3").load().migrate();
                var jdbc=new JdbcTemplate(source);
                jdbc.update("INSERT INTO app_users(username,password_hash,display_name,role_id) SELECT 'archivist1','not-a-login-password','Synthetic upgrade archivist',id FROM roles WHERE code='ARCHIVIST'");
                Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).locations("classpath:mrm/database/migration").target("14").load().migrate();
                UUID reader=jdbc.queryForObject("INSERT INTO app_users(username,password_hash,display_name,role_id) SELECT 'upgrade_reader','not-a-login-password','Synthetic upgrade reader',id FROM roles WHERE code='READER' RETURNING id",UUID.class);
                UUID record=UUID.randomUUID(),request=UUID.randomUUID(),event=UUID.randomUUID(),notice=UUID.randomUUID();
                jdbc.update("INSERT INTO medical_records(id,create_token,create_fingerprint,patient_name,sex,record_state,physical_status,quality_status,created_by,updated_by) VALUES(?,?,?,'Synthetic old record','UNKNOWN','DRAFT','NOT_STORED','NORMAL',?,?)",record,UUID.randomUUID(),"upgrade-fingerprint",reader,reader);
                jdbc.update("INSERT INTO reading_requests(id,submit_token,record_id,requester_id,requester_name_snapshot,purpose,state) VALUES(?,?,?,?,'upgrade_reader','Synthetic old purpose','SUBMITTED')",request,UUID.randomUUID(),record,reader);
                jdbc.update("INSERT INTO request_events(id,request_id,actor,action,revision,details) VALUES(?,?,'upgrade_reader','SUBMIT',1,'Synthetic existing event')",event,request);
                jdbc.update("INSERT INTO notifications(id,recipient_id,request_id,event_id,title,message) VALUES(?,?,?,?,'Existing title','Existing body')",notice,reader,request,event);
                Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).locations("classpath:mrm/database/migration").target("15").load().migrate();
                UUID electronic=UUID.randomUUID();
                jdbc.update("INSERT INTO electronic_records(id,create_token,create_fingerprint,record_code,patient_name,birth_year,sex,state,created_by,updated_by) VALUES(?,?,?,'MRM-REC-2026-999999','Synthetic V15 electronic',1990,'UNKNOWN','SUBMITTED',?,?)",electronic,UUID.randomUUID(),"upgrade-electronic",reader,reader);
                Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).locations("classpath:mrm/database/migration").target("16").load().migrate();
                UUID eEvent=UUID.randomUUID(),eNotice=UUID.randomUUID();
                jdbc.update("INSERT INTO electronic_record_events(id,record_id,actor,action,details,version,snapshot) VALUES(?,?,'upgrade_reader','SUBMIT','Existing electronic event',0,'{}')",eEvent,electronic);
                jdbc.update("INSERT INTO notifications(id,recipient_id,electronic_record_id,electronic_event_id,title,message) VALUES(?,?,?,?,'Existing electronic title','Existing electronic body')",eNotice,reader,electronic,eEvent);
                UUID cancellation=UUID.randomUUID();
                jdbc.update("UPDATE reading_requests SET state='CANCELLED' WHERE id=?",request);
                jdbc.update("INSERT INTO request_events(id,request_id,actor,action,revision,details) VALUES(?,?,'upgrade_reader','CANCEL',1,'Legacy cancellation')",cancellation,request);
                UUID waitingRequest=UUID.randomUUID(),approvalEvent=UUID.randomUUID();
                jdbc.update("INSERT INTO reading_requests(id,submit_token,record_id,requester_id,requester_name_snapshot,purpose,state) VALUES(?,?,?,?,'upgrade_reader','Waiting pickup','APPROVED')",waitingRequest,UUID.randomUUID(),record,reader);
                jdbc.update("INSERT INTO request_events(id,request_id,actor,action,revision,details) VALUES(?,?,'upgrade_director','APPROVE',1,'Approved')",approvalEvent,waitingRequest);
                String collision="MRM-PAPER-"+record.toString().replace("-", "").toUpperCase(java.util.Locale.ROOT);
                jdbc.update("UPDATE electronic_records SET record_code=? WHERE id=?",collision,electronic);
                boolean rejected=false;
                try{Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).locations("classpath:mrm/database/migration").load().migrate();}catch(RuntimeException expected){rejected=true;}
                check(rejected && jdbc.queryForObject("SELECT COUNT(*) FROM medical_records",Integer.class)==1 && jdbc.queryForObject("SELECT COUNT(*) FROM electronic_records",Integer.class)==1,"conflicting merge aborts atomically and leaves both original datasets intact");
                jdbc.update("UPDATE electronic_records SET record_code='MRM-REC-2026-999999' WHERE id=?",electronic);
                Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).locations("classpath:mrm/database/migration").load().migrate();
                check(jdbc.queryForObject("SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1",String.class).equals("26"),"V14 data upgraded to V26");
                check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE event_id=? AND recipient_role='ARCHIVIST' AND title LIKE 'Yêu cầu với hồ sơ % đã hủy'",Integer.class,cancellation)==1,"V20 backfills old cancelled requests for Archivist");
                Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).locations("classpath:mrm/database/migration").load().migrate();
                check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE event_id=? AND recipient_role='ARCHIVIST'",Integer.class,cancellation)==1,"migration restart does not duplicate cancellation notices");
                check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE request_id=? AND event_id=? AND recipient_role='ARCHIVIST'",Integer.class,waitingRequest,approvalEvent)==1,"V21 backfills Archivist confirmation tasks for existing approved requests");
                check(jdbc.queryForObject("SELECT deleted_at IS NULL FROM notifications WHERE id=?",Boolean.class,notice),"V21 preserves historical notices as undeleted");
                check(jdbc.queryForObject("SELECT birth_date IS NULL AND birth_year=1990 FROM medical_records WHERE id=?",Boolean.class,electronic),"V18 preserves legacy year without inventing a birth day or month");
                check(jdbc.queryForObject("SELECT patient_name FROM medical_records WHERE id=?",String.class,record).equals("Synthetic old record"),"existing paper record preserved");
                check(jdbc.queryForObject("SELECT purpose FROM reading_requests WHERE id=?",String.class,request).equals("Synthetic old purpose"),"existing reading request preserved");
                check(Boolean.TRUE.equals(jdbc.queryForObject("SELECT requires_director FROM reading_requests WHERE id=?",Boolean.class,request)),"V19 preserves historical Director approval route");
                check(jdbc.queryForObject("SELECT title||message FROM notifications WHERE id=? AND request_id=? AND event_id=? AND electronic_record_id IS NULL",String.class,notice,request,event).equals("Existing titleExisting body"),"old notification target and content preserved");
                check(jdbc.queryForObject("SELECT COUNT(*) FROM medical_records WHERE has_paper_copy AND has_electronic_copy",Integer.class)==1 && jdbc.queryForObject("SELECT COUNT(*) FROM medical_records WHERE NOT has_paper_copy AND has_electronic_copy",Integer.class)==1,"migration merges representations into one canonical table without changing old IDs");
                check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE id=? AND electronic_record_id=? AND electronic_event_id=?",Integer.class,eNotice,electronic,eEvent)==1,"electronic notification and event retain original foreign keys after merge");
                check(jdbc.queryForObject("SELECT COUNT(*) FROM electronic_records",Integer.class)==1,"upgrade preserves V15 electronic record without synthesizing more records");
                check(jdbc.queryForObject("SELECT patient_name FROM electronic_records WHERE id=? AND note IS NULL AND department_id IS NULL AND deceased=FALSE",String.class,electronic).equals("Synthetic V15 electronic"),"V16 preserves existing electronic record and safely defaults new fields");
            }finally{try(var s=c.createStatement()){s.execute("DROP SCHEMA "+schema+" CASCADE");}}
            try(var s=c.prepareStatement("SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name=?)")){s.setString(1,schema);try(var rs=s.executeQuery()){rs.next();check(!rs.getBoolean(1),"upgrade test schema removed");}}
        }
        System.out.println("PASSED "+checks+" electronic upgrade checks.");
    }
}
