package mrm.features;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import mrm.MedicalRecordApplication;
import mrm.account.*;
import mrm.common.BusinessException;
import mrm.notification.NotificationService;

/** HR service and real HTTP checks, isolated from the user's public schema. */
public class StaffManagementCheck {
    private static final String INITIAL="Synthetic-staff-initial-2048!",LOGIN="Synthetic-staff-selected-2048!",SECRET="Synthetic-staff-approval-2048!";
    private static int checks;private static String base;private static JdbcTemplate jdbc;
    private static AccountService accounts;private static StaffChangeService changes;private static NotificationService notices;
    private static UUID dep,other;
    private static void check(boolean value,String description){if(!value)throw new AssertionError(description);checks++;System.out.println("PASS: "+description);}
    private static void expect(Class<? extends Throwable> type,Runnable action){try{action.run();throw new AssertionError("Expected "+type.getSimpleName());}catch(Throwable ex){if(!type.isInstance(ex))throw ex;checks++;System.out.println("PASS: rejected "+type.getSimpleName());}}
    public static void main(String[] args)throws Exception {
        String password=System.getenv("DB_PASSWORD");if(password==null || password.isBlank())password=Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$","");
        String url=System.getenv().getOrDefault("DB_URL","jdbc:postgresql://127.0.0.1:5433/mrm"),user=System.getenv().getOrDefault("DB_USERNAME","postgres");
        String schema="staff_check_"+UUID.randomUUID().toString().replace("-","");
        try(var connection=DriverManager.getConnection(url,user,password)) {
            try(var s=connection.createStatement()){s.execute("CREATE SCHEMA "+schema);}
            try(var app=new SpringApplicationBuilder(MedicalRecordApplication.class).run("--spring.profiles.active=dev,seed-users","--server.port=0",
                    "--spring.datasource.url="+url+(url.contains("?")?"&":"?")+"currentSchema="+schema,"--spring.datasource.username="+user,"--spring.datasource.password="+password,
                    "--spring.flyway.schemas="+schema,"--spring.flyway.default-schema="+schema,"--SEED_READER_PASSWORD="+INITIAL,"--SEED_ARCHIVIST_PASSWORD="+INITIAL,"--SEED_DIRECTOR_PASSWORD="+INITIAL,"--SEED_ADMIN_PASSWORD="+INITIAL)) {
                jdbc=app.getBean(JdbcTemplate.class);accounts=app.getBean(AccountService.class);changes=app.getBean(StaffChangeService.class);notices=app.getBean(NotificationService.class);
                base="http://localhost:"+((WebServerApplicationContext)app).getWebServer().getPort();
                check(jdbc.queryForObject("SELECT current_schema()",String.class).equals(schema),"mutations confined to disposable schema");
                var departments=jdbc.queryForList("SELECT id FROM departments WHERE active ORDER BY code",UUID.class);dep=departments.get(0);other=departments.get(1);
                for(String name:List.of("reader1","archivist1","director1","admin1"))accounts.completeInitialSetup(name,LOGIN,LOGIN,name.equals("director1")?SECRET:null,name.equals("director1")?SECRET:null);
                automaticAccounts();profiles();workflow();
                check(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE details LIKE ? OR details LIKE ? OR details LIKE ?",Integer.class,"%"+INITIAL+"%","%"+LOGIN+"%","%"+SECRET+"%")==0,"no secrets in audit");
            }finally{try(var s=connection.createStatement()){s.execute("DROP SCHEMA "+schema+" CASCADE");}}
            try(var s=connection.prepareStatement("SELECT to_regnamespace(?) IS NULL")){s.setString(1,schema);try(var rs=s.executeQuery()){rs.next();check(rs.getBoolean(1),"temporary schema removed");}}
        }
        System.out.println("PASSED "+checks+" staff management checks.");
    }
    private static void automaticAccounts()throws Exception {
        var issued=accounts.createAutomatically("Nguyễn Văn An","READER",dep,null,"0000003001",LocalDate.of(1990,4,12),"MALE","admin1");
        check(issued.username().equals("an.nv12041990@mrm.com"),"Vietnamese name and birth date produce agreed username");
        var encoder=new mrm.config.SecurityConfig().passwordEncoder();
        check(encoder.matches(issued.initialPassword(),jdbc.queryForObject("SELECT password_hash FROM app_users WHERE username=?",String.class,issued.username())),"generated random password matches stored hash");
        check(jdbc.queryForObject("SELECT must_change_password FROM app_users WHERE username=?",Boolean.class,issued.username()),"generated account requires first-time password change");
        expect(BusinessException.class,()->accounts.createAutomatically("Nguyễn Văn An","READER",null,null,"0000003002",LocalDate.of(1990,4,12),"MALE","admin1"));
        expect(BusinessException.class,()->accounts.createAutomatically("Another Name","READER",dep,null,"0000003001",LocalDate.of(1992,1,1),"MALE","admin1"));
        expect(org.springframework.security.access.AccessDeniedException.class,()->accounts.createAutomatically("Another Name","READER",dep,null,"0000003002",LocalDate.of(1992,1,1),"MALE","reader1"));
        var collision=accounts.createAutomatically("Nguyễn Văn An","READER",dep,null,"0000003002",LocalDate.of(1990,4,12),"MALE","admin1");
        check(collision.username().equals("an.nv12041990-2@mrm.com"),"same-name birth-date collision receives unique suffix");
        var admin=login("admin1",LOGIN);String form=get(admin,"/admin/accounts/new").body();
        check(!form.contains("name=\"username\"") && !form.contains("name=\"password\""),"creation HTML has no manual username/password fields");
        check(form.contains("name=\"department\" required"),"department is required in HTML");
        String token=csrf(form);
        check(post(admin,"/admin/accounts","bad","displayName","Test").statusCode()==403,"automatic creation enforces CSRF");
        int before=jdbc.queryForObject("SELECT count(*) FROM app_users",Integer.class);
        var response=post(admin,"/admin/accounts",token,"displayName","Trần Văn Bình","role","READER","department",dep.toString(),"citizenId","0000003003","birthDate","1995-02-03","gender","MALE","email","");
        check(response.statusCode()==200 && response.body().contains("binh.tv03021995@mrm.com"),"Admin can create without username/password parameters");
        check(response.headers().firstValue("cache-control").orElse("").contains("no-store"),"credential response is not cached");
        var code=Pattern.compile("<code>([^<]+)</code>").matcher(response.body());check(code.find(),"generated username shown in handover result");check(code.find(),"generated temporary password shown once");String generated=code.group(1);
        check(!get(admin,"/admin/accounts/new").body().contains(generated),"fresh GET does not reveal prior generated password");
        var newReader=login("binh.tv03021995@mrm.com",generated);
        check(get(newReader,"/reader/home").headers().firstValue("location").orElse("").endsWith("/account/initial-setup"),"actual automatically generated password logs in and reaches mandatory setup");
        var duplicate=post(admin,"/admin/accounts",token,"displayName","Name Kept","role","READER","department",dep.toString(),"citizenId","0000003003","birthDate","1995-02-03","gender","MALE");
        check(duplicate.statusCode()==200 && duplicate.body().contains("Căn cước công dân đã tồn tại") && duplicate.body().contains("value=\"Name Kept\""),"duplicate citizen ID reports inline and retains entered data");
        check(jdbc.queryForObject("SELECT count(*) FROM app_users",Integer.class)==before+1,"duplicate creates no orphan account");
        var missing=post(admin,"/admin/accounts",token,"displayName","Missing Department","role","READER","citizenId","0000003004","birthDate","1995-02-03","gender","MALE");
        check(missing.statusCode()==200 && missing.body().contains("Cần chọn khoa hợp lệ"),"server also rejects absent department");
        String management=get(admin,"/admin/accounts").body();
        check(management.contains(">Trần Văn Bình</a>") && management.contains("<td>binh.tv03021995@mrm.com</td>"),"management links full name and keeps username in separate plain-text cell");
        int total=jdbc.queryForObject("SELECT count(*) FROM app_users",Integer.class);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var results=pool.invokeAll(List.<Callable<Boolean>>of(
                    ()->race("Race Alpha"),()->race("Race Beta")));
            int successful=0;for(var result:results)if(result.get())successful++;
            check(successful==1 && jdbc.queryForObject("SELECT count(*) FROM app_users",Integer.class)==total+1,"concurrent duplicate citizen ID creates exactly one account and no orphan");
        }
    }
    private static boolean race(String name){try{accounts.createAutomatically(name,"READER",dep,null,"0000003099",LocalDate.of(1990,1,1),"MALE","admin1");return true;}catch(BusinessException|org.springframework.dao.DataIntegrityViolationException ex){return false;}}
    private static void profiles()throws Exception {
        var reader=login("reader1",LOGIN);String html=get(reader,"/account/profile/edit").body();check(html.contains("name=\"citizenId\"") && !html.contains("name=\"role\"") && !html.contains("name=\"department\"") && !html.contains("name=\"email\""),"self-edit exposes personal fields without editable email or privilege inputs");
        check(html.contains("Đề nghị thay đổi vai trò") && html.contains("Đề nghị thay đổi khoa") && html.contains("href=\"/account/change-requests\""),"role and department links open protected proposal flow from personal edit");
        String profilePage=get(reader,"/account/profile").body();
        check(!profilePage.contains("href=\"/account/change-requests\"") && !profilePage.substring(profilePage.indexOf("<main"),profilePage.indexOf("</main>")).contains("href=\"/account/password\""),"profile page keeps only personal-edit action and removes separate role-change/menu links");
        long version=jdbc.queryForObject("SELECT version FROM app_users WHERE username='reader1'",Long.class);
        var response=post(reader,"/account/profile/edit",csrf(html),"version",Long.toString(version),"displayName","Bác sĩ đã cập nhật","citizenId","0000003010","birthDate","1990-01-02","gender","MALE","email","new@example.invalid","role","ADMIN","department",other.toString());
        check(response.statusCode()==302 && response.headers().firstValue("location").orElse("").endsWith("/account/profile"),"self profile saves through HTTP");
        check(accounts.ownProfile("reader1").get("full_name").equals("Bác sĩ đã cập nhật") && accounts.ownProfile("reader1").get("role").equals("READER") && accounts.ownProfile("reader1").get("department_id")==null,"injected role/department fields cannot change privileges");
        check(accounts.ownProfile("reader1").get("email")==null,"injected email cannot alter stored email during personal edit");
        check(get(reader,"/account/profile").statusCode()==200,"profile edit refreshes current session version");
        long next=version+1;
        expect(BusinessException.class,()->accounts.updateOwnProfile("reader1",next,"Changed","0000003001",LocalDate.of(1990,1,1),"MALE"));
        check(accounts.ownProfile("reader1").get("full_name").equals("Bác sĩ đã cập nhật"),"duplicate citizen ID rolls back personal name update too");
        expect(BusinessException.class,()->accounts.updateOwnProfile("reader1",version,"Stale","0000003010",LocalDate.of(1990,1,1),"MALE"));
    }
    private static void workflow()throws Exception {
        expect(BusinessException.class,()->changes.submit("reader1","ADMIN",dep,"Invalid elevation",UUID.randomUUID()));
        expect(BusinessException.class,()->changes.submit("reader1","DIRECTOR",dep,"Invalid elevation",UUID.randomUUID()));
        var reader=login("reader1",LOGIN);var director=login("director1",LOGIN);var admin=login("admin1",LOGIN);
        String form=get(reader,"/account/change-requests").body();check(form.contains("Gửi Giám đốc") && !form.contains("value=\"ADMIN\""),"staff proposal form renders with allowed roles");
        UUID token=UUID.randomUUID();UUID id=changes.submit("reader1","ARCHIVIST",dep,"Đề nghị điều chuyển",token);
        check(changes.submit("reader1","ARCHIVIST",dep,"Đề nghị điều chuyển",token).equals(id),"repeated submit token is idempotent");
        expect(BusinessException.class,()->changes.submit("reader1","ARCHIVIST",other,"Another open request",UUID.randomUUID()));
        check(accounts.ownProfile("reader1").get("role").equals("READER"),"submission does not alter privileges");
        check(notices.unread("director1")==1 && notices.unread("admin1")==0 && notices.unread("reader1")==0,"only Director receives submission work, no premature staff result");
        check(get(director,"/director/staff-change-requests/"+id).statusCode()==200,"Director review template renders");
        expect(org.springframework.web.server.ResponseStatusException.class,()->changes.detail(id,"archivist1"));
        expect(org.springframework.web.server.ResponseStatusException.class,()->changes.detail(id,"admin1"));
        expect(org.springframework.security.access.AccessDeniedException.class,()->changes.decide(id,0,true,"",SECRET,"reader1"));
        expect(BusinessException.class,()->changes.decide(id,0,false," ",SECRET,"director1"));
        expect(BusinessException.class,()->changes.decide(id,0,true,"", "wrong", "director1"));
        check(state(id).equals("SUBMITTED"),"invalid decision or secret leaves request submitted");
        expect(BusinessException.class,()->changes.apply(id,0,"admin1"));
        String decisionForm=get(director,"/director/staff-change-requests/"+id).body();
        check(decisionForm.contains("id=\"staff-rejection-reason\" hidden") && decisionForm.contains("id=\"staff-decision-note\"") && decisionForm.contains("disabled"),"Director form initially hides and disables rejection reason");
        check(post(reader,"/director/staff-change-requests/"+id+"/decide",csrf(get(reader,"/account/profile").body()),"version","0","decision","APPROVE","approvalSecret",SECRET).statusCode()==403,"employee cannot call Director decision HTTP route");
        var result=post(director,"/director/staff-change-requests/"+id+"/decide",csrf(decisionForm),"version","0","decision","APPROVE","note","INTERNAL_APPROVAL_NOTE","approvalSecret",SECRET);
        check(result.statusCode()==302 && state(id).equals("APPROVED"),"Director approval HTTP transaction completes");
        check(accounts.ownProfile("reader1").get("role").equals("READER"),"Director approval still does not apply permissions");
        check(notices.unread("director1")==0 && notices.unread("admin1")==1 && notices.unread("reader1")==0,"Director approval notifies Admin only, before actual application");
        check(notices.inbox("reader1",0,false).items().isEmpty(),"no premature approval outcome in employee inbox");
        check(jdbc.queryForObject("SELECT decision_note FROM staff_change_requests WHERE id=?",String.class,id).isEmpty(),"approval ignores injected rejection-note text");
        var adminNotice=notices.inbox("admin1",0,false).items().getFirst();
        check(notices.openPath(adminNotice.id(),"admin1").equals("/admin/staff-change-requests/"+id),"Admin notice links to approved staff request");
        expect(BusinessException.class,()->notices.deleteHandled(adminNotice.id(),"admin1"));
        check(get(admin,"/notifications").statusCode()==200 && get(reader,"/notifications").statusCode()==200,"new staff notifications render for Admin and requester");
        var applyPage=get(admin,"/admin/staff-change-requests/"+id);
        check(applyPage.statusCode()==200 && applyPage.body().contains("Áp dụng vai trò/khoa đã duyệt"),"Admin can review the exact approved change");
        expect(org.springframework.security.access.AccessDeniedException.class,()->changes.apply(id,1,"director1"));
        check(post(admin,"/admin/staff-change-requests/"+id+"/apply",csrf(applyPage.body()),"version","1").statusCode()==302 && state(id).equals("APPLIED"),"Admin applies through protected HTTP endpoint");
        var staffNotice=notices.inbox("reader1",0,false).items().getFirst();
        check(notices.unread("reader1")==1 && staffNotice.message().isEmpty() && staffNotice.title().contains("được phê duyệt") && staffNotice.workInfo()==null,"employee receives approval result only after Admin applies");
        check(jdbc.queryForObject("SELECT e.action FROM notifications n JOIN staff_change_events e ON e.id=n.staff_event_id WHERE n.id=?",String.class,staffNotice.id()).equals("APPLY"),"approval notification is linked to actual Admin application event");
        check(accounts.ownProfile("reader1").get("role").equals("ARCHIVIST") && accounts.ownProfile("reader1").get("department_id").equals(dep),"approved role and department actually applied");
        check(jdbc.queryForObject("SELECT role FROM employees WHERE citizen_id='0000003010'",String.class).equals("ARCHIVIST"),"employee role stays synchronized");
        check(get(reader,"/reader/home").headers().firstValue("location").orElse("").contains("expired"),"old employee session revoked after application");
        var relogged=login("reader1",LOGIN);check(get(relogged,"/archivist/home").statusCode()==200 && get(relogged,"/reader/home").statusCode()==403,"relogin receives changed role only");
        check(notices.inbox("reader1",0,false).items().stream().anyMatch(n->n.id().equals(staffNotice.id())),"staff approval notice remains owned after role change");
        notices.read(staffNotice.id(),"reader1");check(notices.unread("reader1")==0,"reading outcome clears staff notification badge");
        check(notices.unread("admin1")==0 && notices.deleteHandled(adminNotice.id(),"admin1"),"Admin work becomes handled and deletable after applying");
        expect(BusinessException.class,()->changes.apply(id,1,"admin1"));
        check(jdbc.queryForObject("SELECT count(*) FROM notifications n JOIN app_users u ON u.id=n.recipient_id WHERE n.staff_request_id=? AND u.username='reader1'",Integer.class,id)==1,"repeat Admin application cannot send duplicate employee results");
        UUID rejected=changes.submit("archivist1","READER",other,"Xin chuyển khoa",UUID.randomUUID());
        changes.decide(rejected,0,false,"Không phù hợp kế hoạch nhân sự <script>",SECRET,"director1");
        var denied=notices.inbox("archivist1",0,false).items().getFirst();
        check(denied.title().contains("từ chối") && denied.message().contains("Không phù hợp"),"rejection result includes required reason below title");
        var arch=login("archivist1",LOGIN);String inbox=get(arch,"/notifications").body();
        check(inbox.contains("&lt;script&gt;") && !inbox.contains("<script>Không phù hợp"),"rejection text is HTML escaped");
        check(accounts.ownProfile("archivist1").get("role").equals("ARCHIVIST"),"rejection does not change employee role");
        expect(BusinessException.class,()->changes.apply(rejected,1,"admin1"));
        accounts.create("director2","Other Director","DIRECTOR",dep,null,INITIAL,"admin1");accounts.completeInitialSetup("director2",LOGIN,LOGIN,SECRET,SECRET);
        UUID own=changes.submit("director1","READER",dep,"Xin đổi nhiệm vụ",UUID.randomUUID());
        expect(BusinessException.class,()->changes.decide(own,0,true,"",SECRET,"director1"));
        check(changes.queue("director1").stream().noneMatch(q->q.get("id").equals(own)),"Director queue excludes own request");
        check(notices.unread("director2")==1,"another Director receives request instead of self-review");
        UUID stale=changes.submit("archivist1","READER",dep,"Stale baseline",UUID.randomUUID());changes.decide(stale,0,true,"",SECRET,"director1");
        jdbc.update("UPDATE app_users SET department_id=? WHERE username='archivist1'",other);
        expect(BusinessException.class,()->changes.apply(stale,1,"admin1"));
        check(notices.inbox("archivist1",0,false).items().stream().noneMatch(n->n.requestId().equals(stale)),"failed Admin application sends no success outcome");
        check(accounts.ownProfile("archivist1").get("department_id").equals(other),"stale approved change never overwrites a newer department");
        changes.decide(own,0,true,"",SECRET,"director2");
        jdbc.update("UPDATE app_users SET status='DISABLED' WHERE username='director2'");
        expect(BusinessException.class,()->changes.apply(own,1,"admin1"));
        check(accounts.ownProfile("director1").get("role").equals("DIRECTOR"),"last active Director cannot be removed by an approved change");
        jdbc.update("UPDATE app_users SET status='ACTIVE' WHERE username='director2'");
        changes.apply(own,1,"admin1");
        check(accounts.ownProfile("director1").get("role").equals("READER"),"Director change can apply when another active Director remains");
        check(notices.inbox("director1",0,false).items().stream().allMatch(n->n.requestId().equals(own)),"former Director cannot retain other employees' review notices after role change");
    }
    private static String state(UUID id){return jdbc.queryForObject("SELECT state FROM staff_change_requests WHERE id=?",String.class,id);}
    private static HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NEVER).build();}
    private static HttpResponse<String> get(HttpClient c,String path)throws Exception{return c.send(HttpRequest.newBuilder(URI.create(base+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    private static String csrf(String html){var m=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);if(!m.find())throw new AssertionError("CSRF missing");return m.group(1);}
    private static HttpResponse<String> post(HttpClient c,String path,String token,String...pairs)throws Exception {
        var body=new StringBuilder("_csrf="+URLEncoder.encode(token,StandardCharsets.UTF_8));for(int i=0;i<pairs.length;i+=2)body.append("&"+URLEncoder.encode(pairs[i],StandardCharsets.UTF_8)+"="+URLEncoder.encode(pairs[i+1],StandardCharsets.UTF_8));
        return c.send(HttpRequest.newBuilder(URI.create(base+path)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
    }
    private static HttpClient login(String name,String password)throws Exception{var c=browser();var r=post(c,"/login",csrf(get(c,"/login").body()),"username",name,"password",password);check(r.statusCode()==302 && !r.headers().firstValue("location").orElse("").contains("error"),"login "+name);return c;}
}
