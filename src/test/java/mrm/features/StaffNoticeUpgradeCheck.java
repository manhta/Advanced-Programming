package mrm.features;

import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Verifies V24 approval data is corrected without removing staff requests or Admin work. */
public class StaffNoticeUpgradeCheck {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
    public static void main(String[] args)throws Exception {
        String password=System.getenv("DB_PASSWORD");if(password==null || password.isBlank())password=Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$","");
        String url=System.getenv().getOrDefault("DB_URL","jdbc:postgresql://127.0.0.1:5433/mrm"),user=System.getenv().getOrDefault("DB_USERNAME","postgres");
        String schema="staff_upgrade_"+UUID.randomUUID().toString().replace("-","");
        try(var c=DriverManager.getConnection(url,user,password)) {
            try(var s=c.createStatement()){s.execute("CREATE SCHEMA "+schema);}
            try {
                var ds=new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema,user,password);
                var jdbc=new JdbcTemplate(ds);
                Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).locations("classpath:mrm/database/migration").target("3").load().migrate();
                insertUser(jdbc,"archivist1","ARCHIVIST");
                Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).locations("classpath:mrm/database/migration").target("24").load().migrate();
                UUID reader=insertUser(jdbc,"upgrade_reader","READER"),director=insertUser(jdbc,"upgrade_director","DIRECTOR"),admin=insertUser(jdbc,"upgrade_admin","ADMIN");
                UUID department=jdbc.queryForObject("SELECT id FROM departments ORDER BY code LIMIT 1",UUID.class);
                UUID pending=request(jdbc,reader,director,admin,department,"APPROVED");
                UUID applied=request(jdbc,reader,director,admin,department,"APPLIED");
                UUID rejected=request(jdbc,reader,director,admin,department,"REJECTED");
                UUID pEvent=event(jdbc,pending,director,"APPROVE"),aEvent=event(jdbc,applied,director,"APPROVE"),applyEvent=event(jdbc,applied,admin,"APPLY"),rEvent=event(jdbc,rejected,director,"REJECT");
                UUID early=notice(jdbc,pending,pEvent,reader,"READER","Early result");
                UUID adminWork=notice(jdbc,pending,pEvent,admin,"ADMIN","Admin pending work");
                UUID oldApplied=notice(jdbc,applied,aEvent,reader,"READER","Old applied result");
                UUID refusal=notice(jdbc,rejected,rEvent,reader,"READER","Required rejection reason");
                Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).locations("classpath:mrm/database/migration").load().migrate();
                check(jdbc.queryForObject("SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1",String.class).equals("26"),"V24 staff data upgrades to V26");
                check(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM notifications WHERE id=?",Boolean.class,early),"premature pending approval result hidden");
                check(jdbc.queryForObject("SELECT deleted_at IS NULL FROM notifications WHERE id=?",Boolean.class,adminWork),"Admin pending approval work preserved");
                check(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM notifications WHERE id=?",Boolean.class,oldApplied),"old Director-based success retired for completed application");
                check(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE recipient_id=? AND staff_event_id=? AND deleted_at IS NULL",Integer.class,reader,applyEvent)==1,"completed application receives exactly one application-based result");
                check(jdbc.queryForObject("SELECT read_at IS NULL AND message='' FROM notifications WHERE recipient_id=? AND staff_event_id=?",Boolean.class,reader,applyEvent),"backfilled result is unread and contains only outcome");
                check(jdbc.queryForObject("SELECT message FROM notifications WHERE id=? AND deleted_at IS NULL",String.class,refusal).equals("Required rejection reason"),"rejection notification and reason preserved");
                check(jdbc.queryForObject("SELECT count(*) FROM staff_change_requests",Integer.class)==3,"all request rows preserved");
                check(jdbc.queryForObject("SELECT state FROM staff_change_requests WHERE id=?",String.class,pending).equals("APPROVED"),"pending request remains pending Admin action");
                check(jdbc.queryForObject("SELECT count(*) FROM staff_change_events",Integer.class)==4,"all historic events preserved");
                Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).locations("classpath:mrm/database/migration").load().migrate();
                check(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE staff_event_id=?",Integer.class,applyEvent)==1,"repeat startup does not duplicate results");
            }finally{try(var s=c.createStatement()){s.execute("DROP SCHEMA "+schema+" CASCADE");}}
            try(var s=c.prepareStatement("SELECT to_regnamespace(?) IS NULL")){s.setString(1,schema);try(var rs=s.executeQuery()){rs.next();check(rs.getBoolean(1),"temporary schema removed");}}
        }
        System.out.println("PASSED "+checks+" staff notification upgrade checks.");
    }
    private static UUID insertUser(JdbcTemplate jdbc,String name,String role){return jdbc.queryForObject("INSERT INTO app_users(username,password_hash,display_name,role_id) SELECT ?,'not-a-login-hash',?,id FROM roles WHERE code=? RETURNING id",UUID.class,name,name,role);}
    private static UUID request(JdbcTemplate jdbc,UUID reader,UUID director,UUID admin,UUID dep,String state){
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO staff_change_requests(id,submit_token,requester_id,original_role,requested_role,requested_department_id,reason,state,decided_by,decided_at,decision_note,applied_by,applied_at) VALUES(?,?,?,'READER','ARCHIVIST',?,'Synthetic request',?,?,CURRENT_TIMESTAMP,?, ?,CASE WHEN ?='APPLIED' THEN CURRENT_TIMESTAMP ELSE NULL END)",id,UUID.randomUUID(),reader,dep,state,director,state.equals("REJECTED")?"Required rejection reason":"",state.equals("APPLIED")?admin:null,state);return id;
    }
    private static UUID event(JdbcTemplate jdbc,UUID request,UUID actor,String action){UUID id=UUID.randomUUID();jdbc.update("INSERT INTO staff_change_events(id,request_id,actor_id,action,details) VALUES(?,?,?,?,'Synthetic event')",id,request,actor,action);return id;}
    private static UUID notice(JdbcTemplate jdbc,UUID request,UUID event,UUID recipient,String role,String text){return jdbc.queryForObject("INSERT INTO notifications(recipient_id,recipient_role,staff_request_id,staff_event_id,title,message) VALUES(?,?,?,?,'Synthetic notice',?) RETURNING id",UUID.class,recipient,role,request,event,text);}
}
