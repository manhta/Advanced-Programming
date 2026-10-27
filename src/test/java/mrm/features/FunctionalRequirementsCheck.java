package mrm.features;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import mrm.MedicalRecordApplication;
import mrm.account.AccountService;
import mrm.catalog.*;
import mrm.common.BusinessException;
import mrm.inventory.InventoryService;
import mrm.record.*;
import mrm.report.*;
import mrm.storage.*;
import mrm.workflow.ReadingService;
import mrm.notification.NotificationService;

/** Boots the entire application in a disposable PostgreSQL schema, including real HTTP security. */
public class FunctionalRequirementsCheck {
    private static int checks;
    private static JdbcTemplate jdbc;
    private static RecordService records;
    private static ReadingService reading;
    private static AccountService accounts;
    private static InventoryService inventory;
    private static ReportService reports;
    private static NotificationService notifications;
    private static UUID department,type,policy,deathPolicy,slot,other;
    private static final LocalDate TODAY=ReadingService.today();
    private static final String LOGIN="Synthetic-test-login-2048!";
    private static final String SECRET="Synthetic-approval-only-2048!";
    private static String base;
    public static void main(String[] args)throws Exception {
        String password=System.getenv("DB_PASSWORD");if(password==null || password.isBlank())password=Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$","");
        String url=System.getenv().getOrDefault("DB_URL","jdbc:postgresql://127.0.0.1:5433/mrm"),user=System.getenv().getOrDefault("DB_USERNAME","postgres");
        String schema="fr_check_"+UUID.randomUUID().toString().replace("-","");
        try(var connection=DriverManager.getConnection(url,user,password)) {
            try(var s=connection.createStatement()){s.execute("CREATE SCHEMA "+schema);}
            try(var context=new SpringApplicationBuilder(MedicalRecordApplication.class).run(
                "--spring.profiles.active=dev,seed-users","--server.port=0",
                "--spring.datasource.url="+url+(url.contains("?")?"&":"?")+"currentSchema="+schema,
                "--spring.datasource.username="+user,"--spring.datasource.password="+password,
                "--spring.flyway.schemas="+schema,"--spring.flyway.default-schema="+schema,
                "--SEED_READER_PASSWORD="+LOGIN,"--SEED_ARCHIVIST_PASSWORD="+LOGIN,
                "--SEED_DIRECTOR_PASSWORD="+LOGIN,"--SEED_ADMIN_PASSWORD="+LOGIN)) {
                jdbc=context.getBean(JdbcTemplate.class);records=context.getBean(RecordService.class);reading=context.getBean(ReadingService.class);
                accounts=context.getBean(AccountService.class);inventory=context.getBean(InventoryService.class);reports=context.getBean(ReportService.class);
                notifications=context.getBean(NotificationService.class);
                base="http://localhost:"+((WebServerApplicationContext)context).getWebServer().getPort();
                check(jdbc.queryForObject("SELECT current_schema()",String.class).equals(schema),"all mutations confined to disposable schema");
                check(jdbc.queryForObject("SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1",String.class).equals("26"),"fresh V1-V26 boot succeeds");
                fixture(context.getBean(StorageService.class),context.getBean(CatalogService.class));
                accountChecks();requesterSnapshotChecks();workflowChecks();approvalRoutingChecks();combinedArchivistReviewChecks();batchBorrowingChecks();borrowAvailabilityChecks();notificationChecks(context.getBean(org.springframework.transaction.PlatformTransactionManager.class));archivistDashboardAndCancellationChecks(context.getBean(mrm.authentication.DashboardService.class));employeeChecks();inventoryChecks();reportChecks();httpChecks();requestHistoryChecks();electronicChecks(context.getBean(mrm.electronic.ElectronicRecordService.class));extendedElectronicChecks(context.getBean(mrm.electronic.ElectronicRecordService.class),context.getBean(mrm.authentication.DashboardService.class));birthDateChecks(context.getBean(mrm.electronic.ElectronicRecordService.class));unifiedRecordChecks(context.getBean(mrm.electronic.ElectronicRecordService.class));
                personalMenuAndDirectorChecks(context.getBean(mrm.authentication.DashboardService.class));
                interfaceCleanupChecks();
                notificationLifecycleChecks(context.getBean(mrm.electronic.ElectronicRecordService.class));
                check(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE details LIKE ?",Integer.class,"%"+SECRET+"%")==0,"approval secret never appears in audit details");
                check(jdbc.queryForObject("SELECT COUNT(*) FROM medical_records WHERE record_state='STORED'",Integer.class)>=100,"100+ synthetic records retained for acceptance checks");
                if(args.length>0 && args[0].equals("--preview")){System.out.println("PREVIEW_URL "+base);System.in.read();}
                jdbc.execute("CREATE TABLE data_reset_history(reset_key VARCHAR(64) PRIMARY KEY)");
                jdbc.update("INSERT INTO data_reset_history VALUES('OPERATIONAL_BASELINE')");
                jdbc.update("UPDATE app_users SET username='retired_'||username WHERE username IN ('reader1','archivist1','director1','admin1')");
                int baselineCount=jdbc.queryForObject("SELECT count(*) FROM app_users",Integer.class);
                context.getBean(mrm.devtools.DemoUserInitializer.class).run(new org.springframework.boot.DefaultApplicationArguments());
                context.getBean(org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy.class).migrate(context.getBean(org.flywaydb.core.Flyway.class));
                check(jdbc.queryForObject("SELECT count(*) FROM app_users",Integer.class)==baselineCount,"reset marker prevents demo recreation by both bootstrap and initializer");
                check(jdbc.queryForObject("SELECT count(*) FROM app_users WHERE username IN ('reader1','archivist1','director1','admin1')",Integer.class)==0,"old demo usernames stay absent even with seed-users profile");
            }finally{try(var s=connection.createStatement()){s.execute("DROP SCHEMA "+schema+" CASCADE");}}
            try(var s=connection.prepareStatement("SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name=?)")){s.setString(1,schema);try(var r=s.executeQuery()){r.next();check(!r.getBoolean(1),"temporary schema removed");}}
        }
        System.out.println("PASSED "+checks+" functional requirement checks; email intentionally deferred.");
    }
    private static void fixture(StorageService storage,CatalogService catalog) {
        department=jdbc.queryForObject("SELECT id FROM departments WHERE code='INTERNAL_MEDICINE'",UUID.class);
        type=jdbc.queryForObject("SELECT id FROM record_types WHERE code='INTERNAL_MEDICINE'",UUID.class);
        // Test policies span historic dates, independent of the current calendar day.
        catalog.createPolicy(new RetentionPolicyForm("FR_GENERAL","FR policy",1,10,LocalDate.of(2000,1,1),null,"Synthetic test basis"),"archivist1");
        policy=jdbc.queryForObject("SELECT id FROM retention_policies WHERE code='FR_GENERAL'",UUID.class);catalog.approve(policy,"director1");
        deathPolicy=jdbc.queryForObject("SELECT id FROM retention_policies WHERE code='DECEASED'",UUID.class);catalog.approve(deathPolicy,"director1");
        UUID wh=storage.create(new LocationForm("FR_WAREHOUSE","PRIVATE_STORAGE_MARKER","WAREHOUSE",null,null),"archivist1");
        UUID shelf=storage.create(new LocationForm("FR_SHELF","FR shelf","SHELF",wh,null),"archivist1");
        slot=storage.create(new LocationForm("FR_SLOT","FR source","COMPARTMENT",shelf,200),"archivist1");
        other=storage.create(new LocationForm("FR_OTHER","FR target","COMPARTMENT",shelf,200),"archivist1");
        for(int i=0;i<100;i++)create("BASE_"+i,false);
        check(records.search(new RecordFilter("FR Patient",null,null,null,null,null,null,0,100,"name"),false).total()==100,"100-record search returns complete count");
        accounts.setApprovalSecret("director1",LOGIN,SECRET);
    }
    private static UUID create(String code,boolean deceased) {
        return records.create(new RecordForm(UUID.randomUUID(),0L,null,"FR_MED_"+code,"FR_PAT_"+code,"FR Patient "+code,1980,"UNKNOWN",department,type,deceased,
            TODAY.minusDays(3),TODAY.minusDays(2),TODAY,List.of(deceased?deathPolicy:policy),slot,"NORMAL","Synthetic acceptance dataset","PRIVATE_RECORD_MARKER",null),"STORE","archivist1");
    }
    private static long version(UUID id){return jdbc.queryForObject("SELECT version FROM reading_requests WHERE id=?",Long.class,id);}
    private static String state(UUID id){return jdbc.queryForObject("SELECT state FROM reading_requests WHERE id=?",String.class,id);}
    private static UUID session(UUID id,LocalDate day){return jdbc.queryForObject("SELECT id FROM reading_sessions WHERE request_id=? AND reading_on=?",UUID.class,id,day);}
    private static UUID submit(UUID record){return reading.submit(record,"Research with synthetic data",UUID.randomUUID(),"reader1");}
    private static void approve(UUID id){
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT requires_director FROM reading_requests WHERE id=?",Boolean.class,id))){
            reading.review(id,version(id),"PENDING_APPROVAL","Checked","archivist1");reading.decide(id,version(id),true,"Approved",SECRET,"director1");
        }else reading.review(id,version(id),"APPROVED","Approved by Archivist","archivist1");
    }
    private static UUID directorRequest(UUID record){
        UUID death=create("ROUTE_COMPANION_"+UUID.randomUUID(),true);
        var ids=reading.submitBatch(List.of(record,death),"Mixed batch requires Director",UUID.randomUUID(),"reader1");
        return ids.stream().filter(id->jdbc.queryForObject("SELECT record_id FROM reading_requests WHERE id=?",UUID.class,id).equals(record)).findFirst().orElseThrow();
    }
    private static void confirm(UUID id){approve(id);reading.confirm(id,version(id),TODAY,"Reading room A; bring staff ID","archivist1");}
    private static void accountChecks(){
        accounts.create("an.nv12041990@mrm.com","Email-shaped account","READER",department,null,LOGIN,"admin1");
        check(accounts.list().stream().anyMatch(x->x.get("username").equals("an.nv12041990@mrm.com")),"email-shaped username accepted by service and database");
        expect(BusinessException.class,()->accounts.create("invalid@@mrm.com","Invalid","READER",department,null,LOGIN,"admin1"));
        accounts.create("fr_reader2","Reader Two","READER",department,null,LOGIN,"admin1");
        var a=accounts.list().stream().filter(x->x.get("username").equals("fr_reader2")).findFirst().orElseThrow();
        check(a.get("role").equals("READER"),"admin creates single-role account");
        expect(org.springframework.security.access.AccessDeniedException.class,()->accounts.create("forbidden","No","READER",null,null,LOGIN,"reader1"));
        expect(BusinessException.class,()->accounts.setApprovalSecret("director1",LOGIN,LOGIN));
        expect(BusinessException.class,()->accounts.setApprovalSecret("director1","wrong",SECRET));
        accounts.update((UUID)a.get("id"),0,"Reader Two","READER","LOCKED",department,null,null,"admin1");
        check(jdbc.queryForObject("SELECT status FROM app_users WHERE username='fr_reader2'",String.class).equals("LOCKED"),"account lock persisted");
        expect(BusinessException.class,()->accounts.update((UUID)a.get("id"),0,"Stale","READER","ACTIVE",department,null,null,"admin1"));
        accounts.update((UUID)a.get("id"),1,"Reader Two","READER","ACTIVE",department,null,LOGIN,"admin1");
        var admin=accounts.list().stream().filter(x->x.get("username").equals("admin1")).findFirst().orElseThrow();
        expect(BusinessException.class,()->accounts.update((UUID)admin.get("id"),0,"Admin","READER","ACTIVE",null,null,null,"admin1"));
    }
    private static void requesterSnapshotChecks() {
        accounts.create("fr_snapshot_reader","Người xin đọc ban đầu","READER",department,null,LOGIN,"admin1");
        UUID record=create("REQUESTER_SNAPSHOT",false);
        UUID request=reading.submit(record,"Snapshot test",UUID.randomUUID(),"fr_snapshot_reader");
        var person=accounts.list().stream().filter(a->a.get("username").equals("fr_snapshot_reader")).findFirst().orElseThrow();
        check(jdbc.queryForObject("SELECT requester_id FROM reading_requests WHERE id=?",UUID.class,request).equals(person.get("id")),"request stores real authenticated requester ID");
        check(jdbc.queryForObject("SELECT requester_name_snapshot FROM reading_requests WHERE id=?",String.class,request).equals("fr_snapshot_reader"),"request stores authenticated requester login username");
        accounts.update((UUID)person.get("id"),0,"Tên tài khoản đã thay đổi","READER","ACTIVE",department,null,null,"admin1");
        check(reading.detail(request,"fr_snapshot_reader").get("requester_name_snapshot").equals("fr_snapshot_reader"),"account rename preserves original request username");
        check(reading.list("fr_snapshot_reader").getFirst().get("requester_name_snapshot").equals("fr_snapshot_reader"),"request list returns original requester username");
        reading.amend(request,version(request),"Updated purpose","fr_snapshot_reader");
        check(reading.detail(request,"fr_snapshot_reader").get("requester_name_snapshot").equals("fr_snapshot_reader"),"request amendment preserves original requester identity");
        UUID second=reading.submit(create("REQUESTER_SNAPSHOT_NEW",false),"New request",UUID.randomUUID(),"fr_snapshot_reader");
        check(reading.detail(second,"fr_snapshot_reader").get("requester_name_snapshot").equals("fr_snapshot_reader"),"new request uses login username instead of renamed display name");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM reading_sessions s JOIN reading_requests q ON q.id=s.request_id WHERE q.id=? AND q.requester_id=?",Integer.class,request,person.get("id"))==0,"Reader submission does not assign a reading session");
    }
    private static void workflowChecks()throws Exception {
        UUID r=create("WORKFLOW",true),token=UUID.randomUUID();
        UUID id=reading.submit(r,"Study",token,"reader1");
        check(reading.submit(r,"Study",token,"reader1").equals(id),"request token replay creates one request");
        expect(BusinessException.class,()->reading.submit(r,"Changed",token,"reader1"));
        expect(org.springframework.web.server.ResponseStatusException.class,()->reading.detail(id,"fr_reader2"));
        reading.review(id,0,"NEEDS_INFO","Clarify purpose","archivist1");check(state(id).equals("NEEDS_INFO"),"archivist returns for additional information");
        reading.amend(id,version(id),"Teaching","reader1");
        check(state(id).equals("SUBMITTED") && jdbc.queryForObject("SELECT revision FROM reading_requests WHERE id=?",Integer.class,id)==2,"amend creates new revision and resets review");
        reading.review(id,version(id),"PENDING_APPROVAL","Checked","archivist1");
        expect(BusinessException.class,()->reading.decide(id,version(id),true,"Approved","wrong","director1"));
        check(state(id).equals("PENDING_APPROVAL"),"wrong second password leaves decision unchanged");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action='APPROVAL_VERIFY' AND outcome='FAILURE'",Integer.class)==1,"failed step-up audit survives rollback");
        reading.decide(id,version(id),true,"Approved",SECRET,"director1");
        check(state(id).equals("APPROVED"),"director approves with separate password");
        expect(BusinessException.class,()->reading.handover(UUID.randomUUID(),version(id),"Normal","archivist1"));
        reading.confirm(id,version(id),TODAY,"Reading room A","archivist1");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM reading_sessions WHERE request_id=? AND state='CONFIRMED'",Integer.class,id)==1,"Archivist assigns one pickup day after approval");
        UUID future=submit(create("FUTURE_PICKUP",false));approve(future);reading.confirm(future,version(future),TODAY.plusDays(1),"Future pickup","archivist1");
        expect(BusinessException.class,()->reading.handover(session(future,TODAY.plusDays(1)),version(future),"Normal","archivist1"));
        long previous=version(id);reading.handover(session(id,TODAY),previous,"Normal pages counted","archivist1");
        check(records.details(r).physicalStatus().equals("READING"),"handover atomically sets physical READING");
        expect(BusinessException.class,()->reading.handover(session(id,TODAY),previous,"Again","archivist1"));
        expect(BusinessException.class,()->records.move(r,other,records.details(r).form().version(),UUID.randomUUID(),"Blocked","archivist1"));
        expect(BusinessException.class,()->reading.cancel(id,version(id),"Cancel","reader1"));
        reading.receive(session(id,TODAY),version(id),other,"NORMAL","Returned complete","archivist1");
        check(records.details(r).physicalStatus().equals("IN_STORAGE") && records.details(r).form().locationId().equals(other),"return updates status and actual return compartment");
        check(records.locationHistory(r).size()==2,"return to different compartment writes movement history");
        expect(BusinessException.class,()->reading.amend(id,version(id),"Changed","reader1"));
        reading.cancel(id,version(id),"Cancel remaining day","archivist1");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM reading_sessions WHERE request_id=? AND state='RETURNED'",Integer.class,id)==1,"cancel preserves completed daily session");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM reading_sessions WHERE request_id=? AND state='CANCELLED'",Integer.class,id)==0,"cancel does not invent another pickup session");
        UUID death=create("DEATH",true);UUID deathRequest=submit(death);check(Boolean.TRUE.equals(reading.detail(deathRequest,"reader1").get("requires_director")),"deceased submission routes to Director");reading.cancel(deathRequest,version(deathRequest),"Release fixture","reader1");
        expect(org.springframework.security.access.AccessDeniedException.class,()->reading.submit(r,"Purpose",UUID.randomUUID(),"archivist1"));
        UUID reject=submit(create("REJECT",true));reading.review(reject,0,"PENDING_APPROVAL","Checked","archivist1");reading.decide(reject,version(reject),false,"Not appropriate",SECRET,"director1");
        check(state(reject).equals("REJECTED"),"rejection records state and reason");
        UUID amend=submit(create("AMEND_APPROVED",false));confirm(amend);reading.amend(amend,version(amend),"New purpose","reader1");
        check(state(amend).equals("SUBMITTED") && jdbc.queryForObject("SELECT approver_id IS NULL FROM reading_requests WHERE id=?",Boolean.class,amend),"editing confirmed request invalidates old approval");
        UUID conflictRecord=create("CONFLICT",false);
        var start=new CountDownLatch(1);try(var executor=Executors.newFixedThreadPool(2)){
            var tasks=List.of("reader1","fr_reader2").stream().map(user->executor.submit(()->{start.await();try{reading.submit(conflictRecord,"Concurrent borrow",UUID.randomUUID(),user);return true;}catch(BusinessException ex){return false;}})).toList();
            start.countDown();int wins=0;for(var task:tasks)if(task.get(30,TimeUnit.SECONDS))wins++;
            check(wins==1 && jdbc.queryForObject("SELECT COUNT(*) FROM reading_requests WHERE record_id=?",Integer.class,conflictRecord)==1,"concurrent Readers cannot register the same free record twice");
        }
        UUID lateRecord=create("LATE",false),late=submit(lateRecord);confirm(late);UUID lateSession=session(late,TODAY);reading.handover(lateSession,version(late),"Normal","archivist1");
        jdbc.update("UPDATE reading_requests SET registered_on=? WHERE id=?",TODAY.minusDays(1),late);
        jdbc.update("UPDATE reading_sessions SET reading_on=? WHERE id=?",TODAY.minusDays(1),lateSession);
        expect(BusinessException.class,()->reading.receive(lateSession,version(late),slot,"NORMAL","Late","archivist1"));
        reading.exception(lateSession,version(late),"Synthetic overdue: contact reader",false,"archivist1");
        check(records.details(lateRecord).physicalStatus().equals("READING"),"overdue exception never pretends record was returned");
        reading.receive(lateSession,version(late),slot,"DAMAGED","Torn page found","archivist1");
        check(records.details(lateRecord).physicalStatus().equals("UNAVAILABLE"),"damaged return blocks further access");
        UUID absent=submit(create("ABSENT",false));confirm(absent);reading.exception(session(absent,TODAY),version(absent),"Reader did not attend",true,"archivist1");
        check(jdbc.queryForObject("SELECT state FROM reading_sessions WHERE request_id=?",String.class,absent).equals("NO_SHOW"),"no-show retained without false handover");
        UUID self=submit(create("SELF",true));reading.review(self,0,"PENDING_APPROVAL","Checked","archivist1");
        jdbc.update("UPDATE reading_requests SET requester_id=(SELECT id FROM app_users WHERE username='director1') WHERE id=?",self);
        expect(BusinessException.class,()->reading.decide(self,version(self),true,"Self approval",SECRET,"director1"));
        accounts.create("fr_director2","Second director","DIRECTOR",null,null,LOGIN,"admin1");accounts.setApprovalSecret("fr_director2",LOGIN,SECRET);
        UUID locked=submit(create("STEP_UP_LOCK",true));reading.review(locked,0,"PENDING_APPROVAL","Checked","archivist1");
        for(int i=0;i<5;i++)expect(BusinessException.class,()->reading.decide(locked,version(locked),true,"Approve","wrong","fr_director2"));
        expect(BusinessException.class,()->reading.decide(locked,version(locked),true,"Approve",SECRET,"fr_director2"));
        check(state(locked).equals("PENDING_APPROVAL"),"five failed step-up attempts block even correct secret during lock window");
    }
    private static void approvalRoutingChecks()throws Exception {
        UUID normal=create("ROUTE_NORMAL",false);
        UUID q=reading.submit(normal,"",UUID.randomUUID(),"reader1");
        check(!Boolean.TRUE.equals(reading.detail(q,"reader1").get("requires_director")),"ordinary batch is approved by Archivist");
        check(mrm.workflow.ReadingProgress.nodes(reading.detail(q,"reader1")).stream().noneMatch(n->n.title().contains("Giám đốc")),"ordinary timeline omits Director stage");
        expect(BusinessException.class,()->reading.review(q,version(q),"PENDING_APPROVAL","Forged escalation","archivist1"));
        approve(q);
        check(state(q).equals("APPROVED") && jdbc.queryForObject("SELECT u.username FROM reading_requests r JOIN app_users u ON u.id=r.approver_id WHERE r.id=?",String.class,q).equals("archivist1"),"Archivist decision records actual approver");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE request_id=? AND recipient_role='DIRECTOR'",Integer.class,q)==0,"ordinary approval sends no Director notification");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE request_id=? AND recipient_role='READER'",Integer.class,q)==1,"Archivist approval notifies requester");
        expect(BusinessException.class,()->reading.decide(q,version(q),true,"Forged",SECRET,"director1"));
        UUID death=create("ROUTE_DEATH",true),normal2=create("ROUTE_MIXED",false),token=UUID.randomUUID();
        expect(BusinessException.class,()->reading.submitBatch(List.of(death,normal2),"",token,"reader1"));
        check(jdbc.queryForObject("SELECT COUNT(*) FROM reading_request_batches WHERE submit_token=?",Integer.class,token)==0,"missing reason rolls back entire deceased batch");
        expect(BusinessException.class,()->reading.submit(death,"",UUID.randomUUID(),"reader1"));
        var mixed=reading.submitBatch(List.of(death,normal2),"Clinical comparison",token,"reader1");
        check(reading.submitBatch(List.of(normal2,death),"Clinical comparison",token,"reader1").equals(mixed),"mixed batch retry preserves route and request IDs");
        for(UUID child:mixed){
            check(Boolean.TRUE.equals(reading.detail(child,"reader1").get("requires_director")),"every mixed child requires Director including ordinary record");
            expect(BusinessException.class,()->reading.review(child,version(child),"APPROVED","Bypass","archivist1"));
            expect(BusinessException.class,()->reading.review(child,version(child),"REJECTED","Bypass","archivist1"));
            expect(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE reading_requests SET requires_director=FALSE WHERE id=?",child));
            approve(child);reading.confirm(child,version(child),TODAY,"Approved mixed batch","archivist1");
            check(state(child).equals("CONFIRMED"),"Director-approved mixed child can receive a pickup schedule");
        }
        UUID deceasedRequest=mixed.stream().filter(id->jdbc.queryForObject("SELECT record_id FROM reading_requests WHERE id=?",UUID.class,id).equals(death)).findFirst().orElseThrow();
        reading.handover(session(deceasedRequest,TODAY),version(deceasedRequest),"Complete","archivist1");
        reading.receive(session(deceasedRequest,TODAY),version(deceasedRequest),slot,"NORMAL","Returned","archivist1");
        check(records.details(death).physicalStatus().equals("IN_STORAGE"),"deceased record completes authorized handover and return");
        var reader=login("reader1");
        var form=get(reader,"/reader/requests/new?recordId="+death);
        check(form.statusCode()==200 && form.body().contains("data-deceased=\"true\"") && Pattern.compile("<textarea[^>]*name=\"purpose\"[^>]*required",Pattern.DOTALL).matcher(form.body()).find(),"preselected deceased record requires reason without depending on JavaScript");
        UUID cancelledDeath=create("ROUTE_CANCEL_DEATH",true),keptNormal=create("ROUTE_KEEP_NORMAL",false);
        var retained=reading.submitBatch(List.of(cancelledDeath,keptNormal),"Keep batch route",UUID.randomUUID(),"reader1");
        for(UUID child:retained){
            if(jdbc.queryForObject("SELECT record_id FROM reading_requests WHERE id=?",UUID.class,child).equals(cancelledDeath))reading.cancel(child,version(child),"Cancel deceased child","reader1");
            else expect(BusinessException.class,()->reading.review(child,version(child),"APPROVED","Downgrade after cancellation","archivist1"));
        }
        UUID changed=create("ROUTE_CHANGED",false),changedRequest=submit(changed);
        jdbc.update("UPDATE medical_records SET deceased=TRUE WHERE id=?",changed);
        expect(BusinessException.class,()->reading.review(changedRequest,version(changedRequest),"APPROVED","Must resubmit","archivist1"));
        check(state(changedRequest).equals("SUBMITTED"),"later deceased correction cannot bypass Director routing");
        UUID pending=submit(create("ROUTE_NORMAL_UI",false));var arch=login("archivist1");
        String view=get(arch,"/archivist/requests/"+pending).body();
        check(view.contains("value=\"APPROVED\"") && !view.contains("value=\"PENDING_APPROVAL\""),"ordinary review UI offers Archivist approval instead of forwarding");
        var drafts=get(arch,"/archivist/records?scope=DRAFTS");
        check(drafts.statusCode()==200 && drafts.body().contains("chưa xác nhận nhập kho"),"paper drafts page explains unpublished temporary records");
    }
    private static void combinedArchivistReviewChecks()throws Exception {
        UUID ordinary=submit(create("COMBINED_APPROVAL",false));
        reading.reviewAndSchedule(ordinary,0,"APPROVED","",TODAY,"Phòng đọc A",true,"archivist1");
        check(state(ordinary).equals("CONFIRMED") && version(ordinary)==2 && session(ordinary,TODAY)!=null,"one Archivist submission approves and schedules atomically with optional note");
        check(reading.detail(ordinary,"reader1").get("confirmation_note").equals("Phòng đọc A"),"combined form stores the pickup room for Reader timeline");
        UUID invalid=submit(create("COMBINED_INVALID",false));
        expect(BusinessException.class,()->reading.reviewAndSchedule(invalid,0,"APPROVED","",TODAY,"Room",false,"archivist1"));
        expect(BusinessException.class,()->reading.reviewAndSchedule(invalid,0,"APPROVED","",null,"Room",true,"archivist1"));
        expect(BusinessException.class,()->reading.reviewAndSchedule(invalid,0,"APPROVED","",TODAY,"",true,"archivist1"));
        expect(BusinessException.class,()->reading.reviewAndSchedule(invalid,0,"APPROVED","",TODAY.plusDays(3),"Room",true,"archivist1"));
        check(state(invalid).equals("SUBMITTED") && version(invalid)==0 && jdbc.queryForObject("SELECT COUNT(*) FROM reading_sessions WHERE request_id=?",Integer.class,invalid)==0,"invalid schedule rolls back approval and session changes");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM request_events WHERE request_id=?",Integer.class,invalid)==1 && jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE request_id=? AND recipient_role='READER'",Integer.class,invalid)==0,"failed combined approval leaves no phantom history or result notice");
        expect(BusinessException.class,()->reading.reviewAndSchedule(invalid,0,"NEEDS_INFO","",null,null,false,"archivist1"));
        expect(BusinessException.class,()->reading.reviewAndSchedule(invalid,0,"REJECTED","",null,null,false,"archivist1"));
        expect(BusinessException.class,()->reading.reviewAndSchedule(invalid,0,"NEEDS_INFO","Clarify",TODAY,"Room",false,"archivist1"));
        expect(org.springframework.security.access.AccessDeniedException.class,()->reading.reviewAndSchedule(invalid,0,"APPROVED","",TODAY,"Room",true,"reader1"));
        reading.reviewAndSchedule(invalid,0,"NEEDS_INFO","Cần bổ sung mục đích",null,null,false,"archivist1");
        check(state(invalid).equals("NEEDS_INFO"),"return-for-information requires and stores its explanation without scheduling");
        reading.amend(invalid,version(invalid),"Purpose clarified","reader1");
        reading.reviewAndSchedule(invalid,version(invalid),"REJECTED","Lý do từ chối",null,null,false,"archivist1");
        check(state(invalid).equals("REJECTED") && reading.detail(invalid,"reader1").get("decision_note").equals("Lý do từ chối"),"rejection requires a reason and closes the request without a schedule");
        var arch=login("archivist1");var reader=login("reader1");
        UUID special=submit(create("COMBINED_DECEASED",true));
        String specialHtml=get(arch,"/archivist/requests/"+special).body();
        check(specialHtml.contains("value=\"PENDING_APPROVAL\"") && !specialHtml.contains("data-review-schedule") && !specialHtml.contains("name=\"pickupOn\""),"deceased batch review shows forwarding without date or room fields");
        reading.reviewAndSchedule(special,0,"PENDING_APPROVAL","",null,null,false,"archivist1");
        check(state(special).equals("PENDING_APPROVAL") && jdbc.queryForObject("SELECT COUNT(*) FROM reading_sessions WHERE request_id=?",Integer.class,special)==0,"forwarding with optional note creates no premature pickup schedule");
        reading.decide(special,version(special),true,"Approved",SECRET,"director1");
        check(get(arch,"/archivist/requests/"+special).body().contains("name=\"pickupOn\""),"Archivist can schedule deceased records only after Director approval");
        UUID http=submit(create("COMBINED_HTTP",false));
        String form=get(arch,"/archivist/requests/"+http).body();
        check(!form.contains("Lịch nhận và giao–trả") && !form.contains("Hủy phần chưa thực hiện") && !form.contains("<h2>Lịch sử xử lý</h2>"),"Archivist approval page removes circulation, cancellation and processing history blocks");
        String token=csrf(form);
        check(post(arch,"/archivist/requests/"+http+"/review",null,"version",0,"result","APPROVED","pickupOn",TODAY,"pickupRoom","Room HTTP","approvalConfirmed",true).statusCode()==403,"combined review still requires CSRF");
        check(post(arch,"/archivist/requests/"+http+"/review",token,"version",0,"result","APPROVED","note","","pickupOn",TODAY,"pickupRoom","Room HTTP","approvalConfirmed",true).statusCode()==302 && state(http).equals("CONFIRMED"),"HTTP combined review accepts an empty approval note and confirms pickup");
        long events=jdbc.queryForObject("SELECT COUNT(*) FROM request_events WHERE request_id=?",Long.class,http);
        post(arch,"/archivist/requests/"+http+"/review",token,"version",0,"result","APPROVED","pickupOn",TODAY,"pickupRoom","Room HTTP","approvalConfirmed",true);
        check(jdbc.queryForObject("SELECT COUNT(*) FROM request_events WHERE request_id=?",Long.class,http)==events,"duplicate review POST cannot create duplicate approval or schedule");
        check(get(arch,"/archivist/requests/"+http+"/circulation").statusCode()==200 && get(reader,"/archivist/requests/"+http+"/circulation").statusCode()==403,"separate circulation page remains usable only by Archivist");
        check(get(reader,"/reader/requests/"+http).body().contains("Room HTTP"),"combined pickup details flow through to Reader progress");
    }
    private static void batchBorrowingChecks() {
        UUID first=create("BATCH_ONE",false),second=create("BATCH_TWO",false),token=UUID.randomUUID();
        var batch=reading.submitBatch(List.of(first,second),"Multiple files style selection",token,"reader1");
        check(batch.size()==2 && reading.submitBatch(List.of(second,first),"Multiple files style selection",token,"reader1").equals(batch),"batch token replay with same selected set returns same requests");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM reading_requests q JOIN reading_request_batches b ON b.id=q.batch_id WHERE b.submit_token=?",Integer.class,token)==2,"one registration creates independently tracked child requests");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM reading_sessions s JOIN reading_requests q ON q.id=s.request_id WHERE q.id IN (?,?)",Integer.class,batch.get(0),batch.get(1))==0,"Reader registration never creates dates or sessions");
        check(jdbc.queryForObject("SELECT pickup_deadline=registered_on+2 FROM reading_requests WHERE id=?",Boolean.class,batch.getFirst()),"pickup deadline is two calendar days after registration");
        expect(BusinessException.class,()->reading.submitBatch(List.of(first),"Multiple files style selection",token,"reader1"));
        expect(BusinessException.class,()->reading.submitBatch(List.of(first,first),"Duplicate selections",UUID.randomUUID(),"reader1"));
        expect(BusinessException.class,()->reading.submitBatch(List.of(),"Empty selections",UUID.randomUUID(),"reader1"));
        UUID invalid=create("BATCH_INVALID",false),badToken=UUID.randomUUID();
        jdbc.update("UPDATE medical_records SET quality_status='DAMAGED' WHERE id=?",invalid);
        expect(BusinessException.class,()->reading.submitBatch(List.of(create("BATCH_ROLLBACK_VALID",false),invalid),"Rollback all",badToken,"reader1"));
        check(jdbc.queryForObject("SELECT COUNT(*) FROM reading_request_batches WHERE submit_token=?",Integer.class,badToken)==0,"invalid selected record leaves no partial registration batch");
        UUID q=batch.getFirst();approve(q);
        expect(org.springframework.security.access.AccessDeniedException.class,()->reading.confirm(q,version(q),TODAY,"Reader schedule attempt","reader1"));
        expect(BusinessException.class,()->reading.confirm(q,version(q),TODAY.plusDays(3),"Outside deadline","archivist1"));
        expect(BusinessException.class,()->reading.confirm(q,version(q),TODAY.minusDays(1),"Past date","archivist1"));
        check(state(q).equals("APPROVED") && jdbc.queryForObject("SELECT COUNT(*) FROM reading_sessions WHERE request_id=?",Integer.class,q)==0,"invalid Archivist pickup dates roll back without sessions");
        reading.confirm(q,version(q),TODAY.plusDays(2),"Deadline pickup","archivist1");
        check(state(q).equals("CONFIRMED") && session(q,TODAY.plusDays(2))!=null,"Archivist may choose deadline day inclusive");
        expect(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("UPDATE reading_sessions SET reading_on=? WHERE request_id=?",TODAY.plusDays(3),q));
        UUID expired=batch.get(1);approve(expired);
        jdbc.update("UPDATE reading_requests SET registered_on=? WHERE id=?",TODAY.minusDays(3),expired);
        expect(BusinessException.class,()->reading.confirm(expired,version(expired),TODAY,"Expired request","archivist1"));
        check(!records.readerMayView((UUID)reading.detail(expired,"reader1").get("record_id"),"reader1"),"unscheduled approval expires at original pickup deadline");
        long before=notifications.unread("archivist1");reading.amend(q,version(q),"Revise purpose only","reader1");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM reading_sessions WHERE request_id=?",Integer.class,q)==0 && !records.readerMayView((UUID)reading.detail(q,"reader1").get("record_id"),"reader1"),"amend clears assigned pickup and approval");
        check(notifications.unread("archivist1")==before+1,"amended borrowing request notifies Archivist");
        check(mrm.workflow.ReadingProgress.nodes(reading.detail(q,"reader1")).stream().noneMatch(node->node.pickupDates()!=null),"amended request does not display a stale pickup schedule");
        UUID legacy=submit(create("LEGACY_SCHEDULE",false));
        jdbc.update("UPDATE reading_requests SET batch_id=NULL,scheduling_mode='LEGACY',registered_on=? WHERE id=?",TODAY.minusDays(10),legacy);
        jdbc.update("INSERT INTO reading_sessions(request_id,record_id,reading_on,state) SELECT id,record_id,?,'REQUESTED' FROM reading_requests WHERE id=?",TODAY.plusDays(5),legacy);
        approve(legacy);reading.confirm(legacy,version(legacy),null,"Preserve pre-upgrade schedule","archivist1");
        check(state(legacy).equals("CONFIRMED") && session(legacy,TODAY.plusDays(5))!=null,"pre-upgrade LEGACY schedule is preserved without retroactive two-day limit");
    }
    private static void borrowAvailabilityChecks() {
        UUID record=create("FREE_PICKER",false);
        check(records.borrowCandidates("FR_MED_FREE_PICKER",0,"reader1").items().stream().anyMatch(r->r.id().equals(record)),"unregistered available record appears in picker");
        var preview=records.borrowPreview(record,"reader1");
        check(preview.medicalRecordNumber().equals("FR_MED_FREE_PICKER") && preview.departmentName()!=null && preview.recordTypeName()!=null && preview.admissionDate()!=null,"expandable picker preview includes allowed identity and treatment dates");
        UUID request=submit(record);
        check(records.borrowCandidates("FR_MED_FREE_PICKER",0,"reader1").total()==0 && records.borrowCandidates("FR_MED_FREE_PICKER",0,"fr_reader2").total()==0,"pending registration hides record from every Reader");
        expect(BusinessException.class,()->records.borrowPreview(record,"fr_reader2"));
        expect(BusinessException.class,()->reading.submit(record,"Duplicate new token",UUID.randomUUID(),"reader1"));
        reading.review(request,version(request),"NEEDS_INFO","Clarify","archivist1");
        check(records.borrowCandidates("FR_MED_FREE_PICKER",0,"reader1").total()==0,"needs-info request continues reserving the record");
        reading.cancel(request,version(request),"Release pending request","reader1");
        check(records.borrowCandidates("FR_MED_FREE_PICKER",0,"reader1").total()==1,"cancellation releases an available record");
        UUID rejected=submit(record);reading.review(rejected,version(rejected),"REJECTED","Rejected","archivist1");
        check(records.borrowCandidates("FR_MED_FREE_PICKER",0,"reader1").total()==1,"rejection releases the record");
        UUID approved=submit(record);approve(approved);
        check(records.borrowCandidates("FR_MED_FREE_PICKER",0,"reader1").total()==0,"approved unscheduled registration remains reserved");
        reading.confirm(approved,version(approved),TODAY,"Room","archivist1");
        check(records.borrowCandidates("FR_MED_FREE_PICKER",0,"reader1").total()==0,"confirmed uncollected record remains reserved");
        reading.handover(session(approved,TODAY),version(approved),"Normal","archivist1");
        check(records.borrowCandidates("FR_MED_FREE_PICKER",0,"reader1").total()==0,"physically borrowed record is excluded");
        reading.receive(session(approved,TODAY),version(approved),slot,"NORMAL","Returned","archivist1");
        check(records.borrowCandidates("FR_MED_FREE_PICKER",0,"reader1").total()==1,"fully returned available record can be selected again");
        UUID stale=submit(create("STALE_PICKER",false));
        jdbc.update("UPDATE reading_requests SET registered_on=? WHERE id=?",TODAY.minusDays(3),stale);
        check(records.borrowCandidates("FR_MED_STALE_PICKER",0,"reader1").total()==0,"expired unresolved request stays reserved until explicitly closed");
    }
    private static void notificationChecks(org.springframework.transaction.PlatformTransactionManager manager){
        long staffBefore=notifications.unread("archivist1");
        UUID id=submit(create("NOTICE",true));
        check(notifications.unread("archivist1")==staffBefore+1,"Reader submission notifies active Archivist");
        UUID staffNotice=jdbc.queryForObject("SELECT id FROM notifications WHERE request_id=? AND recipient_role='ARCHIVIST'",UUID.class,id);
        expect(org.springframework.web.server.ResponseStatusException.class,()->notifications.read(staffNotice,"reader1"));
        notifications.read(staffNotice,"archivist1");
        check(jdbc.queryForObject("SELECT read_at IS NOT NULL FROM notifications WHERE id=?",Boolean.class,staffNotice),"Archivist can read own work notification");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE recipient_role='READER' AND request_id=?",Integer.class,id)==0,"no result notification before a result exists");
        reading.review(id,version(id),"NEEDS_INFO","Bổ sung mục đích <script>alert(1)</script>","archivist1");
        UUID first=jdbc.queryForObject("SELECT id FROM notifications WHERE recipient_role='READER' AND request_id=?",UUID.class,id);
        check(jdbc.queryForObject("SELECT title FROM notifications WHERE id=?",String.class,first).equals("Yêu cầu đọc cần bổ sung"),"needs-info creates correctly titled internal notice");
        check(notifications.inbox("reader1",0,false).items().stream().anyMatch(n->n.id().equals(first)),"notification routed to request owner");
        check(notifications.inbox("fr_reader2",0,false).items().stream().noneMatch(n->n.id().equals(first)),"other reader cannot list owner's notice");
        expect(org.springframework.web.server.ResponseStatusException.class,()->notifications.read(first,"fr_reader2"));
        check(jdbc.queryForObject("SELECT read_at IS NULL FROM notifications WHERE id=?",Boolean.class,first),"unauthorized read cannot mutate read state");
        long unread=notifications.unread("reader1");notifications.read(first,"reader1");
        check(notifications.unread("reader1")==unread-1,"mark-read decreases own unread badge");
        var readAt=jdbc.queryForObject("SELECT read_at FROM notifications WHERE id=?",java.sql.Timestamp.class,first);notifications.read(first,"reader1");
        check(readAt.equals(jdbc.queryForObject("SELECT read_at FROM notifications WHERE id=?",java.sql.Timestamp.class,first)),"mark-read is idempotent and preserves timestamp");
        reading.amend(id,version(id),"Complete purpose","reader1");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE request_id=? AND recipient_role='ARCHIVIST'",Integer.class,id)==2,"resubmitted revision notifies Archivist again");
        var tx=new org.springframework.transaction.support.TransactionTemplate(manager);
        expect(BusinessException.class,()->tx.executeWithoutResult(status->{reading.review(id,version(id),"NEEDS_INFO","Rolled-back notice","archivist1");throw new BusinessException("Force rollback");}));
        check(state(id).equals("SUBMITTED") && jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE recipient_role='READER' AND request_id=?",Integer.class,id)==1,"failed transaction rolls back decision, event and notification together");
        approve(id);reading.confirm(id,version(id),TODAY,"Room A; bring identification","archivist1");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE request_id=? AND recipient_role='DIRECTOR'",Integer.class,id)==2,"Archivist submission notifies both active Directors");
        notifications.readAll("archivist1");check(notifications.unread("archivist1")==0 && notifications.unread("director1")>0,"staff mark-all-read preserves other role notifications");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE recipient_role='READER' AND request_id=?",Integer.class,id)==3,"approval and schedule confirmation each create notice");
        long current=version(id);expect(BusinessException.class,()->reading.confirm(id,current,TODAY,"Duplicate","archivist1"));
        check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE recipient_role='READER' AND request_id=?",Integer.class,id)==3,"repeated confirmation does not create duplicate notice");
        check(notifications.inbox("reader1",0,true).items().stream().allMatch(n->n.readAt()==null),"unread filter only returns unread rows");
        check(notifications.inbox("admin1",0,false).items().isEmpty() && notifications.unread("admin1")==0,"admin inbox has no reader clinical notifications");
        reading.cancel(id,version(id),"Cancelled unperformed days","archivist1");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE recipient_role='READER' AND request_id=?",Integer.class,id)==4,"archivist cancellation notifies requester");
        var cancelledNotice=notifications.inbox("reader1",0,false).items().stream().filter(n->n.requestId().equals(id) && n.title().equals("Yêu cầu đã hủy")).findFirst().orElseThrow();
        check(cancelledNotice.message().isEmpty(),"new cancellation notice only has short Vietnamese title");
        jdbc.update("UPDATE notifications SET title='Yêu cầu / ngày đọc chưa thực hiện đã hủy',message='Legacy cancellation details' WHERE id=?",cancelledNotice.id());
        var oldNotice=notifications.inbox("reader1",0,false).items().stream().filter(n->n.id().equals(cancelledNotice.id())).findFirst().orElseThrow();
        check(oldNotice.title().equals("Yêu cầu đã hủy") && oldNotice.message().isEmpty(),"old cancellation notices also render short title without details");
        check(jdbc.queryForObject("SELECT message FROM notifications WHERE id=?",String.class,cancelledNotice.id()).equals("Legacy cancellation details"),"display normalization preserves historical notification data");
        UUID refused=submit(create("NOTICE_REJECT",true));reading.review(refused,0,"PENDING_APPROVAL","Checked","archivist1");reading.decide(refused,version(refused),false,"Not approved",SECRET,"director1");
        check(jdbc.queryForObject("SELECT title FROM notifications WHERE recipient_role='READER' AND request_id=?",String.class,refused).contains("từ chối"),"rejection produces internal result notification");
        accounts.create("fr_notice_user","Notice reader","READER",department,null,LOGIN,"admin1");
        UUID changed=reading.submit(create("NOTICE_ROLE",false),"Purpose",UUID.randomUUID(),"fr_notice_user");reading.review(changed,0,"NEEDS_INFO","Clarify","archivist1");
        var changedUser=accounts.list().stream().filter(a->a.get("username").equals("fr_notice_user")).findFirst().orElseThrow();
        accounts.update((UUID)changedUser.get("id"),0,"Now technical","ADMIN","ACTIVE",null,null,null,"admin1");
        check(notifications.unread("fr_notice_user")==0 && notifications.inbox("fr_notice_user",0,false).items().isEmpty(),"role change to admin hides previous clinical notices");
        UUID readerTwo=reading.submit(create("NOTICE_OTHER",false),"Other purpose",UUID.randomUUID(),"fr_reader2");reading.review(readerTwo,0,"NEEDS_INFO","Other notice","archivist1");
        notifications.readAll("reader1");check(notifications.unread("reader1")==0 && notifications.unread("fr_reader2")==1,"read-all affects only current recipient");
        notifications.readAll("reader1");check(notifications.unread("reader1")==0,"read-all remains idempotent");
        expect(BusinessException.class,()->notifications.inbox("reader1",-1,false));
        // More than one page, using real unique events, without modifying unrelated requests.
        for(int i=0;i<22;i++){
            UUID event=jdbc.queryForObject("INSERT INTO request_events(request_id,actor,action,revision,details) VALUES(?,'archivist1','REVIEW_NEEDS_INFO',1,?) RETURNING id",UUID.class,id,"Pagination "+i);
            tx.executeWithoutResult(status->{notifications.forRequest(id,event,"REVIEW_NEEDS_INFO","Pagination notice");notifications.forRequest(id,event,"REVIEW_NEEDS_INFO","Pagination notice");});
        }
        var page0=notifications.inbox("reader1",0,false);var page1=notifications.inbox("reader1",1,false);
        check(page0.items().size()==20 && !page1.items().isEmpty(),"notification inbox paginates at twenty");
        check(page0.items().stream().noneMatch(n->page1.items().stream().anyMatch(p->p.id().equals(n.id()))),"stable notification pagination has no duplicates");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE recipient_role='READER' AND request_id=?",Integer.class,id)==26,"unique recipient/event suppresses duplicate notification creation");
        // PostgreSQL CURRENT_TIMESTAMP is fixed at transaction start. A newer notice must still be markable.
        var arrived=new java.util.concurrent.atomic.AtomicReference<UUID>();
        tx.executeWithoutResult(status->{
            jdbc.queryForObject("SELECT CURRENT_TIMESTAMP",java.sql.Timestamp.class);
            try(var worker=java.util.concurrent.Executors.newSingleThreadExecutor()){
                arrived.set(worker.submit(()->tx.execute(inner->{
                    UUID event=jdbc.queryForObject("INSERT INTO request_events(request_id,actor,action,revision,details) VALUES(?,'archivist1','CONFIRM',1,'Concurrent arrival') RETURNING id",UUID.class,id);
                    notifications.forRequest(id,event,"CONFIRM","Concurrent arrival");
                    return jdbc.queryForObject("SELECT id FROM notifications WHERE event_id=?",UUID.class,event);
                })).get(15,java.util.concurrent.TimeUnit.SECONDS));
            }catch(Exception ex){throw new IllegalStateException(ex);}
            notifications.readAll("reader1");
        });
        check(jdbc.queryForObject("SELECT read_at IS NOT NULL AND read_at>=created_at FROM notifications WHERE id=?",Boolean.class,arrived.get()),"read-all tolerates notification committed after marking transaction started");
    }
    private static void archivistDashboardAndCancellationChecks(mrm.authentication.DashboardService dashboard)throws Exception {
        UUID record=create("CANCEL_STAFF_NOTICE",false),request=submit(record);
        UUID old=jdbc.queryForObject("SELECT id FROM notifications WHERE request_id=? AND recipient_role='ARCHIVIST'",UUID.class,request);
        long before=notifications.unread("archivist1");
        reading.cancel(request,version(request),"Reader cancels","reader1");
        String code=jdbc.queryForObject("SELECT record_code FROM medical_records WHERE id=?",String.class,record);
        var inbox=notifications.inbox("archivist1",0,false);
        var replacement=inbox.items().stream().filter(n->n.requestId().equals(request)).findFirst().orElseThrow();
        check(replacement.title().equals("Yêu cầu với hồ sơ "+code+" đã hủy") && replacement.cancelled(),"Archivist receives cancellation identifying the record");
        check(inbox.items().stream().noneMatch(n->n.id().equals(old)) && notifications.unread("archivist1")==before-1,"cancelled work notice leaves the pending unread badge");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE id=?",Integer.class,old)==1,"obsolete work notification remains stored for audit");
        check(notifications.openPath(replacement.id(),"archivist1").equals("/notifications"),"stale cancellation open action returns to inbox");
        var arch=login("archivist1");var reader=login("reader1");
        String archHtml=get(arch,"/notifications?scope=handled").body();
        check(archHtml.contains(code+" đã hủy") && !archHtml.contains("/notifications/"+replacement.id()+"/open"),"Archivist cancelled notification has no open-request button");
        var readerNotice=notifications.inbox("reader1",0,false).items().stream().filter(n->n.requestId().equals(request)).findFirst().orElseThrow();
        check(readerNotice.cancelled() && !get(reader,"/notifications?scope=handled").body().contains("/notifications/"+readerNotice.id()+"/open"),"Reader cancelled notification has no open-request button");
        expect(org.springframework.web.server.ResponseStatusException.class,()->notifications.openPath(old,"archivist1"));
        UUID pending=submit(create("DASHBOARD_REVIEW",false));
        var work=dashboard.archivistWork("archivist1","review");
        check(work.requests().stream().anyMatch(item->item.url().endsWith(pending.toString())) && work.requests().stream().noneMatch(item->item.url().endsWith(request.toString())),"review queue shows submitted work and excludes cancelled requests");
        var stats=dashboard.load("archivist1").stats();
        check(stats.get(0).value()==work.requests().size()+work.declarations().size(),"review statistic matches both work tables");
        check(stats.stream().allMatch(stat->stat.url()!=null) && stats.get(0).url().equals("/archivist/work?queue=review") && stats.get(2).url().equals("/archivist/work?queue=handover"),"all three Archivist statistics link to their own filtered lists");
        UUID ready=submit(create("DASHBOARD_HANDOVER",false));confirm(ready);
        var handovers=dashboard.archivistWork("archivist1","handover");
        check(handovers.requests().stream().anyMatch(item->item.url().endsWith(ready+"/circulation")) && dashboard.load("archivist1").stats().get(2).value()==handovers.requests().size(),"handover queue matches confirmed-session statistic");
        for(var stat:dashboard.load("archivist1").stats())check(get(arch,stat.url()).statusCode()==200,"Archivist stat destination renders: "+stat.label());
        check(get(reader,"/archivist/work?queue=review").statusCode()==403,"Reader cannot open Archivist work queues");
        expect(org.springframework.security.access.AccessDeniedException.class,()->dashboard.archivistWork("reader1","review"));
        expect(BusinessException.class,()->dashboard.archivistWork("archivist1","all"));
        check(get(reader,"/css/portal.css").body().contains(".borrow-result.borrow-deceased") && get(reader,"/js/borrow-record-picker.js").body().contains("row.classList.add('borrow-deceased')"),"picker uses distinct death colours for candidates and selected records");
        String search=get(reader,"/reader/records?q="+encode("FR_MED_DEATH")).body();
        check(search.contains("Bệnh án tử") && search.contains("deceased-badge"),"search notes display death marker without exposing internal notes");
    }
    private static void employeeChecks() {
        var birth=LocalDate.of(1990,2,3);
        expect(BusinessException.class,()->accounts.createWithEmployee("fr_emp_invalid","Invalid","READER",null,null,LOGIN,"123",birth,"MALE","admin1"));
        check(jdbc.queryForObject("SELECT COUNT(*) FROM app_users WHERE username='fr_emp_invalid'",Integer.class)==0,"invalid employee ID leaves no partial account");
        expect(BusinessException.class,()->accounts.createWithEmployee("fr_emp_future","Future","READER",null,null,LOGIN,"0000000010",TODAY.plusDays(1),"MALE","admin1"));
        expect(org.springframework.security.access.AccessDeniedException.class,()->accounts.createWithEmployee("fr_emp_forbidden","Forbidden","READER",null,null,LOGIN,"0000000010",birth,"MALE","reader1"));
        accounts.createWithEmployee("fr_emp_reader","Nhân viên thử","READER",department,null,LOGIN,"0000000010",birth,"MALE","admin1");
        UUID employeeAccount=jdbc.queryForObject("SELECT id FROM app_users WHERE username='fr_emp_reader'",UUID.class);
        check(jdbc.queryForObject("SELECT citizen_id FROM employees WHERE account_id=?",String.class,employeeAccount).equals("0000000010"),"employee ID preserves leading zeros");
        expect(BusinessException.class,()->accounts.createWithEmployee("fr_emp_duplicate","Duplicate","READER",null,null,LOGIN,"0000000010",birth,"FEMALE","admin1"));
        check(jdbc.queryForObject("SELECT COUNT(*) FROM app_users WHERE username='fr_emp_duplicate'",Integer.class)==0,"duplicate employee ID rolls back account and audit");
        accounts.update(employeeAccount,0,"Tên nhân viên mới","DIRECTOR","ACTIVE",department,null,null,"admin1");
        check(jdbc.queryForObject("SELECT full_name || ':' || role FROM employees WHERE account_id=?",String.class,employeeAccount).equals("Tên nhân viên mới:DIRECTOR"),"employee name and role stay synchronized with account updates");
        expect(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("UPDATE employees SET role='READER' WHERE account_id=?",employeeAccount));
        accounts.updateWithEmployee(employeeAccount,1,"Tên nhân viên mới","READER","ACTIVE",department,null,null,"0000000010",LocalDate.of(1992,2,3),"OTHER","admin1");
        check(jdbc.queryForObject("SELECT gender FROM employees WHERE account_id=?",String.class,employeeAccount).equals("OTHER"),"Admin can update employee demographics");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE details LIKE '%0000000010%'",Integer.class)==0,"employee citizen ID is not copied into audit details");
    }
    @SuppressWarnings("unchecked") private static void inventoryChecks(){
        UUID batch=inventory.create("Synthetic stocktake",slot,"archivist1");var items=(List<Map<String,Object>>)inventory.detail(batch,"archivist1").get("items");
        check(items.size()>=100,"inventory captures scope and expected locations");expect(BusinessException.class,()->inventory.close(batch,"archivist1"));
        UUID sample=(UUID)items.getFirst().get("record_id");expect(BusinessException.class,()->inventory.check(batch,sample,"MISSING",null,"","archivist1"));
        expect(BusinessException.class,()->inventory.check(batch,sample,"FOUND",other,"Contradictory result","archivist1"));
        for(var item:items)inventory.check(batch,(UUID)item.get("record_id"),"FOUND",null,"Checked synthetic record","archivist1");
        inventory.check(batch,sample,"WRONG_LOCATION",other,"Found in adjacent compartment","archivist1");
        inventory.close(batch,"archivist1");check(inventory.detail(batch,"archivist1").get("state").equals("CLOSED"),"complete inventory closes");
        expect(BusinessException.class,()->inventory.check(batch,sample,"FOUND",null,"After closure","archivist1"));
        check(records.details(sample).form().locationId().equals(slot),"stocktake does not silently change medical record location");
    }
    private static void reportChecks(){
        var all=new ReportService.Filter("","","","",null,null,"");
        for(String type:ReportService.TYPES.keySet())check(reports.rows(type,all,"archivist1")!=null,"report query executes: "+type);
        for(String type:ReportService.TYPES.keySet())if(!List.of("locations","inventory").contains(type))check(!reports.rows(type,all,"director1").toString().contains("PRIVATE_STORAGE_MARKER"),"director report excludes storage path: "+type);
        expect(org.springframework.security.access.AccessDeniedException.class,()->reports.rows("records",all,"admin1"));
        expect(org.springframework.security.access.AccessDeniedException.class,()->reports.rows("audit",all,"reader1"));
        check(reports.rows("audit",all,"admin1").stream().allMatch(r->List.of("ACCOUNT","AUTH").contains(r.get("entity_type"))),"admin audit limited to technical accounts/authentication");
        check(reports.rows("records",new ReportService.Filter("' OR 1=1 --","","","",null,null,""),"archivist1").isEmpty(),"report search is parameterized");
        check(reports.rows("audit",new ReportService.Filter("","director1","APPROVE","",null,null,""),"director1").stream().allMatch(r->r.get("actor").equals("director1") && r.get("action").equals("APPROVE")),"audit filters actor and action");
        check(ReportService.csv(List.of(Map.of("name","=HYPERLINK(\"bad\")"))).contains("'=HYPERLINK"),"CSV neutralizes spreadsheet formulas");
        check(ReportService.csv(List.of(Map.of("name","=1+1\nnext line"))).contains("'=1+1"),"CSV formula protection covers multiline cells");
    }
    private static HttpClient client(){return HttpClient.newBuilder().cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NEVER).build();}
    private static HttpResponse<String> get(HttpClient client,String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(20)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    private static void requestHistoryChecks()throws Exception {
        var ids=new ArrayList<UUID>();
        for(int i=0;i<12;i++)ids.add(submit(create("STATEMENT_"+i,false)));
        var first=reading.readerHistory("reader1","FR Patient STATEMENT_",null,null,0);
        var second=reading.readerHistory("reader1","FR Patient STATEMENT_",null,null,1);
        check(first.total()==12 && first.items().size()==10 && second.items().size()==2,"statement paginates ten requests and returns all remaining rows");
        check(first.items().stream().noneMatch(a->second.items().stream().anyMatch(b->b.get("id").equals(a.get("id")))),"statement pages do not overlap");
        check(first.from().equals(TODAY.minusDays(6)) && first.to().equals(TODAY),"statement defaults to seven calendar days inclusive");
        UUID older=ids.getFirst();jdbc.update("UPDATE reading_requests SET registered_on=? WHERE id=?",TODAY.minusDays(7),older);
        check(reading.readerHistory("reader1","FR Patient STATEMENT_",null,null,0).total()==11,"default statement excludes day before seven-day window");
        check(reading.readerHistory("reader1","FR Patient STATEMENT_",TODAY.minusDays(7),TODAY.minusDays(7),0).total()==1,"date-only boundaries are inclusive and historical range is selectable");
        check(reading.readerHistory("reader1","FR Patient STATEMENT_0",TODAY.minusDays(7),TODAY,0).total()==1,"name and date filters combine");
        check(reading.readerHistory("reader1","no matching records",null,null,0).items().isEmpty(),"empty statement handled");
        expect(BusinessException.class,()->reading.readerHistory("reader1","",TODAY,TODAY.minusDays(1),0));
        var browser=login("reader1");
        var page=get(browser,"/reader/requests?q="+encode("FR Patient STATEMENT_")+"&from="+TODAY.minusDays(7)+"&to="+TODAY);
        check(page.statusCode()==200 && page.body().contains("Ngày đăng ký") && !page.body().contains("FR Patient STATEMENT_"+"0</span>"),"statement HTML renders registration date without patient subtitle");
        check(get(browser,"/reader/requests?from="+TODAY+"&to="+TODAY.minusDays(1)).body().contains("Ngày bắt đầu phải trước"),"invalid statement date range shows useful error");
        UUID pending=reading.submit(create("LIFE_PENDING",false),"VISIBLE_PENDING_PURPOSE",UUID.randomUUID(),"reader1");
        UUID earlier=reading.submit(create("LIFE_EARLIER",false),"EARLIER_PENDING_PURPOSE",UUID.randomUUID(),"reader1");
        jdbc.update("UPDATE reading_requests SET registered_on=?,created_at=created_at-INTERVAL '1 day' WHERE id=?",TODAY.minusDays(1),earlier);
        UUID scheduled=reading.submit(create("LIFE_SCHEDULED",false),"VISIBLE_SCHEDULED_PURPOSE",UUID.randomUUID(),"reader1");confirm(scheduled);
        UUID loanRecord=create("LIFE_BORROWED",false),loan=reading.submit(loanRecord,"HIDDEN_BORROWED_PURPOSE",UUID.randomUUID(),"reader1");confirm(loan);
        reading.handover(session(loan,TODAY),version(loan),"Normal","archivist1");
        var pendingOrder=reading.readerHistory("reader1","FR Patient LIFE_",null,null,0,"SUBMITTED","status");
        check(pendingOrder.total()==2 && pendingOrder.items().getFirst().get("id").equals(earlier),"status filtering combines with oldest registration first within same state");
        var dateOrder=reading.readerHistory("reader1","FR Patient LIFE_",null,null,0,"","registered");
        check(dateOrder.items().getLast().get("id").equals(earlier),"default registration sort puts newer registrations before older ones");
        check(reading.readerHistory("reader1","FR Patient LIFE_",null,null,0,"","registered_asc").items().getFirst().get("id").equals(earlier),"date ascending arrow selects oldest registrations first");
        check(reading.readerHistory("fr_snapshot_reader","",null,null,0).availableStates().equals(List.of("SUBMITTED")),"status choices contain only states of current reader, not other readers");
        check(reading.readerHistory("fr_snapshot_reader","no rows",TODAY.minusDays(60),TODAY.minusDays(30),0).availableStates().equals(List.of("SUBMITTED")),"own status choices remain stable when text and date filters yield no rows");
        check(reading.readerHistory("reader1","FR Patient LIFE_",null,null,0,"CONFIRMED","registered").total()==1 && reading.readerHistory("reader1","FR Patient LIFE_",null,null,0,"BORROWED","registered").total()==1,"scheduled and physically borrowed requests filter independently");
        String inProgress=get(browser,"/reader/requests?q="+encode("FR Patient LIFE_")).body();
        check(inProgress.contains("Đang mượn") && inProgress.contains("VISIBLE_PENDING_PURPOSE") && !inProgress.contains("HIDDEN_BORROWED_PURPOSE") && !inProgress.contains("Chờ nhân viên lưu trữ xếp lịch"),"notes show purpose only while processing and borrowed label after handover");
        reading.receive(session(loan,TODAY),version(loan),slot,"NORMAL","Normal return","archivist1");
        var complete=reading.readerHistory("reader1","FR Patient LIFE_",null,null,0,"COMPLETED","status");
        check(complete.total()==1 && complete.items().getFirst().get("id").equals(loan) && complete.items().getFirst().get("returned_on")!=null,"all returned sessions derive completed state and actual return date");
        var reverseStatus=reading.readerHistory("reader1","FR Patient LIFE_",null,null,0,"","status_desc");
        check(reverseStatus.items().getFirst().get("display_state").equals("COMPLETED"),"state descending arrow reverses lifecycle groups");
        check(reading.readerHistory("reader1","FR Patient LIFE_",null,null,0,"SUBMITTED","status_desc").items().getFirst().get("id").equals(earlier),"reversed status groups still keep oldest first within each state");
        String completeHtml=get(browser,"/reader/requests?q="+encode("FR Patient LIFE_")+"&status=COMPLETED&sort=status").body();
        check(completeHtml.contains("Hoàn thành") && completeHtml.contains("Ngày trả ") && !completeHtml.contains("HIDDEN_BORROWED_PURPOSE"),"completed HTML shows return date without borrowing purpose");
        jdbc.update("INSERT INTO reading_sessions(request_id,record_id,reading_on,state) VALUES(?,?,?,'CONFIRMED')",loan,loanRecord,TODAY.plusDays(1));
        check(reading.readerHistory("reader1","FR Patient LIFE_BORROWED",null,null,0,"COMPLETED","registered").total()==0,"partially returned multi-session request is not falsely complete");
        jdbc.update("DELETE FROM reading_sessions WHERE request_id=? AND reading_on=?",loan,TODAY.plusDays(1));
        reading.cancel(pending,version(pending),"Cancel lifecycle fixture","reader1");
        String cancelledHtml=get(browser,"/reader/requests?q="+encode("FR Patient LIFE_PENDING")+"&status=CANCELLED").body();
        check(Pattern.compile("<td\\s*>"+TODAY+"</td>\\s*<td\\s*>\\s*</td>").matcher(cancelledHtml).find() && !cancelledHtml.contains("VISIBLE_PENDING_PURPOSE"),"cancelled requests retain registration date but omit pickup and purpose");
        String filteredPage=get(browser,"/reader/requests?q="+encode("FR Patient STATEMENT_")+"&status=SUBMITTED&sort=status").body();
        check(filteredPage.contains("status=SUBMITTED") && filteredPage.contains("sort=registered") && filteredPage.contains("page=1"),"pagination retains status filter and sort order");
        check(!filteredPage.contains("id=\"request-sort\"") && filteredPage.contains("aria-label=\"Ngày đăng ký tăng dần\"") && !filteredPage.contains("aria-label=\"Trạng thái giảm dần\""),"sorting dropdown and state arrows absent; accessible date arrows retained");
        var limitedReader=login("fr_snapshot_reader");
        String limitedHtml=get(limitedReader,"/reader/requests").body();
        var statusSelect=Pattern.compile("<select[^>]*id=\"request-status\"[^>]*>(.*?)</select>",Pattern.DOTALL).matcher(limitedHtml);
        check(statusSelect.find() && statusSelect.group(1).contains("SUBMITTED") && !statusSelect.group(1).contains("COMPLETED") && !statusSelect.group(1).contains("CANCELLED"),"rendered status menu excludes states present only for other readers");
        expect(BusinessException.class,()->reading.readerHistory("reader1","",null,null,0,"INJECTED","registered"));
        expect(BusinessException.class,()->reading.readerHistory("reader1","",null,null,0,"","invalid-order"));
        UUID optional=create("OPTIONAL_REASON",false);var blank=reading.submitBatch(List.of(optional),"",UUID.randomUUID(),"reader1");
        check(blank.size()==1,"up to five records can register without a reason");
        var six=new ArrayList<UUID>();for(int i=0;i<6;i++)six.add(create("REASON_SIX_"+i,false));
        UUID token=UUID.randomUUID();expect(BusinessException.class,()->reading.submitBatch(six," ",token,"reader1"));
        check(jdbc.queryForObject("SELECT COUNT(*) FROM reading_request_batches WHERE submit_token=?",Integer.class,token)==0,"missing reason above five creates no partial batch");
        check(reading.submitBatch(six,"Required reason for six",token,"reader1").size()==6,"more than five records accepted with required reason");
        var form=get(browser,"/reader/requests/new");
        check(!form.body().contains("Theo dõi yêu cầu đã gửi"),"registration page removes tracking footer");
        check(form.body().contains("borrow-selection-box") && !form.body().contains("/20 hồ sơ") && form.body().contains("data-borrow-purpose"),"registration field style hides maximum and marks conditional reason");
        var search=get(browser,"/reader/records");
        check(search.statusCode()==200 && !search.body().contains(">IN_STORAGE<") && !search.body().contains(">STORED<") && search.body().contains("record-search-grid"),"record search shows a single localized status and aligned filter grid");
    }
    private static mrm.electronic.ElectronicRecordForm datedForm(UUID token,Long version,String name,Integer year,String sex,String code){
        return datedForm(token,version,name,year,sex,code,null,null,false,null,null,null,null);
    }
    private static mrm.electronic.ElectronicRecordForm datedForm(UUID token,Long version,String name,Integer year,String sex,String code,UUID dept,UUID typeId,Boolean deceased,LocalDate admission,LocalDate discharge,String source,String note){
        return new mrm.electronic.ElectronicRecordForm(token,version,name,year,sex,code,dept,typeId,deceased,admission,discharge,source,note,15,1);
    }
    private static void electronicChecks(mrm.electronic.ElectronicRecordService service)throws Exception {
        accounts.create("fr_electronic_other","Other Doctor","READER",null,null,LOGIN,"admin1");
        long paperCount=jdbc.queryForObject("SELECT COUNT(*) FROM medical_records WHERE has_paper_copy",Long.class);
        long locationCount=jdbc.queryForObject("SELECT COUNT(*) FROM record_location_history",Long.class);
        var form=datedForm(UUID.randomUUID(),0L,"Electronic synthetic patient",1992,"FEMALE",null);
        UUID id=service.create(form,"reader1");
        var initial=service.detail(id,"reader1");String code=(String)initial.get("record_code");
        check(code.matches("MRM-REC-"+TODAY.getYear()+"-[0-9]{6,}") && initial.get("state").equals("SUBMITTED") && initial.get("published_at")==null,"Reader gets automatic code and unpublished electronic record");
        check(service.create(form,"reader1").equals(id),"duplicate electronic submit token does not create another record");
        expect(BusinessException.class,()->service.create(datedForm(form.token(),0L,"Changed token content",1992,"FEMALE",null),"reader1"));
        expect(org.springframework.security.access.AccessDeniedException.class,()->service.create(form,"archivist1"));
        expect(org.springframework.web.server.ResponseStatusException.class,()->service.detail(id,"fr_electronic_other"));
        expect(BusinessException.class,()->service.create(datedForm(UUID.randomUUID(),0L,"Future",TODAY.getYear()+1,"MALE",null),"reader1"));
        expect(BusinessException.class,()->service.decide(id,0,true,"Bypass review",SECRET,"director1"));
        UUID staffNotice=jdbc.queryForObject("SELECT n.id FROM notifications n JOIN app_users u ON u.id=n.recipient_id WHERE n.electronic_record_id=? AND n.recipient_role='ARCHIVIST' AND u.username='archivist1'",UUID.class,id);
        check(notifications.openPath(staffNotice,"archivist1").equals("/archivist/records/"+id),"electronic staff bell opens electronic work, not reading request");
        service.review(id,0,false,"Clarify patient name","archivist1");
        check(service.detail(id,"reader1").get("state").equals("NEEDS_INFO"),"Archivist can request electronic record corrections");
        service.revise(id,datedForm(UUID.randomUUID(),1L,"Electronic corrected patient",1992,"FEMALE","E-SYNTH"),"reader1");
        check(service.detail(id,"reader1").get("record_code").equals(code),"resubmission preserves generated code");
        expect(BusinessException.class,()->service.review(id,1,true,"Stale version","archivist1"));
        expect(org.springframework.security.access.AccessDeniedException.class,()->service.review(id,2,true,"Forbidden","reader1"));
        service.review(id,2,true,"Identity checked","archivist1");
        check(service.detail(id,"director1").get("published_at")==null,"Archivist confirmation never publishes before Director approval");
        UUID directorNotice=jdbc.queryForObject("SELECT n.id FROM notifications n JOIN app_users u ON u.id=n.recipient_id WHERE n.electronic_record_id=? AND n.recipient_role='DIRECTOR' AND u.username='director1'",UUID.class,id);
        check(notifications.openPath(directorNotice,"director1").equals("/director/records/"+id),"Director electronic notification routes correctly");
        expect(BusinessException.class,()->service.decide(id,3,true,"Incorrect secret","wrong-secret","director1"));
        check(service.detail(id,"reader1").get("state").equals("PENDING_APPROVAL"),"wrong step-up password leaves electronic record unpublished");
        service.decide(id,3,true,"Approved electronic publication",SECRET,"director1");
        var published=service.detail(id,"reader1");
        check(published.get("state").equals("APPROVED") && published.get("published_at")!=null && published.get("approver_id")!=null,"Director approval immediately publishes electronic record in same transaction");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM medical_records WHERE has_paper_copy",Long.class)==paperCount && jdbc.queryForObject("SELECT COUNT(*) FROM record_location_history",Long.class)==locationCount,"electronic publication neither creates paper record nor occupies physical storage");
        expect(BusinessException.class,()->service.cancel(id,4,"reader1"));
        expect(BusinessException.class,()->service.revise(id,datedForm(UUID.randomUUID(),4L,"Unauthorized revision",1992,"FEMALE",null),"reader1"));
        expect(BusinessException.class,()->service.decide(id,4,true,"Duplicate approval",SECRET,"director1"));
        check(jdbc.queryForObject("SELECT COUNT(*) FROM electronic_record_events WHERE record_id=? AND action='APPROVE'",Integer.class,id)==1,"electronic approval and event not duplicated");
        UUID rejected=service.create(datedForm(UUID.randomUUID(),0L,"Reject synthetic",1990,"MALE",null),"reader1");
        service.review(rejected,0,true,"Reviewed","archivist1");service.decide(rejected,1,false,"Rejected",SECRET,"director1");
        check(service.detail(rejected,"reader1").get("published_at")==null,"rejected electronic record never publishes");
        UUID cancelled=service.create(datedForm(UUID.randomUUID(),0L,"Cancel synthetic",1980,"UNKNOWN",null),"reader1");
        service.cancel(cancelled,0,"reader1");check(service.detail(cancelled,"reader1").get("state").equals("CANCELLED"),"Reader can cancel own pending electronic record");
        var pool=Executors.newFixedThreadPool(4);
        try {
            var jobs=new ArrayList<Callable<UUID>>();for(int i=0;i<8;i++){final int item=i;jobs.add(()->service.create(datedForm(UUID.randomUUID(),0L,"Concurrent electronic "+item,1980,"UNKNOWN",null),"reader1"));}
            var codes=new HashSet<String>();for(var job:pool.invokeAll(jobs))codes.add((String)service.detail(job.get(),"reader1").get("record_code"));
            check(codes.size()==8,"concurrent electronic submissions receive unique automatic codes");
        }finally{pool.shutdownNow();}
        check(service.list("reader1","corrected","APPROVED",0).total()==1 && service.list("fr_electronic_other","","",0).total()==0,"electronic search combines name/state and isolates other doctors");
        var reader=login("reader1");var arch=login("archivist1");var director=login("director1");var admin=login("admin1");
        var htmlForm=get(reader,"/reader/records/new");String csrf=csrf(htmlForm.body());
        check(htmlForm.statusCode()==200 && !htmlForm.body().contains("name=\"archiveNumber\"") && !htmlForm.body().contains("name=\"locationId\""),"Reader patient-only form never asks for archive number or storage location");
        check(get(arch,"/reader/records/new").statusCode()==403 && get(admin,"/reader/records/new").statusCode()==403,"other roles cannot use doctor creation endpoint");
        check(get(reader,"/reader/records").statusCode()==200 && get(arch,"/archivist/records").statusCode()==200 && get(director,"/director/records").statusCode()==200,"electronic lists render for all three clinical roles");
        check(get(arch,"/archivist/records/new").statusCode()==200,"Archivist retains paper intake form");
        UUID submitToken=UUID.randomUUID();
        check(post(reader,"/reader/records","","token",submitToken,"version",0,"patientName","Forged","birthYear",1990,"birthDay",15,"birthMonth",1,"sex","MALE").statusCode()==403,"electronic creation requires CSRF");
        var submit=post(reader,"/reader/records",csrf,"token",submitToken,"version",0,"patientName","HTTP electronic <script>alert(1)</script>","birthYear",1990,"birthDay",15,"birthMonth",1,"sex","MALE","state","APPROVED","published_at","2020-01-01","record_code","FORGED");
        check(submit.statusCode()==302,"HTTP doctor submits electronic patient form");UUID httpId=jdbc.queryForObject("SELECT id FROM electronic_records WHERE create_token=?",UUID.class,submitToken);
        var httpRecord=service.detail(httpId,"reader1");
        check(httpRecord.get("state").equals("SUBMITTED") && !httpRecord.get("record_code").equals("FORGED") && httpRecord.get("published_at")==null,"forged code/state/publication inputs cannot bypass workflow");
        check(get(reader,"/reader/records/"+httpId).body().contains("&lt;script&gt;"),"electronic detail escapes patient data");
        check(get(login("fr_electronic_other"),"/reader/records/"+httpId).statusCode()==404,"HTTP ownership protects electronic patient information");
        var staffHtml=get(arch,"/archivist/records/"+httpId);
        check(staffHtml.body().contains("Xác nhận và trình Giám đốc"),"Archivist sees electronic verification form");
        check(post(arch,"/archivist/records/"+httpId+"/review",csrf(staffHtml.body()),"version",0,"ready",true,"note","HTTP reviewed").statusCode()==302,"HTTP Archivist submits electronic record to Director");
        var directorHtml=get(director,"/director/records/"+httpId);
        check(post(director,"/director/records/"+httpId+"/decide",csrf(directorHtml.body()),"version",1,"approve",true,"note","HTTP publication","secret",SECRET).statusCode()==302,"HTTP Director publishes electronic record");
        check(get(reader,"/reader/records/"+httpId).body().contains("Đã công bố"),"Reader sees electronic publication immediately after Director approval");
        UUID publicationNotice=jdbc.queryForObject("SELECT id FROM notifications WHERE electronic_record_id=? AND recipient_role='READER'",UUID.class,httpId);
        var opened=post(reader,"/notifications/"+publicationNotice+"/open",csrf(get(reader,"/notifications").body()));
        check(opened.statusCode()==302 && opened.headers().firstValue("location").orElse("").endsWith("/reader/records/"+httpId),"publication bell opens correct electronic record for its owner");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE entity_type='ELECTRONIC_RECORD' AND entity_id=?",Integer.class,httpId.toString())==3,"electronic workflow records all three actions in audit");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM electronic_record_events WHERE snapshot->>'record_code' IS NULL",Integer.class)==0,"electronic event snapshots retain automatic record code");
    }
    private static void extendedElectronicChecks(mrm.electronic.ElectronicRecordService service,mrm.authentication.DashboardService dashboards)throws Exception {
        var detailed=datedForm(UUID.randomUUID(),0L,"Expanded synthetic patient",1988,"FEMALE","SYNTHETIC-EXT",department,type,false,TODAY.minusDays(5),TODAY.minusDays(1),"Synthetic source","Optional note <script>alert(1)</script>");
        UUID id=service.create(detailed,"reader1");var saved=service.detail(id,"reader1");
        check(saved.get("department_id").equals(department) && saved.get("record_type_id").equals(type) && saved.get("note").equals(detailed.note()),"expanded electronic fields persist with optional note");
        check(saved.get("department_name_snapshot")!=null && saved.get("record_type_name_snapshot")!=null,"electronic metadata snapshots retain department and type names");
        expect(BusinessException.class,()->service.create(datedForm(UUID.randomUUID(),0L,"Future dates",1988,"MALE",null,department,type,false,TODAY.plusDays(1),null,null,null),"reader1"));
        expect(BusinessException.class,()->service.create(datedForm(UUID.randomUUID(),0L,"Inverted dates",1988,"MALE",null,department,type,false,TODAY,TODAY.minusDays(1),null,null),"reader1"));
        expect(BusinessException.class,()->service.create(datedForm(UUID.randomUUID(),0L,"Invalid reference",1988,"MALE",null,UUID.randomUUID(),type,false,null,null,null,null),"reader1"));
        expect(BusinessException.class,()->service.create(datedForm(UUID.randomUUID(),0L,"Too long notes",1988,"MALE",null,null,null,false,null,null,null,"x".repeat(2001)),"reader1"));
        UUID blank=service.create(datedForm(UUID.randomUUID(),0L,"No note synthetic",1980,"UNKNOWN",null,department,type,false,null,null," "," "),"reader1");
        check(service.detail(blank,"reader1").get("note")==null,"blank note accepted and normalized to null");
        service.revise(id,datedForm(UUID.randomUUID(),0L,"Expanded revised patient",1988,"FEMALE","SYNTHETIC-EXT",department,type,true,TODAY.minusDays(5),TODAY.minusDays(1),"Revised source","Revised optional note"),"reader1");
        var revised=service.detail(id,"reader1");check(Boolean.TRUE.equals(revised.get("deceased")) && revised.get("note").equals("Revised optional note") && revised.get("record_code").equals(saved.get("record_code")),"edit updates full clinical metadata without changing code");
        service.review(id,1,true,"Expanded metadata checked","archivist1");service.decide(id,2,true,"Electronic metadata approved",SECRET,"director1");
        check(service.detail(id,"reader1").get("published_at")!=null,"extended metadata retains immediate electronic publication flow");
        var reader=login("reader1");var arch=login("archivist1");var director=login("director1");var admin=login("admin1");
        var form=get(reader,"/reader/records/new");
        for(String field:List.of("departmentId","recordTypeId","admissionDate","dischargeDate","deceased","sourceNote","note"))check(form.body().contains("name=\""+field+"\""),"expanded doctor form contains "+field);
        var noteTag=Pattern.compile("<textarea[^>]*name=\"note\"[^>]*>").matcher(form.body());check(noteTag.find() && !noteTag.group().contains("required"),"doctor note textarea has no required attribute");
        check(!form.body().contains("name=\"locationId\"") && !form.body().contains("name=\"policyIds\"") && !form.body().contains("name=\"firstStoredOn\""),"doctor form excludes physical storage metadata");
        for(var browser:List.of(reader,arch,director)) {
            String namespace=browser==reader?"reader":browser==arch?"archivist":"director";
            var detail=get(browser,"/"+namespace+"/records/"+id);
            check(detail.statusCode()==200 && detail.body().contains("Revised optional note") && detail.body().contains("Đợt điều trị và ghi chú"),"expanded metadata visible to "+namespace);
        }
        for(var pair:Map.of("reader",reader,"archivist",arch,"director",director,"admin",admin).entrySet()) {
            var home=get(pair.getValue(),"/"+pair.getKey()+"/home");
            check(home.statusCode()==200 && home.body().contains("welcome-banner") && home.body().contains("quick-action-card") && home.body().contains("/css/portal.css") && home.body().contains("data-nav-toggle"),"shared emerald dashboard renders for "+pair.getKey());
        }
        var readerDashboard=dashboards.load("reader1");check(readerDashboard.stats().get(2).value()==jdbc.queryForObject("SELECT COUNT(*) FROM electronic_records e JOIN app_users u ON u.id=e.created_by WHERE u.username='reader1'",Long.class),"dashboard values come from live database rather than prototype constants");
        check(readerDashboard.recent().stream().allMatch(r->r.url().startsWith("/reader/records/")),"Reader recent records link only to owned namespace");
        var adminHome=get(admin,"/admin/home");check(!adminHome.body().contains("Expanded revised patient") && !adminHome.body().contains("/reader/records") && !adminHome.body().contains("/admin/electronic-records"),"Admin dashboard never exposes clinical record data or actions");
        var adminDashboard=dashboards.load("admin1");check(adminDashboard.actions().stream().allMatch(a->a.url().startsWith("/admin/")),"Admin dashboard links retain account and audit responsibilities");
    }
    private static mrm.electronic.ElectronicRecordForm birthdayForm(int year,int month,int day,LocalDate admission){
        return new mrm.electronic.ElectronicRecordForm(UUID.randomUUID(),0L,"Birthday synthetic patient",year,"UNKNOWN",null,department,type,false,admission,null,null,null,day,month);
    }
    private static void birthDateChecks(mrm.electronic.ElectronicRecordService submissions)throws Exception {
        UUID id=submissions.create(birthdayForm(2000,2,29,null),"reader1");
        check(jdbc.queryForObject("SELECT birth_date FROM medical_records WHERE id=?",LocalDate.class,id).equals(LocalDate.of(2000,2,29)),"valid leap-day birth date is persisted without losing day or month");
        check(jdbc.queryForObject("SELECT birth_date FROM electronic_records WHERE id=?",LocalDate.class,id).equals(LocalDate.of(2000,2,29)),"compatibility projection exposes full birth date");
        check(jdbc.queryForObject("SELECT snapshot->>'birth_date' FROM electronic_record_events WHERE record_id=? ORDER BY occurred_at LIMIT 1",String.class,id).equals("2000-02-29"),"creation history preserves full birth date snapshot");
        expect(BusinessException.class,()->submissions.create(birthdayForm(2001,2,29,null),"reader1"));
        expect(BusinessException.class,()->submissions.create(birthdayForm(2000,4,31,null),"reader1"));
        LocalDate tomorrow=TODAY.plusDays(1);
        expect(BusinessException.class,()->submissions.create(birthdayForm(tomorrow.getYear(),tomorrow.getMonthValue(),tomorrow.getDayOfMonth(),null),"reader1"));
        expect(BusinessException.class,()->submissions.create(birthdayForm(2000,6,15,LocalDate.of(2000,6,14)),"reader1"));
        expect(BusinessException.class,()->submissions.create(new mrm.electronic.ElectronicRecordForm(UUID.randomUUID(),0L,"Missing month and day",2000,"UNKNOWN",null),"reader1"));
        var reader=login("reader1");String edit=get(reader,"/reader/records/"+id+"/edit").body();
        check(edit.contains("name=\"birthDay\"") && edit.contains("name=\"birthMonth\"") && edit.contains("patient-birth-inputs"),"doctor form contains separate accessible day/month/year controls");
        check(Pattern.compile("name=\"birthDay\"[^>]*value=\"29\"").matcher(edit).find() && Pattern.compile("name=\"birthMonth\"[^>]*value=\"2\"").matcher(edit).find(),"edit form restores the saved day and month");
        String form=get(reader,"/reader/records/new").body();long before=jdbc.queryForObject("SELECT COUNT(*) FROM medical_records",Long.class);
        var invalid=post(reader,"/reader/records",csrf(form),"token",UUID.randomUUID(),"version",0,"patientName","Invalid birthday HTTP","birthYear",2001,"birthMonth",2,"birthDay",29,"sex","UNKNOWN");
        check(invalid.statusCode()==200 && invalid.body().contains("Ngày sinh không hợp lệ") && jdbc.queryForObject("SELECT COUNT(*) FROM medical_records",Long.class)==before,"server rejects invalid calendar dates even when client validation is bypassed");
        var missing=post(reader,"/reader/records",csrf(form),"token",UUID.randomUUID(),"version",0,"patientName","Missing birthday HTTP","birthYear",2000,"sex","UNKNOWN");
        check(missing.statusCode()==200 && jdbc.queryForObject("SELECT COUNT(*) FROM medical_records",Long.class)==before,"server rejects missing birthday components without partial writes");
        var correction=new mrm.electronic.ElectronicRecordForm(UUID.randomUUID(),0L,"Corrected birthday",2004,"UNKNOWN",null,department,type,false,null,null,null,null,29,2);
        submissions.revise(id,correction,"reader1");
        check(jdbc.queryForObject("SELECT birth_date FROM medical_records WHERE id=?",LocalDate.class,id).equals(LocalDate.of(2004,2,29)) && jdbc.queryForObject("SELECT birth_year FROM medical_records WHERE id=?",Integer.class,id)==2004,"revision updates date and derived year atomically");
        submissions.review(id,1,true,"Birthday checked","archivist1");submissions.decide(id,2,true,"Birthday publication",SECRET,"director1");
        check(get(login("fr_electronic_other"),"/reader/records/"+id).body().contains("29/02/2004"),"public patient metadata displays full birthday in day/month/year order");
        expect(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("UPDATE medical_records SET birth_year=1999 WHERE id=?",id));
        UUID legacy=submissions.create(datedForm(UUID.randomUUID(),0L,"Legacy birthday precision",1980,"UNKNOWN",null),"reader1");
        jdbc.update("UPDATE medical_records SET birth_date=NULL WHERE id=?",legacy);
        String legacyForm=get(reader,"/reader/records/"+legacy+"/edit").body();
        check(legacyForm.contains("Hồ sơ cũ chỉ có năm sinh") && jdbc.queryForObject("SELECT birth_date IS NULL AND birth_year=1980 FROM medical_records WHERE id=?",Boolean.class,legacy),"legacy year-only record is not silently assigned a fabricated day or month");
    }
    private static void unifiedRecordChecks(mrm.electronic.ElectronicRecordService submissions)throws Exception {
        UUID id=submissions.create(datedForm(UUID.randomUUID(),0L,"Unified synthetic patient",1980,"MALE","SYNTH-UNIFIED",department,type,false,TODAY.minusDays(3),TODAY.minusDays(1),"PRIVATE_UNIFIED_SOURCE","PRIVATE_UNIFIED_NOTE"),"reader1");
        String code=(String)submissions.detail(id,"reader1").get("record_code");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM medical_records WHERE id=? AND NOT has_paper_copy AND has_electronic_copy AND submission_state='SUBMITTED'",Integer.class,id)==1,"doctor intake writes directly to single canonical record table");
        var filter=new RecordFilter(code,null,null,null,null,null,null,0,10,"newest",null,"ELECTRONIC_ONLY","");
        check(records.search(filter,"reader1").total()==0 && records.search(filter,"director1").total()==0,"official search excludes unpublished declarations even for owner and Director");
        var declarations=new RecordFilter(code,null,null,null,null,null,null,0,10,"newest",null,"ELECTRONIC_ONLY","","DECLARATIONS");
        check(records.search(declarations,"reader1").total()==1 && records.search(declarations,"fr_electronic_other").total()==0 && records.search(declarations,"director1").total()==1,"separate declaration search restricts unpublished records to owner and reviewers");
        var draftReader=login("reader1");
        check(!get(draftReader,"/reader/records?q="+encode(code)).body().contains("Unified synthetic patient") && get(draftReader,"/reader/records?scope=DECLARATIONS&q="+encode(code)).body().contains("Unified synthetic patient"),"pending declaration appears only in separate table, not official page");
        check(get(draftReader,"/reader/records?scope=DRAFTS").statusCode()==403,"Reader cannot access Archivist paper drafts tab");
        var dashboards=new mrm.authentication.DashboardService(jdbc,accounts);
        var work=dashboards.readerWork("reader1");var dashboard=dashboards.load("reader1");
        check(dashboard.stats().get(1).value()==work.requests().size()+work.declarations().size(),"pending dashboard card count matches both work queues");
        check(dashboard.stats().get(0).url().contains("status=BORROWED") && dashboard.stats().get(0).url().contains("from=1800-01-01") && dashboard.stats().get(2).url().contains("scope=DECLARATIONS"),"Reader stat links target all-time borrowed requests and own declarations");
        check(get(draftReader,"/reader/work").statusCode()==200 && get(draftReader,"/reader/work").body().contains(code),"pending card opens real queue including own unpublished declaration");
        check(dashboards.readerWork("fr_electronic_other").declarations().stream().noneMatch(x->x.code().equals(code)),"pending work queue never includes another doctor's declarations");
        var statHome=get(draftReader,"/reader/home");
        check(statHome.body().contains("href=\"/reader/work\"") && statHome.body().contains("scope=DECLARATIONS") && statHome.body().contains("Lịch sử yêu cầu"),"home stat links and Reader history navigation render");
        var requestHistory=get(draftReader,"/reader/requests");
        check(requestHistory.body().contains("<h1>Lịch sử yêu cầu</h1>") && !requestHistory.body().contains("aria-label=\"Trạng thái tăng dần\""),"Reader history renamed and status header no longer sortable");
        expect(BusinessException.class,()->reading.submit(id,"Cannot borrow electronic only",UUID.randomUUID(),"reader1"));
        expect(BusinessException.class,()->records.availability(id,0,"IN_STORAGE","No physical copy","archivist1"));
        submissions.review(id,0,true,"Ready for unified publication","archivist1");submissions.decide(id,1,true,"Publish unified metadata",SECRET,"director1");
        check(records.search(filter,"fr_electronic_other").total()==1,"published electronic record appears in same shared search");
        var reader=login("reader1");var otherReader=login("fr_electronic_other");var arch=login("archivist1");
        var basic=get(otherReader,"/reader/records/"+id);
        check(basic.statusCode()==200 && basic.body().contains("Unified synthetic patient") && !basic.body().contains("PRIVATE_UNIFIED_NOTE") && !basic.body().contains("PRIVATE_UNIFIED_SOURCE"),"published electronic metadata opens directly without exposing internal notes to another doctor");
        var eSearch=get(reader,"/reader/records?q="+encode(code)+"&format=ELECTRONIC_ONLY");
        check(eSearch.statusCode()==200 && eSearch.body().contains("Điện tử") && !eSearch.body().contains("data-borrow-record"),"electronic-only row never shows borrow button");
        check(eSearch.body().contains("<th>Mã hồ sơ</th>") && !eSearch.body().contains("<th>Đợt điều trị</th>"),"unified results use code heading and omit treatment episode column");
        check(eSearch.body().contains("<th>Ghi chú</th>") && !eSearch.body().contains("<th>Trạng thái</th>"),"results replace status column with notes after borrowing action");
        check(eSearch.body().contains("page-breadcrumb") && eSearch.body().contains("aria-label=\"Đường dẫn trang\""),"shared pages render accessible secondary breadcrumb");
        String accountChip=eSearch.body().substring(eSearch.body().indexOf("class=\"account-chip\""),eSearch.body().indexOf("</div>",eSearch.body().indexOf("class=\"account-chip\"")));
        check(accountChip.contains("current-user-name") && !accountChip.contains("<small>"),"shared navigation displays employee name without username and role subtitle");
        check(!eSearch.body().substring(eSearch.body().indexOf("<nav"),eSearch.body().indexOf("</nav>")).contains("electronic-records"),"navigation no longer has separate electronic record section");
        check(get(reader,"/reader/electronic-records").statusCode()==302,"old electronic list URL redirects to unified search");
        UUID paper=create("UNIFIED_PAPER",false);String paperCode=records.details(paper).form().archiveNumber();
        check(jdbc.queryForObject("SELECT has_paper_copy AND has_electronic_copy FROM medical_records WHERE id=?",Boolean.class,paper),"Archivist paper intake sets both representation flags");
        jdbc.update("UPDATE medical_records SET has_electronic_copy=FALSE WHERE id=?",paper);
        var paperOnly=get(reader,"/reader/records?q="+encode(paperCode)+"&format=PAPER_ONLY");
        check(paperOnly.statusCode()==200 && paperOnly.body().contains("<td>Giấy</td>"),"paper-only representation renders Giấy and supports exact format filter");
        jdbc.update("UPDATE medical_records SET has_electronic_copy=TRUE WHERE id=?",paper);
        var paperSearch=get(reader,"/reader/records?q="+encode(paperCode)+"&format=PAPER");
        check(paperSearch.body().contains("data-borrow-record") && paperSearch.body().contains("/reader/requests/new?recordId="+paper),"available paper row has separate borrowing action");
        check(get(reader,"/reader/records/"+paper).statusCode()==200,"paper code opens patient information rather than request form");
        UUID request=reading.submit(paper,"Unified paper request",UUID.randomUUID(),"reader1");
        check(!get(reader,"/reader/records?q="+encode(paperCode)).body().contains("data-borrow-record"),"active request removes paper borrow action for all search users");
        check(get(reader,"/reader/records?q="+encode(paperCode)).body().contains("Đã đăng ký mượn"),"reserved paper displays registered rather than borrowed");
        check(get(reader,"/reader/records?q="+encode(paperCode)).body().matches("(?s).*class=\"record-borrow-note\"[^>]*>.*Đã đăng ký mượn.*"),"reservation description belongs to notes cell");
        reading.cancel(request,version(request),"Release unified fixture","reader1");
        check(get(reader,"/reader/records?q="+encode(paperCode)).body().contains("data-borrow-record"),"cancelled request releases paper borrow action");
        UUID scheduled=reading.submit(paper,"Borrow status fixture",UUID.randomUUID(),"reader1");confirm(scheduled);
        check(get(reader,"/reader/records?q="+encode(paperCode)).body().contains("Đã xác nhận lịch mượn"),"confirmed pickup displays scheduled rather than physically borrowed");
        reading.handover(session(scheduled,TODAY),version(scheduled),"Normal","archivist1");
        check(get(reader,"/reader/records?q="+encode(paperCode)).body().contains("Đã được mượn"),"handed-over paper displays borrowed");
        reading.receive(session(scheduled,TODAY),version(scheduled),slot,"NORMAL","Returned normally","archivist1");
        check(get(reader,"/reader/records?q="+encode(paperCode)).body().contains("data-borrow-record"),"returned paper restores borrow action without stale reservation label");
        var existing=records.details(id).form();long before=jdbc.queryForObject("SELECT COUNT(*) FROM medical_records",Long.class);
        var attach=new RecordForm(existing.operationKey(),existing.version(),null,existing.medicalRecordNumber(),existing.patientCode(),existing.patientName(),existing.birthYear(),existing.sex(),department,type,false,TODAY.minusDays(3),TODAY.minusDays(1),TODAY,List.of(policy),other,"NORMAL","Synthetic paper arrived","PRIVATE_UNIFIED_NOTE","Attach physical copy to existing record");
        check(get(arch,"/archivist/records/"+id+"/attach-paper").statusCode()==200,"Archivist can open physical attachment form for approved electronic record");
        records.update(id,attach,"STORE","archivist1");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM medical_records",Long.class)==before && jdbc.queryForObject("SELECT record_code FROM medical_records WHERE id=?",String.class,id).equals(code),"attaching paper keeps one record and immutable automatic code");
        check(jdbc.queryForObject("SELECT has_paper_copy AND has_electronic_copy AND record_state='STORED' FROM medical_records WHERE id=?",Boolean.class,id) && records.locationHistory(id).size()==1,"physical attachment records storage position and both flags");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM electronic_record_events WHERE record_id=?",Integer.class,id)==3,"physical attachment preserves original submission and approval history");
        expect(BusinessException.class,()->records.create(new RecordForm(UUID.randomUUID(),0L,null,code,"SYNTH-UNIFIED","Duplicate attempted",1980,"MALE",department,type,false,TODAY.minusDays(3),TODAY.minusDays(1),null,List.of(),null,"NORMAL",null,null,null),"DRAFT","archivist1"));
        var attached=get(reader,"/reader/records?q="+encode(code));
        check(attached.body().contains("Giấy và điện tử") && attached.body().contains("data-borrow-record"),"same electronic code becomes borrowable after physical attachment");
        check(jdbc.queryForObject("SELECT relkind::text FROM pg_class WHERE oid='electronic_records'::regclass",String.class).equals("v"),"electronic_records is only a read-only projection, not redundant storage");
        expect(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE electronic_records SET patient_name='Forbidden view write' WHERE id=?",id));
        check(jdbc.queryForObject("SELECT COUNT(*) FROM medical_records WHERE record_code=?",Integer.class,code)==1,"compatibility projection cannot create divergent record contents");
        long next=jdbc.queryForObject("SELECT last_value+1 FROM record_code_counters WHERE year=?",Long.class,TODAY.getYear());
        String reserved="MRM-REC-"+TODAY.getYear()+"-"+String.format(java.util.Locale.ROOT,"%06d",next);
        jdbc.update("INSERT INTO medical_records(id,create_token,create_fingerprint,archive_number,patient_name,sex,record_state,physical_status,quality_status,created_by,updated_by) SELECT ?,?,? ,?,'Legacy reserved code','UNKNOWN','DRAFT','NOT_STORED','NORMAL',id,id FROM app_users WHERE username='archivist1'",UUID.randomUUID(),UUID.randomUUID(),"reserved-fixture",reserved);
        UUID generated=submissions.create(datedForm(UUID.randomUUID(),0L,"Collision-safe automatic code",1980,"UNKNOWN",null),"reader1");
        check(!submissions.detail(generated,"reader1").get("record_code").equals(reserved),"automatic generator skips a historical paper code in its namespace");
        expect(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("UPDATE medical_records SET has_paper_copy=FALSE,has_electronic_copy=FALSE WHERE id=?",paper));

    }
    private static String encode(Object x){return URLEncoder.encode(Objects.toString(x,""),StandardCharsets.UTF_8);}
    private static String body(Object...pairs){var parts=new ArrayList<String>();for(int i=0;i<pairs.length;i+=2)parts.add(encode(pairs[i])+"="+encode(pairs[i+1]));return String.join("&",parts);}
    private static String csrf(String html){var m=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);if(!m.find())throw new AssertionError("Missing CSRF");return m.group(1);}
    private static HttpResponse<String> post(HttpClient c,String path,String token,Object...data)throws Exception{return c.send(HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(20)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body(data)+"&_csrf="+encode(token))).build(),HttpResponse.BodyHandlers.ofString());}
    // These regression scenarios exercise established accounts. Initial setup and its HTTP gate
    // are independently tested end-to-end by InitialCredentialSetupCheck with unmodified account state.
    private static HttpClient login(String name)throws Exception{jdbc.update("UPDATE app_users SET must_change_password=FALSE WHERE username=?",name);var c=client();String token=csrf(get(c,"/login").body());check(post(c,"/login",token,"username",name,"password",LOGIN).statusCode()==302,"HTTP login "+name);return c;}
    private static void httpChecks()throws Exception {
        for(String role:List.of("reader","archivist","director","admin")) {
            var toastBrowser=login(role+"1");
            check(get(toastBrowser,"/home").statusCode()==302,"login routes through home for "+role);
            var firstHome=get(toastBrowser,"/"+role+"/home");
            check(firstHome.statusCode()==200 && firstHome.body().contains("data-login-toast") && firstHome.body().contains("/js/login-toast.js") && firstHome.body().contains("data-dismiss-toast"),"successful login prepares dismissible toast for "+role);
            var reloaded=get(toastBrowser,"/"+role+"/home");
            check(!reloaded.body().contains("data-login-toast") && !reloaded.body().contains("Bạn đã đăng nhập thành công."),"home reload never repeats login success toast for "+role);
        }

        var staleBrowser=client();
        String oldLoginToken=csrf(get(staleBrowser,"/login").body());
        check(post(staleBrowser,"/login",oldLoginToken,"username","reader1","password",LOGIN).headers().firstValue("location").orElse("").endsWith("/home"),"initial login succeeds before testing stale form");
        var staleLogin=post(staleBrowser,"/login",oldLoginToken,"username","archivist1","password",LOGIN);
        check(staleLogin.statusCode()==302 && staleLogin.headers().firstValue("location").orElse("").endsWith("/login?expired"),"stale login CSRF redirects to fresh form instead of 403");
        var freshLogin=get(staleBrowser,"/login?expired");
        check(freshLogin.statusCode()==200 && freshLogin.body().contains("biểu mẫu đã hết hiệu lực"),"expired login renders helpful message and fresh CSRF");
        check(post(staleBrowser,"/login",csrf(freshLogin.body()),"username","archivist1","password",LOGIN).headers().firstValue("location").orElse("").endsWith("/home"),"fresh login form switches to archivist successfully");
        check(get(staleBrowser,"/archivist/home").statusCode()==200,"archivist home accessible after refreshing stale login");
        check(get(staleBrowser,"/admin/home").statusCode()==403,"login CSRF recovery does not grant another role access");
        var missingLogin=post(client(),"/login","","username","admin1","password",LOGIN);
        check(missingLogin.statusCode()==302 && missingLogin.headers().firstValue("location").orElse("").endsWith("/login?expired"),"login without CSRF cannot authenticate and recovers to fresh form");
        var reader=login("reader1");var arch=login("archivist1");var director=login("director1");var admin=login("admin1");
        UUID accessRecord=create("PERMISSION",false);
        var permissionRedirect=get(reader,"/reader/records/"+accessRecord);
        check(permissionRedirect.statusCode()==200 && permissionRedirect.body().contains("FR_PAT_PERMISSION") && !permissionRedirect.body().contains("PRIVATE_RECORD_MARKER"),"published patient metadata opens directly without a paper borrowing approval");
        check(records.basicDetail(accessRecord,"reader1").id().equals(accessRecord),"basic published metadata is independent of the paper borrowing workflow");
        var permissionForm=get(reader,"/reader/requests/new?recordId="+accessRecord);
        check(permissionForm.statusCode()==200 && permissionForm.body().contains("Đăng ký mượn hồ sơ") && !permissionForm.body().contains("FR_PAT_PERMISSION"),"permission form shows minimal preview without detailed metadata");
        UUID accessRequest=directorRequest(accessRecord);
        check(!records.readerMayView(accessRecord,"reader1"),"SUBMITTED does not grant record detail");
        UUID workNotice=jdbc.queryForObject("SELECT id FROM notifications WHERE request_id=? AND recipient_role='ARCHIVIST'",UUID.class,accessRequest);
        var staffOpen=post(arch,"/notifications/"+workNotice+"/open",csrf(get(arch,"/notifications").body()));
        check(staffOpen.statusCode()==302 && staffOpen.headers().firstValue("location").orElse("").endsWith("/archivist/requests/"+accessRequest),"Archivist bell opens work in Archivist namespace");
        reading.review(accessRequest,version(accessRequest),"PENDING_APPROVAL","Ready","archivist1");
        check(get(reader,"/reader/records/"+accessRecord).statusCode()==200,"pending borrowing approval does not hide published patient metadata");
        UUID approvalNotice=jdbc.queryForObject("SELECT n.id FROM notifications n JOIN app_users u ON u.id=n.recipient_id WHERE n.request_id=? AND u.username='director1'",UUID.class,accessRequest);
        var directorOpen=post(director,"/notifications/"+approvalNotice+"/open",csrf(get(director,"/notifications").body()));
        check(directorOpen.statusCode()==302 && directorOpen.headers().firstValue("location").orElse("").endsWith("/director/requests/"+accessRequest),"Director bell opens pending approval in Director namespace");
        reading.decide(accessRequest,version(accessRequest),true,"Approved",SECRET,"director1");
        var allowedDetail=get(reader,"/reader/records/"+accessRecord);
        check(allowedDetail.statusCode()==200 && allowedDetail.body().contains("FR_PAT_PERMISSION") && !allowedDetail.body().contains("PRIVATE_RECORD_MARKER"),"approval exposes authorized patient metadata without internal notes");
        check(get(login("fr_reader2"),"/reader/records/"+accessRecord).statusCode()==200,"other Reader can open basic published metadata without inheriting a loan permission");
        var requestList=get(reader,"/reader/requests");
        check(requestList.body().contains("/reader/records/"+accessRecord) && requestList.body().contains("href=\"/reader/requests/"+accessRequest+"\"") && !requestList.body().contains("/reader/requests/"+accessRequest+"/cancel"),"approved request retains record and progress links without cancellation");
        expect(BusinessException.class,()->reading.cancel(accessRequest,version(accessRequest),"Stale cancellation","reader1"));
        check(state(accessRequest).equals("APPROVED"),"Reader cannot cancel after Director approval");
        check(requestList.body().contains("Đã duyệt, chờ lịch") && !requestList.body().contains("SUBMITTED: mới gửi"),"request states localized without code legend");
        var approvedProgress=get(reader,"/reader/requests/"+accessRequest);
        check(!approvedProgress.body().contains("data-cancel-request"),"approved timeline hides Reader cancellation");
        check(approvedProgress.statusCode()==200 && approvedProgress.body().contains("aria-current=\"step\"") && approvedProgress.body().contains("Xác nhận ngày nhận") && !approvedProgress.body().contains("data-open-approved-record"),"approved progress waits for staff schedule without completion button");
        reading.amend(accessRequest,version(accessRequest),"Changed purpose","reader1");
        check(!records.readerMayView(accessRecord,"reader1") && get(reader,"/reader/records/"+accessRecord).statusCode()==200,"amending revokes loan permission but published metadata stays available");
        approve(accessRequest);reading.confirm(accessRequest,version(accessRequest),TODAY,"Access schedule","archivist1");
        var readyProgress=get(reader,"/reader/requests/"+accessRequest);
        check(readyProgress.body().contains("data-open-approved-record") && readyProgress.body().contains("/reader/records/"+accessRecord) && readyProgress.body().contains("Đã hoàn tất duyệt và xác nhận lịch"),"confirmed valid request provides record detail button");
        jdbc.update("UPDATE reading_requests SET registered_on=? WHERE id=?",TODAY.minusDays(3),accessRequest);
        jdbc.update("UPDATE reading_sessions SET reading_on=? WHERE request_id=?",TODAY.minusDays(1),accessRequest);
        check(!records.readerMayView(accessRecord,"reader1"),"approval expires after last reading day");
        check(!get(reader,"/reader/requests/"+accessRequest).body().contains("data-open-approved-record"),"expired reading permission never shows record button");
        jdbc.update("UPDATE reading_requests SET registered_on=? WHERE id=?",TODAY,accessRequest);
        jdbc.update("UPDATE reading_sessions SET reading_on=? WHERE request_id=?",TODAY.plusDays(1),accessRequest);
        check(records.readerMayView(accessRecord,"reader1"),"approval grants access before future reading day starts");
        reading.cancel(accessRequest,version(accessRequest),"Cancel permission","archivist1");
        check(!records.readerMayView(accessRecord,"reader1"),"cancellation revokes detail permission");
        check(get(admin,"/admin/home").statusCode()==200,"admin home accessible with fresh login form");
        check(get(client(),"/notifications").statusCode()==302,"anonymous cannot access internal notifications");
        UUID xss=submit(create("NOTICE_HTTP",false));reading.review(xss,0,"NEEDS_INFO","Cần bổ sung <script>alert(1)</script>","archivist1");
        var notices=get(reader,"/notifications");
        check(notices.statusCode()==200 && notices.body().contains("Thông báo của bạn") && notices.body().contains("notification-bell"),"notification inbox and bell render with nonempty rows");
        var escapedProgress=get(reader,"/reader/requests/"+xss);
        check(escapedProgress.statusCode()==200 && !escapedProgress.body().contains("<script>alert(1)</script>"),"timeline never renders executable processing notes");
        check(get(login("fr_reader2"),"/reader/requests/"+xss).statusCode()==404,"Reader cannot view another Reader timeline");
        check(notices.body().contains("&lt;script&gt;") && !notices.body().contains("<script>alert(1)</script>"),"notification body escapes script content");
        UUID ownNotice=jdbc.queryForObject("SELECT id FROM notifications WHERE recipient_role='READER' AND request_id=?",UUID.class,xss);
        String noticeToken=csrf(notices.body());
        check(post(reader,"/notifications/"+ownNotice+"/read","",new Object[0]).statusCode()==403,"notification mark-read requires CSRF");
        var secondReader=login("fr_reader2");String secondToken=csrf(get(secondReader,"/reader/home").body());
        check(post(secondReader,"/notifications/"+ownNotice+"/open",secondToken,new Object[0]).statusCode()==404,"another reader cannot open notification by guessing UUID");
        check(post(secondReader,"/notifications/"+ownNotice+"/read",secondToken,new Object[0]).statusCode()==404,"another reader cannot mark notification read");
        check(jdbc.queryForObject("SELECT read_at IS NULL FROM notifications WHERE id=?",Boolean.class,ownNotice),"forbidden HTTP operations preserve unread state");
        check(post(reader,"/notifications/"+ownNotice+"/open",noticeToken,new Object[0]).headers().firstValue("location").orElse("").endsWith("/reader/requests/"+xss),"opening own notice marks read and redirects to server-owned request");
        check(jdbc.queryForObject("SELECT read_at IS NOT NULL FROM notifications WHERE id=?",Boolean.class,ownNotice),"open request persists read timestamp");
        check(get(reader,"/notifications?unread=true").statusCode()==200,"unread inbox filter renders");
        check(get(reader,"/notifications?page=1").statusCode()==200,"second notification page renders");
        check(get(admin,"/notifications").statusCode()==200 && !get(admin,"/notifications").body().contains("Cần bổ sung"),"admin notification page does not expose reader messages");
        check(post(reader,"/notifications/read-all",noticeToken,new Object[0]).statusCode()==302 && notifications.unread("reader1")==0,"HTTP mark-all-read updates own badge count");
        check(get(reader,"/reader/home").body().contains("Thông báo: 0 chưa đọc"),"bell accessible label updates after mark-all-read");
        for(String path:List.of("/archivist/departments","/archivist/record-types","/archivist/retention-policies"))check(get(arch,path).body().contains("notification-bell"),"legacy catalog page includes notification bell: "+path);
        for(var entry:Map.of("reader",reader,"archivist",arch,"director",director).entrySet())check(get(entry.getValue(),"/"+entry.getKey()+"/requests").statusCode()==200,"request list renders "+entry.getKey());
        check(get(admin,"/admin/accounts").statusCode()==200,"account management template renders");
        check(get(director,"/director/approval-secret").statusCode()==200,"second-secret template renders");
        String adminToken=csrf(get(admin,"/admin/accounts").body());
        check(get(admin,"/admin/home").body().contains("Tạo tài khoản") && !get(admin,"/admin/home").body().contains("createRole="),"Admin home has one account creation entry");
        check(get(admin,"/admin/accounts/new").body().contains("value=\"READER\">Bác sĩ") && get(admin,"/admin/accounts/new").body().contains("value=\"ARCHIVIST\">Nhân viên lưu trữ"),"unified form displays localized role labels");
        check(post(admin,"/admin/accounts",adminToken,"displayName","HTTP Reader","citizenId","0000000001","birthDate","1990-01-02","gender","MALE","role","READER","department",department,"email","").statusCode()==200 && jdbc.queryForObject("SELECT COUNT(*) FROM app_users WHERE username='reader.h02011990@mrm.com'",Integer.class)==1,"HTTP account create automatically generates credentials and persists");
        // Normalize these established-user regression fixtures after testing automatic creation.
        // StaffManagementCheck independently authenticates with the actual generated password.
        jdbc.update("UPDATE app_users SET username='fr_http_reader',password_hash=? WHERE username='reader.h02011990@mrm.com'",new mrm.config.SecurityConfig().passwordEncoder().encode(LOGIN));
        check(jdbc.queryForObject("SELECT COUNT(*) FROM employees e JOIN app_users u ON u.id=e.account_id WHERE u.username='fr_http_reader' AND e.citizen_id='0000000001' AND e.role='READER'",Integer.class)==1,"unified HTTP form saves linked employee profile");
        check(get(login("fr_http_reader"),"/reader/home").statusCode()==200,"newly created Reader can log in to Reader home");
        check(post(admin,"/admin/accounts",adminToken,"displayName","HTTP Archivist","citizenId","0000000002","birthDate","1991-02-03","gender","FEMALE","role","ARCHIVIST","department",department,"email","").statusCode()==200,"Admin creates Archivist using automatic account form");
        jdbc.update("UPDATE app_users SET username='fr_http_archivist',password_hash=? WHERE username='archivist.h03021991@mrm.com'",new mrm.config.SecurityConfig().passwordEncoder().encode(LOGIN));
        var newArchivist=login("fr_http_archivist");
        check(get(newArchivist,"/archivist/home").statusCode()==200 && get(newArchivist,"/archivist/records/new").statusCode()==200,"new Archivist receives storage permissions");
        check(get(newArchivist,"/admin/accounts").statusCode()==403,"new Archivist cannot manage accounts");
        check(post(admin,"/admin/accounts",adminToken,"displayName","Duplicate","citizenId","0000000002","birthDate","1991-02-03","gender","OTHER","role","READER","department",department,"email","").statusCode()==200 && jdbc.queryForObject("SELECT COUNT(*) FROM employees WHERE citizen_id='0000000002'",Integer.class)==1,"duplicate citizen ID is rejected inline without a second account");
        var httpAccount=accounts.list().stream().filter(x->x.get("username").equals("fr_http_reader")).findFirst().orElseThrow();
        var management=get(admin,"/admin/accounts");var creation=get(admin,"/admin/accounts/new");
        check(management.statusCode()==200 && !management.body().contains("name=\"password\"") && !management.body().contains("name=\"birthDate\""),"management contains table and filters without inline edit/create forms");
        check(creation.statusCode()==200 && creation.body().contains("name=\"citizenId\"") && !creation.body().contains("<table") && !creation.body().contains("fr_http_reader"),"create page does not contain account list");
        check(management.body().contains("/admin/accounts/"+httpAccount.get("id")+"/edit"),"account name links to separate edit page");
        var editAccount=get(admin,"/admin/accounts/"+httpAccount.get("id")+"/edit");
        check(editAccount.statusCode()==200 && editAccount.body().contains("value=\"HTTP Reader\"") && editAccount.body().contains("0000000001") && !editAccount.body().contains(LOGIN),"separate edit page loads selected account and employee without password");
        check(accounts.search("fr_http","").size()==2,"name-only account search matches usernames");
        check(accounts.search("", "ARCHIVIST").stream().allMatch(a->a.get("role").equals("ARCHIVIST")),"role-only account search accepts blank name");
        check(accounts.search("HTTP Reader", "READER").size()==1 && accounts.search("HTTP Reader", "ARCHIVIST").isEmpty(),"combined name and role search applies both filters");
        check(accounts.search("   ","").size()==accounts.list().size(),"empty search fields return all accounts");
        check(accounts.search("","","0000000001").getFirst().get("username").equals("fr_http_reader"),"citizen ID search preserves leading zeros and matches linked employee");
        check(accounts.search("HTTP Reader","READER","0000000001").size()==1 && accounts.search("HTTP Reader","ARCHIVIST","0000000001").isEmpty(),"citizen ID combines with name and role using AND");
        check(accounts.search("","","9999999999").isEmpty(),"unknown citizen ID returns no accounts");
        expect(BusinessException.class,()->accounts.search("","","000000000"));
        check(accounts.search("","","").size()==accounts.list().size(),"empty citizen ID keeps legacy accounts without employee profiles");
        var citizenResults=get(admin,"/admin/accounts?citizenId=0000000001");
        check(citizenResults.statusCode()==200 && citizenResults.body().contains("fr_http_reader") && !citizenResults.body().contains("fr_http_archivist") && citizenResults.body().contains("value=\"0000000001\""),"HTTP citizen ID filter binds and retains input");
        check(accounts.search("%","").isEmpty() && accounts.search("' OR 1=1 --","").isEmpty(),"search treats wildcard and SQL syntax as literal text");
        var filtered=get(admin,"/admin/accounts?name="+encode("HTTP Reader")+"&role=READER");
        check(filtered.statusCode()==200 && filtered.body().contains("fr_http_reader") && !filtered.body().contains("fr_http_archivist"),"HTTP filters bind independently and combine correctly");
        check(get(admin,"/admin/accounts?name=not_found_anywhere&role=READER").body().contains("Không có tài khoản phù hợp"),"empty account results render helpful message");
        check(get(admin,"/admin/accounts/"+UUID.randomUUID()+"/edit").statusCode()==404,"unknown account edit returns 404");
        check(get(reader,"/admin/accounts/new").statusCode()==403 && get(reader,"/admin/accounts/"+httpAccount.get("id")+"/edit").statusCode()==403,"Reader cannot access separate create/edit pages");
        check(post(admin,"/admin/accounts/"+httpAccount.get("id"),adminToken,"version",0,"displayName","HTTP Reader Changed","role","READER","status","DISABLED","department",department,"email","","password","").statusCode()==302 && jdbc.queryForObject("SELECT status FROM app_users WHERE username='fr_http_reader'",String.class).equals("DISABLED"),"HTTP account edit locks or disables without revealing hash");
        String secretToken=csrf(get(director,"/director/approval-secret").body());
        check(post(director,"/director/approval-secret",secretToken,"currentPassword",LOGIN,"secret",SECRET).statusCode()==302,"HTTP second-password setup accepts correct current password");
        check(get(reader,"/admin/accounts").statusCode()==403,"reader cannot manage accounts");
        check(get(admin,"/reader/requests").statusCode()==403,"admin cannot access clinical requests");
        UUID r=create("HTTP",true);var form=get(reader,"/reader/requests/new?recordId="+r);check(form.statusCode()==200,"request form renders");String token=csrf(form.body());
        check(form.body().contains("data-record-dialog") && form.body().contains("Chọn hồ sơ") && !form.body().contains("type=\"date\"") && !form.body().contains("name=\"dates\""),"Reader registration shows record attachment picker without dates");
        check(get(reader,"/reader/requests/new").statusCode()==200,"registration menu opens without a preselected record");
        var candidates=get(reader,"/reader/requests/records?q="+encode("FR_MED_HTTP"));
        check(candidates.statusCode()==200 && candidates.body().contains(r.toString()) && !candidates.body().contains("PRIVATE_RECORD_MARKER") && !candidates.body().contains("patientCode"),"record picker search only exposes minimal published candidate metadata");
        var expanded=get(reader,"/reader/requests/records/"+r);
        check(expanded.statusCode()==200 && expanded.body().contains("FR_MED_HTTP") && expanded.body().contains("departmentName") && !expanded.body().contains("birthYear") && !expanded.body().contains("PRIVATE_RECORD_MARKER"),"expanded identity API preserves approval boundary and excludes private fields");
        check(form.body().contains("<details") && form.body().contains("Thông tin hồ sơ"),"preselected record renders accessible expandable information");
        String nav=form.body().substring(form.body().indexOf("<nav"),form.body().indexOf("</nav>"));
        check(nav.indexOf("Trang chủ")<nav.indexOf("Tra cứu hồ sơ") && nav.indexOf("Tra cứu hồ sơ")<nav.indexOf("Đăng ký mượn hồ sơ"),"Reader navigation follows requested order");
        check(get(reader,"/reader/requests/records?q="+encode("FR_MED_DEATH")).body().contains("FR Patient DEATH"),"deceased records available in borrow picker with guarded approval route");
        check(get(admin,"/reader/requests/records").statusCode()==403,"Admin cannot call Reader record picker API");
        UUID forgedDateToken=UUID.randomUUID();
        var forgedDate=post(reader,"/reader/requests",token,"recordIds",r,"token",forgedDateToken,"purpose","Forged dates","dates",TODAY);
        check(forgedDate.statusCode()==200 && jdbc.queryForObject("SELECT COUNT(*) FROM reading_request_batches WHERE submit_token=?",Integer.class,forgedDateToken)==0,"Reader cannot inject a chosen date into registration POST");
        UUID another=create("HTTP_BATCH",false),batchFirst=create("HTTP_BATCH_FIRST",false),batchToken=UUID.randomUUID();
        var multi=post(reader,"/reader/requests",token,"recordIds",batchFirst,"recordIds",another,"token",batchToken,"purpose","Two selected records");
        check(multi.statusCode()==302 && multi.headers().firstValue("location").orElse("").endsWith("/reader/requests") && jdbc.queryForObject("SELECT COUNT(*) FROM reading_requests q JOIN reading_request_batches b ON b.id=q.batch_id WHERE b.submit_token=?",Integer.class,batchToken)==2,"HTTP multi-record selection creates batch and opens tracking list");
        var submitted=post(reader,"/reader/requests",token,"recordIds",r,"token",UUID.randomUUID(),"purpose","HTTP study");
        check(submitted.statusCode()==302,"HTTP submit reading request");String location=submitted.headers().firstValue("location").orElseThrow();UUID id=UUID.fromString(location.substring(location.lastIndexOf('/')+1));
        var history=get(reader,"/reader/requests/"+id);
        check(history.statusCode()==200 && history.body().contains("Lịch sử xử lý"),"Reader history renders");
        check(history.body().contains("request-timeline-node") && history.body().contains("aria-current=\"step\"") && history.body().contains("Nhân viên lưu trữ") && history.body().contains("Giám đốc"),"pending timeline shows actual and remaining approval nodes with responsible roles");
        check(!history.body().contains("data-open-approved-record"),"new request has no premature record button");
        check(!history.body().contains("/amend") && history.body().contains("data-cancel-request") && !history.body().contains("Lịch nhận và giao–trả"),"Reader tracking allows cancellation without editing controls");
        var pendingList=get(reader,"/reader/requests");
        check(!pendingList.body().contains("Hiển thị 10 yêu cầu mỗi trang") && !pendingList.body().contains("Tìm thấy <strong"),"request list removes result count summary");
        check(pendingList.body().contains("/reader/requests/"+id+"/cancel") && pendingList.body().contains("Hủy yêu cầu") && pendingList.body().contains("Mới gửi"),"pending request has localized state and inline cancellation");
        check(pendingList.body().contains("href=\"/reader/requests/"+id+"\""),"pending request retains tracking link");

        for(String pendingState:List.of("SUBMITTED","NEEDS_INFO","PENDING_APPROVAL")) {
            UUID cancellable=directorRequest(create("CANCEL_HTTP_"+pendingState,false));
            if(!pendingState.equals("SUBMITTED"))reading.review(cancellable,version(cancellable),pendingState,"Check cancellation","archivist1");
            check(post(reader,"/reader/requests/"+cancellable+"/cancel",null,"version",version(cancellable),"note","Cancel").statusCode()==403,"cancellation requires CSRF in "+pendingState);
            var outsider=login("fr_reader2");
            check(post(outsider,"/reader/requests/"+cancellable+"/cancel",csrf(get(outsider,"/reader/requests").body()),"version",version(cancellable),"note","Cancel").statusCode()==403,"another Reader cannot cancel in "+pendingState);
            var cancelHistory=get(reader,"/reader/requests/"+cancellable);
            check(cancelHistory.body().contains("data-cancel-request") && cancelHistory.body().contains("/reader/requests/"+cancellable+"/cancel"),"timeline offers cancellation in "+pendingState);
            long oldVersion=version(cancellable);
            var cancelled=post(reader,"/reader/requests/"+cancellable+"/cancel",csrf(cancelHistory.body()),"version",oldVersion,"note","Reader cancels pending request");
            check(cancelled.statusCode()==302 && cancelled.headers().firstValue("location").orElse("").endsWith("/reader/requests") && state(cancellable).equals("CANCELLED"),"Reader cancels "+pendingState+" from list and returns to list");
            check(jdbc.queryForObject("SELECT COUNT(*) FROM request_events WHERE request_id=? AND action='CANCEL' AND actor='reader1'",Integer.class,cancellable)==1,"cancellation records Reader history in "+pendingState);
            post(reader,"/reader/requests/"+cancellable+"/cancel",csrf(pendingList.body()),"version",oldVersion,"note","Duplicate cancel");
            check(jdbc.queryForObject("SELECT COUNT(*) FROM request_events WHERE request_id=? AND action='CANCEL'",Integer.class,cancellable)==1,"stale cancellation cannot create duplicate history");
            var closedList=get(reader,"/reader/requests");
            check(!get(reader,"/reader/requests/"+cancellable).body().contains("data-cancel-request"),"cancelled timeline hides cancellation form");
            String cancelledRow=closedList.body().substring(closedList.body().indexOf("href=\"/reader/records/"+jdbc.queryForObject("SELECT record_id FROM reading_requests WHERE id=?",UUID.class,cancellable)+"\""));
            cancelledRow=cancelledRow.substring(0,cancelledRow.indexOf("</tr>"));
            check(cancelledRow.contains("Đã hủy") && !cancelledRow.contains("Đã hủy phần chưa thực hiện") && Pattern.compile("<td\\s*>"+TODAY+"</td>\\s*<td\\s*>\\s*</td>").matcher(cancelledRow).find() && !cancelledRow.contains("Lý do mượn") && !cancelledRow.contains("Chờ nhân viên lưu trữ xếp lịch"),"cancelled row retains registration date, hides pickup and purpose");
            var cancelInbox=get(reader,"/notifications?scope=handled");
            check(cancelInbox.body().contains("Yêu cầu đã hủy") && !cancelInbox.body().contains("Yêu cầu / ngày đọc chưa thực hiện đã hủy"),"cancellation inbox uses short Vietnamese title");

            check(closedList.body().contains("href=\"/reader/requests/"+cancellable+"\"") && !closedList.body().contains("/reader/requests/"+cancellable+"/cancel"),"cancelled request retains progress link without cancellation control");
        }
        String archToken=csrf(get(arch,"/archivist/requests/"+id).body());
        check(post(arch,"/archivist/requests/"+id+"/review",archToken,"version",version(id),"result","PENDING_APPROVAL","note","HTTP checked").statusCode()==302 && state(id).equals("PENDING_APPROVAL"),"HTTP review submits to director");
        String directorToken=csrf(get(director,"/director/requests/"+id).body());
        check(post(director,"/director/requests/"+id+"/decide",directorToken,"version",version(id),"approve",true,"note","HTTP approved","secret",SECRET).statusCode()==302 && state(id).equals("APPROVED"),"HTTP step-up approval succeeds");
        check(post(arch,"/archivist/requests/"+id+"/confirm",archToken,"version",version(id),"pickupOn",TODAY,"note","HTTP room A").statusCode()==302 && state(id).equals("CONFIRMED"),"HTTP confirms reading schedule");
        var scheduleProgress=get(reader,"/reader/requests/"+id);
        String pickupDate=TODAY.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        check(scheduleProgress.statusCode()==200 && scheduleProgress.body().contains("Ngày nhận:") && scheduleProgress.body().contains(pickupDate)
            && scheduleProgress.body().contains("Phòng nhận:") && scheduleProgress.body().contains("HTTP room A"),"Reader confirmation node displays scheduled date and pickup room");
        var nodes=mrm.workflow.ReadingProgress.nodes(reading.detail(id,"reader1"));
        check(nodes.stream().filter(node->node.pickupDates()!=null).count()==1 && nodes.stream().anyMatch(node->"HTTP room A".equals(node.pickupRoom())),"pickup details appear only under the actual confirmation node");
        UUID s=session(id,TODAY);
        check(post(arch,"/archivist/requests/"+id+"/sessions/"+s+"/handover",archToken,"version",version(id),"condition","HTTP normal").statusCode()==302 && records.details(r).physicalStatus().equals("READING"),"HTTP handover executes");
        check(!get(reader,"/reader/requests/"+id).body().contains(slot.toString()),"reader request response has no location UUID");
        String held=get(reader,"/reader/records/"+r).body();
        check(held.contains("Thông tin hồ sơ bệnh án") && !held.contains("Đăng ký mượn hồ sơ tử vong phải có lý do") && !held.contains("Mượn hồ sơ giấy") && !held.contains("Yêu cầu mượn bản giấy được kiểm tra"),"Reader who received paper sees record information without the three borrowing prompts");
        check(records.paperHandedToReader(r,"reader1") && !records.paperHandedToReader(r,"fr_reader2"),"paper handover presentation is scoped to the actual borrower");
        check(post(arch,"/archivist/requests/"+id+"/sessions/"+s+"/return",archToken,"version",version(id),"destination",slot,"quality","NORMAL","condition","HTTP complete").statusCode()==302 && records.details(r).physicalStatus().equals("IN_STORAGE"),"HTTP return executes");
        for(String kind:ReportService.TYPES.keySet())check(get(arch,"/archivist/reports?type="+kind).statusCode()==200,"HTTP report template "+kind);
        var exported=get(arch,"/archivist/reports?type=records&download=true");check(exported.statusCode()==200 && exported.headers().firstValue("Content-Disposition").orElse("").contains(".csv") && exported.body().contains("FR Patient"),"HTTP authorized CSV exports selected dataset");
        check(get(reader,"/archivist/reports?type=records&download=true").statusCode()==403,"reader cannot export clinical reports");
        check(get(admin,"/admin/reports?type=records&download=true").statusCode()==403,"admin cannot export clinical metadata");
        check(get(admin,"/admin/reports").statusCode()==200,"admin technical audit renders");
        check(get(arch,"/archivist/inventories").statusCode()==200,"inventory list renders");
        var newBatch=post(arch,"/archivist/inventories",archToken,"name","HTTP stocktake","location",slot);check(newBatch.statusCode()==302,"HTTP inventory creates batch");
        String batchUrl=newBatch.headers().firstValue("location").orElseThrow();UUID newBatchId=UUID.fromString(batchUrl.substring(batchUrl.lastIndexOf('/')+1));
        UUID inventoryRecord=jdbc.queryForObject("SELECT record_id FROM inventory_items WHERE batch_id=? LIMIT 1",UUID.class,newBatchId);
        check(post(arch,"/archivist/inventories/"+newBatchId+"/check",archToken,"record",inventoryRecord,"observed","FOUND","location","","note","HTTP checked").statusCode()==302 && jdbc.queryForObject("SELECT observed FROM inventory_items WHERE batch_id=? AND record_id=?",String.class,newBatchId,inventoryRecord).equals("FOUND"),"HTTP inventory check binds result");
        UUID batch=jdbc.queryForObject("SELECT id FROM inventory_batches LIMIT 1",UUID.class);
        check(get(arch,"/archivist/inventories/"+batch).statusCode()==200,"closed inventory detail renders");
        check(get(director,"/director/inventories").statusCode()==403,"Director inventory feature is removed");
        check(get(director,"/director/inventories/"+batch).statusCode()==403,"director cannot access exact inventory locations");
        var c=login("fr_reader2");var a=accounts.list().stream().filter(x->x.get("username").equals("fr_reader2")).findFirst().orElseThrow();
        accounts.update((UUID)a.get("id"),((Number)a.get("version")).longValue(),"Reader Two","READER","LOCKED",department,null,null,"admin1");
        check(get(c,"/reader/requests").statusCode()==302,"existing session revoked after administrative account lock");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action='LOGIN'",Integer.class)>0,"login audit persisted");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action='REPORT_EXPORT'",Integer.class)>0,"report export audited");
        check(post(arch,"/archivist/requests/"+id+"/cancel","","version",version(id),"note","No CSRF").statusCode()==403,"workflow POST requires CSRF");
        check(post(reader,"/logout",csrf(get(reader,"/reader/home").body())).statusCode()==302 && jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE actor='reader1' AND action='LOGOUT'",Integer.class)>0,"logout persists audit and ends session");
    }
    private static void personalMenuAndDirectorChecks(mrm.authentication.DashboardService dashboard)throws Exception {
        for(String username:List.of("reader1","archivist1","director1")){
            var browser=login(username);
            String home=get(browser,"/home").headers().firstValue("location").orElseThrow();
            String html=get(browser,URI.create(home).getRawPath()).body();
            check(html.contains("data-account-menu") && html.contains("/account/profile") && html.contains("/account/password"),"personal dropdown available to "+username);
            String nav=html.substring(html.indexOf("<nav"),html.indexOf("</nav>")+6);
            check(!nav.contains("/director/approval-secret"),"second password removed from primary navigation for "+username);
            check(get(browser,"/account/profile").statusCode()==200 && get(browser,"/account/password").statusCode()==200,"own profile and password form render for "+username);
        }
        var admin=login("admin1");
        check(get(admin,"/account/profile").statusCode()==403 && get(admin,"/account/password").statusCode()==403,"new personal pages keep requested three-role scope");
        check(get(client(),"/account/profile").statusCode()==302,"anonymous profile access redirects to login");
        var director=login("director1");var stats=dashboard.load("director1").stats();
        for(int i=0;i<stats.size();i++){
            var stat=stats.get(i);String queue=List.of("pending","approved","published").get(i);
            var work=dashboard.directorWork("director1",queue);
            check(stat.url()!=null && get(director,stat.url()).statusCode()==200,"Director statistic links to "+queue+" list");
            check(stat.value()==work.requests().size()+work.declarations().size(),"Director "+queue+" statistic matches destination lists");
        }
        expect(org.springframework.security.access.AccessDeniedException.class,()->dashboard.directorWork("reader1","pending"));
        expect(BusinessException.class,()->dashboard.directorWork("director1","unknown"));
        accounts.createWithEmployee("fr_personal_reader","Họ tên cá nhân thử nghiệm","READER",department,"self@example.invalid",LOGIN,"0000000901",LocalDate.of(1996,4,12),"FEMALE","admin1");
        accounts.create("fr_personal_other","OTHER_PRIVATE_PERSON","READER",null,null,LOGIN,"admin1");
        var browser=login("fr_personal_reader");var stale=login("fr_personal_reader");
        String profile=get(browser,"/account/profile?username=fr_personal_other").body();
        check(profile.contains("0000000901") && profile.contains("12/04/1996") && profile.contains("Họ tên cá nhân thử nghiệm") && !profile.contains("OTHER_PRIVATE_PERSON"),"personal page reads own employee fields and ignores forged identity");
        check(!profile.contains("password_hash") && !profile.contains(LOGIN),"personal page never exposes password or hash");
        String token=csrf(get(browser,"/account/password").body());String replacement="Synthetic-personal-new-4096!";
        long before=jdbc.queryForObject("SELECT version FROM app_users WHERE username='fr_personal_reader'",Long.class);
        check(post(browser,"/account/password",null,"currentPassword",LOGIN,"newPassword",replacement,"confirmPassword",replacement).statusCode()==403,"password change requires CSRF");
        post(browser,"/account/password",token,"currentPassword","Wrong-current-password","newPassword",replacement,"confirmPassword",replacement);
        check(jdbc.queryForObject("SELECT version FROM app_users WHERE username='fr_personal_reader'",Long.class)==before,"wrong old password cannot change account");
        expect(BusinessException.class,()->accounts.changeOwnPassword("fr_personal_reader",LOGIN,replacement,"Mismatch"));
        expect(BusinessException.class,()->accounts.changeOwnPassword("fr_personal_reader",LOGIN,"short","short"));
        expect(BusinessException.class,()->accounts.changeOwnPassword("fr_personal_reader",LOGIN,LOGIN,LOGIN));
        expect(BusinessException.class,()->accounts.changeOwnPassword("director1",LOGIN,SECRET,SECRET));
        check(post(browser,"/account/password",token,"currentPassword",LOGIN,"newPassword",replacement,"confirmPassword",replacement).statusCode()==302,"self password update redirects without putting credentials in URL");
        check(get(browser,"/account/profile").statusCode()==200 && get(stale,"/account/profile").headers().firstValue("location").orElse("").endsWith("/login?expired"),"password change keeps current session and revokes another session");
        var fresh=client();String loginToken=csrf(get(fresh,"/login").body());
        check(post(fresh,"/login",loginToken,"username","fr_personal_reader","password",replacement).headers().firstValue("location").orElse("").endsWith("/home"),"new password authenticates after self change");
        var old=client();loginToken=csrf(get(old,"/login").body());
        check(post(old,"/login",loginToken,"username","fr_personal_reader","password",LOGIN).headers().firstValue("location").orElse("").endsWith("/login?error"),"old password no longer authenticates");
        check(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE details LIKE ?",Integer.class,"%"+replacement+"%")==0,"new password absent from audit data");
        var other=login("fr_personal_other");
        for(int i=0;i<5;i++)expect(BusinessException.class,()->accounts.changeOwnPassword("fr_personal_other","Wrong-current-password",replacement,replacement));
        expect(BusinessException.class,()->accounts.changeOwnPassword("fr_personal_other",LOGIN,replacement,replacement));
        expect(org.springframework.security.access.AccessDeniedException.class,()->accounts.changeOwnPassword("admin1",LOGIN,replacement,replacement));
    }
    private static void interfaceCleanupChecks()throws Exception {
        UUID record=create("NOTICE_PRESENTATION",false);
        UUID request=reading.submit(record,"HIDDEN_PURPOSE_IN_ARCHIVIST_NOTICE",UUID.randomUUID(),"reader1");
        UUID notice=jdbc.queryForObject("SELECT n.id FROM notifications n JOIN app_users u ON u.id=n.recipient_id WHERE n.request_id=? AND u.username='archivist1'",UUID.class,request);
        String stored=jdbc.queryForObject("SELECT message FROM notifications WHERE id=?",String.class,notice);
        check(stored.contains("Người yêu cầu:") && stored.contains("reader1") && stored.contains("Ngày giờ yêu cầu:") && !stored.contains("HIDDEN_PURPOSE"),"new Archivist work notice stores requester, record and registration time without purpose");
        jdbc.update("UPDATE notifications SET message='LEGACY_RAW_UUID_PURPOSE_TO_HIDE' WHERE id=?",notice);
        var item=notifications.inbox("archivist1",0,false).items().stream().filter(n->n.id().equals(notice)).findFirst().orElseThrow();
        String code=jdbc.queryForObject("SELECT record_code FROM medical_records WHERE id=?",String.class,record);
        check(item.workInfo()!=null && item.workInfo().requesterUsername().equals("reader1") && item.workInfo().recordCode().equals(code) && item.workInfo().requestedAt()!=null && item.message().isEmpty(),"legacy work notices use structured request metadata instead of old purpose text");
        var arch=login("archivist1");var director=login("director1");
        String inbox=get(arch,"/notifications").body();
        check(inbox.contains("Người yêu cầu") && inbox.contains("Hồ sơ yêu cầu") && inbox.contains("Ngày giờ yêu cầu") && inbox.contains(code) && !inbox.contains("LEGACY_RAW_UUID_PURPOSE_TO_HIDE") && !inbox.contains("HIDDEN_PURPOSE_IN_ARCHIVIST_NOTICE"),"Archivist inbox renders structured metadata without borrowing purpose");
        String report=get(director,"/director/reports?type=audit").body();
        for(String heading:List.of("Người thao tác","Hành động","Loại đối tượng","Mã đối tượng","Kết quả","Nội dung","Thời điểm"))
            check(report.contains("<th>"+heading+"</th>"),"Vietnamese report heading: "+heading);
        check(report.contains("<td>director1</td>") && report.contains("<td>LOGIN</td>"),"translated report table retains actual row values rather than rendering empty cells");
        check(!report.contains("Ngày lọc: hồ sơ") && !report.contains("Nhật ký: actor = người") && !report.contains("<th>actor</th>"),"Director reports remove explanatory paragraphs and English column headers");
        check(!report.contains("value=\"inventory\""),"inventory report option absent for Director");
        check(get(director,"/director/reports?type=inventory").statusCode()==403 && get(director,"/director/reports?type=inventory&download=true").statusCode()==403,"Director cannot access or export inventory through a direct URL");
        expect(org.springframework.security.access.AccessDeniedException.class,()->inventory.list("director1"));
        expect(org.springframework.security.access.AccessDeniedException.class,()->inventory.detail(UUID.randomUUID(),"director1"));
        expect(org.springframework.security.access.AccessDeniedException.class,()->reports.rows("inventory",new ReportService.Filter("","","","",null,null,""),"director1"));
        check(!get(director,"/director/home").body().contains("/director/inventories"),"Director navigation contains no inventory link");
        UUID special=submit(create("DIRECTOR_CLEAN_REVIEW",true));
        reading.review(special,version(special),"PENDING_APPROVAL","Checked","archivist1");
        String detail=get(director,"/director/requests/"+special).body();
        check(detail.contains("name=\"secret\"") && !detail.contains("Lịch nhận và giao–trả") && !detail.contains("Lịch sử xử lý"),"Director approval retains its decision form without circulation or processing-history sections");
    }
    private static void notificationLifecycleChecks(mrm.electronic.ElectronicRecordService electronic)throws Exception {
        accounts.create("fr_lifecycle_reader","Lifecycle Reader","READER",department,null,LOGIN,"admin1");
        accounts.create("fr_lifecycle_other","Other Lifecycle Reader","READER",department,null,LOGIN,"admin1");
        UUID normalRecord=create("NOTICE_LIFECYCLE",false);
        UUID request=reading.submit(normalRecord,"Purpose not displayed",UUID.randomUUID(),"fr_lifecycle_reader");
        UUID work=noticeFor(request,"archivist1","SUBMIT");
        check(notifications.inbox("archivist1",0,false,"pending").items().stream().anyMatch(n->n.id().equals(work) && n.pending()),"new reading request remains in Archivist pending inbox");
        notifications.read(work,"archivist1");
        expect(BusinessException.class,()->notifications.deleteHandled(work,"archivist1"));
        expect(org.springframework.web.server.ResponseStatusException.class,()->notifications.deleteHandled(work,"fr_lifecycle_other"));
        reading.review(request,0,"APPROVED","Approved","archivist1");
        UUID normalSchedule=noticeFor(request,"archivist1","ARCHIVIST_APPROVE");
        check(notifications.inbox("archivist1",0,false,"pending").items().stream().anyMatch(n->n.id().equals(normalSchedule)),"ordinary legacy two-step approvals also retain their pending schedule task");

        check(notifications.inbox("archivist1",0,false,"pending").items().stream().noneMatch(n->n.id().equals(work)),"reviewed work automatically leaves pending inbox even without manual deletion");
        check(notifications.deleteHandled(work,"archivist1") && !notifications.deleteHandled(work,"archivist1"),"processed-notice deletion is soft and idempotent");
        check(Boolean.TRUE.equals(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM notifications WHERE id=?",Boolean.class,work)) && state(request).equals("APPROVED"),"notification deletion preserves its source request and database history");
        expect(org.springframework.web.server.ResponseStatusException.class,()->notifications.openPath(work,"archivist1"));
        UUID readerApproval=noticeFor(request,"fr_lifecycle_reader","ARCHIVIST_APPROVE");
        expect(BusinessException.class,()->notifications.deleteHandled(readerApproval,"fr_lifecycle_reader"));
        reading.confirm(request,version(request),TODAY,"Phòng nhận vòng đời","archivist1");
        UUID confirmed=noticeFor(request,"fr_lifecycle_reader","CONFIRM");
        var result=notifications.inbox("fr_lifecycle_reader",0,false,"handled").items().stream().filter(n->n.id().equals(confirmed)).findFirst().orElseThrow();
        check(!result.pending() && result.workInfo().requesterUsername().equals("fr_lifecycle_reader") && result.workInfo().pickupRoom().equals("Phòng nhận vòng đời"),"Reader result uses common metadata layout with confirmed pickup room");
        UUID special=reading.submit(create("NOTICE_LIFECYCLE_DEATH",true),"Clinical review",UUID.randomUUID(),"fr_lifecycle_reader");
        reading.review(special,version(special),"PENDING_APPROVAL","Checked","archivist1");
        UUID directorWork=noticeFor(special,"director1","REVIEW_PENDING_APPROVAL");
        expect(BusinessException.class,()->notifications.deleteHandled(directorWork,"director1"));
        check(notifications.inbox("director1",0,false,"pending").items().stream().anyMatch(n->n.id().equals(directorWork) && n.workInfo()!=null),"Director pending approval uses the same requester/record/time card");
        reading.decide(special,version(special),true,"Approved",SECRET,"director1");
        check(notifications.deleteHandled(directorWork,"director1"),"Director may delete its notice after deciding");
        UUID scheduleWork=noticeFor(special,"archivist1","APPROVE");
        expect(BusinessException.class,()->notifications.deleteHandled(scheduleWork,"archivist1"));
        check(notifications.inbox("archivist1",0,false,"pending").items().stream().anyMatch(n->n.id().equals(scheduleWork)),"Director approval notifies Archivist to confirm pickup instead of losing the next task");
        reading.confirm(special,version(special),TODAY,"Room special","archivist1");
        check(notifications.deleteHandled(scheduleWork,"archivist1"),"schedule task becomes deletable only after confirmation");
        var form=datedForm(UUID.randomUUID(),0L,"Electronic lifecycle",1990,"MALE","NOTICE_ELIFE",department,type,false,TODAY.minusDays(3),TODAY.minusDays(1),"Synthetic source",null);
        UUID e=electronic.create(form,"fr_lifecycle_reader");
        var eWork=notifications.inbox("archivist1",0,false,"pending").items().stream().filter(n->n.requestId().equals(e)).findFirst().orElseThrow();
        check(eWork.pending() && eWork.workInfo().requesterUsername().equals("fr_lifecycle_reader"),"electronic declaration uses the common pending-notice layout");
        expect(BusinessException.class,()->notifications.deleteHandled(eWork.id(),"archivist1"));
        electronic.review(e,0,false,"Bổ sung thông tin","archivist1");
        var correction=notifications.inbox("fr_lifecycle_reader",0,false,"pending").items().stream().filter(n->n.requestId().equals(e)).findFirst().orElseThrow();
        check(correction.pending() && correction.message().equals("Bổ sung thông tin"),"supplement requests retain useful result text inside the common card");
        expect(BusinessException.class,()->notifications.deleteHandled(correction.id(),"fr_lifecycle_reader"));
        electronic.cancel(e,1,"fr_lifecycle_reader");
        check(notifications.deleteHandled(correction.id(),"fr_lifecycle_reader"),"cancelled declaration releases its old supplement notification for deletion");
        UUID protectedRecord=create("NOTICE_PENDING_PROTECTED",false);
        UUID pending=reading.submit(protectedRecord,"Study",UUID.randomUUID(),"fr_lifecycle_reader");
        reading.review(pending,0,"NEEDS_INFO","Cần bổ sung lý do","archivist1");
        UUID protectedNotice=noticeFor(pending,"fr_lifecycle_reader","REVIEW_NEEDS_INFO");
        var reader=login("fr_lifecycle_reader");var other=login("fr_lifecycle_other");var admin=login("admin1");
        String html=get(reader,"/notifications").body();String token=csrf(html);
        check(html.contains("NOTICE_PENDING_PROTECTED") && !html.contains("NOTICE_LIFECYCLE</") && !html.contains("/notifications/"+protectedNotice+"/delete"),"default inbox shows only pending work and offers no delete action for it");
        check(post(reader,"/notifications/"+confirmed+"/delete",null,"scope","handled").statusCode()==403,"notification deletion requires CSRF");
        check(post(other,"/notifications/"+confirmed+"/delete",csrf(get(other,"/notifications").body()),"scope","handled").statusCode()==404,"another user cannot delete a processed notification by guessing its ID");
        check(post(reader,"/notifications/"+confirmed+"/delete",token,"scope","handled").statusCode()==302 && Boolean.TRUE.equals(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM notifications WHERE id=?",Boolean.class,confirmed)),"HTTP delete marks only an owned processed notification as deleted");
        post(reader,"/notifications/"+protectedNotice+"/delete",token,"scope","pending");
        check(jdbc.queryForObject("SELECT deleted_at IS NULL FROM notifications WHERE id=?",Boolean.class,protectedNotice),"forged delete of pending notification is rejected server-side");
        int removed=notifications.deleteHandled("fr_lifecycle_reader");
        check(removed>0 && jdbc.queryForObject("SELECT deleted_at IS NULL FROM notifications WHERE id=?",Boolean.class,protectedNotice),"bulk deletion removes processed notices while preserving pending notices");
        check(notifications.deleteHandled("fr_lifecycle_reader")==0,"repeated bulk cleanup does not delete pending work or duplicate deletion");
        notifications.read(protectedNotice,"fr_lifecycle_reader");
        check(notifications.inbox("fr_lifecycle_reader",0,false,"pending").items().stream().anyMatch(n->n.id().equals(protectedNotice)),"marking pending work as read does not remove it from the pending list");
        check(get(admin,"/notifications").statusCode()==200 && !get(admin,"/notifications").body().contains("Lifecycle Reader"),"Admin uses common empty notification UI without clinical data");
        check(get(reader,"/notifications?scope=invalid").statusCode()==400,"unknown notification scope is rejected");
    }
    private static UUID noticeFor(UUID request,String username,String action){
        return jdbc.queryForObject("SELECT n.id FROM notifications n JOIN app_users u ON u.id=n.recipient_id JOIN request_events e ON e.id=n.event_id WHERE n.request_id=? AND u.username=? AND e.action=? ORDER BY n.created_at DESC LIMIT 1",UUID.class,request,username,action);
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;System.out.println("PASS: "+message);}
    private static void expect(Class<? extends Throwable> kind,Runnable work){try{work.run();}catch(Throwable e){if(kind.isInstance(e)){check(true,"expected "+kind.getSimpleName());return;}throw e;}throw new AssertionError("Expected "+kind.getSimpleName());}
}
