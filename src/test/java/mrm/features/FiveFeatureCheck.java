package mrm.features;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.dao.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import mrm.catalog.*;
import mrm.common.BusinessException;
import mrm.department.*;
import mrm.record.*;
import mrm.storage.*;

/** Explicit integration checks. Creates only UUID-prefixed fixtures and cleans them in finally. */
public class FiveFeatureCheck {
    @Configuration @EnableTransactionManagement static class Transactions { }
    private static final String ROOT="FTEST"+UUID.randomUUID().toString().replace("-","").substring(0,10).toUpperCase();
    private static final LocalDate TODAY=LocalDate.now(ZoneId.of("Asia/Bangkok"));
    private static int checks,fixtures;
    private static JdbcTemplate jdbc; private static RecordService records; private static RecordRepository recordRepository;
    private static StorageService storage; private static CatalogService catalogs; private static TransactionTemplate tx;
    private record Fixture(String prefix,UUID department,UUID type,UUID policy,UUID warehouse,UUID shelf,UUID source,UUID target,UUID other) { }

    public static void main(String[] args) throws Exception {
        String password=System.getenv("DB_PASSWORD");
        if(password==null || password.isBlank()) password=Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$","");
        var ds=new DriverManagerDataSource(System.getenv().getOrDefault("DB_URL","jdbc:postgresql://127.0.0.1:5433/mrm"),System.getenv().getOrDefault("DB_USERNAME","postgres"),password);
        try(var context=new AnnotationConfigApplicationContext()) {
            context.registerBean(DataSource.class,()->ds);
            context.registerBean(JdbcTemplate.class,()->{var template=new JdbcTemplate(ds);template.setQueryTimeout(10);return template;});
            context.registerBean(DataSourceTransactionManager.class,()->new DataSourceTransactionManager(ds));
            context.register(Transactions.class,CatalogRepository.class,CatalogService.class,CatalogSelectionService.class,
                    DepartmentRepository.class,DepartmentService.class,StorageRepository.class,StorageService.class,RecordRepository.class,RecordService.class);
            context.refresh(); jdbc=context.getBean(JdbcTemplate.class); records=context.getBean(RecordService.class);
            recordRepository=context.getBean(RecordRepository.class); storage=context.getBean(StorageService.class); catalogs=context.getBean(CatalogService.class);
            tx=new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
            try {
                databaseChecks(); concurrencyChecks();
                if(args.length>0) httpChecks(args[0]);
            } finally { cleanup(); }
            check(jdbc.queryForObject("SELECT COUNT(*) FROM record_types WHERE code LIKE ?",Integer.class,ROOT+"%") == 0,"all test catalog fixtures removed");
            check(jdbc.queryForObject("SELECT COUNT(*) FROM storage_locations WHERE code LIKE ?",Integer.class,ROOT+"%") == 0,"all test storage fixtures removed");
        }
        System.out.println("PASSED "+checks+" five-feature checks.");
    }
    private static Fixture fixture() {
        String p=ROOT+"_"+(++fixtures);
        UUID dept=jdbc.queryForObject("SELECT id FROM departments WHERE code='INTERNAL_MEDICINE'",UUID.class);
        catalogs.createType(new RecordTypeForm(p+"_TYPE","Type "+p),"archivist1");
        UUID type=jdbc.queryForObject("SELECT id FROM record_types WHERE code=?",UUID.class,p+"_TYPE");
        catalogs.createPolicy(new RetentionPolicyForm(p+"_POL","Policy "+p,1,10,LocalDate.of(2000,1,1),null,"PRIVATE_BASIS_"+ROOT),"archivist1");
        UUID policy=jdbc.queryForObject("SELECT id FROM retention_policies WHERE code=?",UUID.class,p+"_POL"); catalogs.approve(policy,"director1");
        UUID warehouse=storage.create(new LocationForm(p+"_WH","PRIVATE_LOCATION_"+p,"WAREHOUSE",null,null),"archivist1");
        UUID shelf=storage.create(new LocationForm(p+"_SH","Shelf "+p,"SHELF",warehouse,null),"archivist1");
        UUID source=storage.create(new LocationForm(p+"_A","Source "+p,"COMPARTMENT",shelf,5),"archivist1");
        UUID target=storage.create(new LocationForm(p+"_B","Target "+p,"COMPARTMENT",shelf,1),"archivist1");
        UUID other=storage.create(new LocationForm(p+"_C","Other "+p,"COMPARTMENT",shelf,5),"archivist1");
        return new Fixture(p,dept,type,policy,warehouse,shelf,source,target,other);
    }
    private static RecordForm complete(Fixture f,String number) {
        return new RecordForm(UUID.randomUUID(),0L,null,"MED_"+number,"PAT_"+f.prefix(),"Nguyễn Đặng Thị Ánh "+f.prefix(),1980,"FEMALE",f.department(),f.type(),false,
                TODAY.minusDays(10),TODAY.minusDays(1),TODAY,List.of(f.policy()),f.source(),"NORMAL","TEST:"+f.prefix(),"PRIVATE_NOTE_"+ROOT,null);
    }
    private static RecordForm copy(RecordForm f,UUID token,long version,String archive,String medical,String name,
            LocalDate admission,LocalDate discharge,LocalDate stored,UUID location,List<UUID> policies,String reason) {
        return new RecordForm(token,version,archive,medical,f.patientCode(),name,f.birthYear(),f.sex(),f.departmentId(),f.recordTypeId(),f.deceased(),
                admission,discharge,stored,policies,location,f.qualityStatus(),f.sourceNote(),f.note(),reason);
    }
    private static RecordForm edit(RecordDetails d,String name,String reason) {
        var f=d.form(); return copy(f,f.operationKey(),f.version(),f.archiveNumber(),f.medicalRecordNumber(),name,f.admissionDate(),f.dischargeDate(),f.firstStoredOn(),f.locationId(),f.policyIds(),reason);
    }
    private static RecordFilter filter(String q,int page,int size) { return new RecordFilter(q,null,null,null,null,null,null,page,size,"name"); }
    private static void rollback(Runnable action) { tx.executeWithoutResult(status->{try{action.run();}finally{status.setRollbackOnly();}}); }

    private static void databaseChecks() {
        rollback(()->{
            var f=fixture();var a=complete(f,"SOURCE_LIMIT");
            var input=new RecordForm(a.operationKey(),a.version(),a.archiveNumber(),a.medicalRecordNumber(),a.patientCode(),a.patientName(),a.birthYear(),a.sex(),a.departmentId(),a.recordTypeId(),a.deceased(),a.admissionDate(),a.dischargeDate(),a.firstStoredOn(),a.policyIds(),a.locationId(),a.qualityStatus(),"S".repeat(1000),a.note(),a.reason());
            UUID id=records.create(input,"STORE","archivist1");
            check(records.details(id).form().sourceNote().length()==1000 && records.history(id).size()==1,"maximum-length source note stores with audit atomically");
        });
        rollback(()->{
            var f=fixture();var input=complete(f,"A");UUID id=records.create(input,"STORE","archivist1"); var saved=records.details(id);
            check(saved.state().equals("STORED") && saved.physicalStatus().equals("IN_STORAGE"),"UC04 stores a complete record");
            check(saved.form().archiveNumber().matches("\\d{4}-\\d{2}-\\d{6}"),"archive number allocated by stored month");
            check(saved.policies().size()==1 && saved.retentionYears()==10 && saved.retentionDueOn().equals(TODAY.plusYears(10)),"policy snapshot persisted and readable");
            check(records.locationHistory(id).size()==1 && records.history(id).size()==1,"initial receipt and audit saved atomically");
            check(records.create(input,"STORE","archivist1").equals(id),"identical create replay returns original ID");
            check(records.history(id).size()==1,"create replay does not duplicate audit");
            var changed=copy(input,input.operationKey(),0L,null,input.medicalRecordNumber(),"Changed",input.admissionDate(),input.dischargeDate(),input.firstStoredOn(),input.locationId(),input.policyIds(),null);
            expect(BusinessException.class,()->records.create(changed,"STORE","archivist1"));
        });
        rollback(()->{
            var f=fixture();var sparse=new RecordForm(UUID.randomUUID(),0L,null,null,null,"Draft "+f.prefix(),null,"UNKNOWN",null,f.type(),false,null,null,null,List.of(),null,"INCOMPLETE","TEST:"+f.prefix(),null,null);
            UUID id=records.create(sparse,"DRAFT","archivist1");var saved=records.details(id);
            check(saved.retentionDueOn()==null && saved.form().archiveNumber()==null && saved.state().equals("DRAFT"),"incomplete draft has no invented date or retention");
            check(records.search(filter("Draft "+f.prefix(),0,10),false).total()==0,"draft excluded from public search");
            expect(ResponseStatusException.class,()->recordRepository.summary(id,false));
            var filled=complete(f,"PUBLISH"); filled=copy(filled,saved.form().operationKey(),saved.form().version(),null,filled.medicalRecordNumber(),filled.patientName(),filled.admissionDate(),filled.dischargeDate(),filled.firstStoredOn(),filled.locationId(),filled.policyIds(),"Đã đối soát giấy");
            records.update(id,filled,"STORE","archivist1");check(records.details(id).state().equals("STORED") && records.locationHistory(id).size()==1,"draft publishes after reconciliation");
            expect(BusinessException.class,()->records.update(id,edit(records.details(id),"Name","Reason"),"DRAFT","archivist1"));
        });
        rollback(()->{
            var f=fixture();var a=complete(f,"A"); a=copy(a,a.operationKey(),0L,f.prefix()+"-ONE",a.medicalRecordNumber(),a.patientName(),a.admissionDate(),a.dischargeDate(),a.firstStoredOn(),a.locationId(),a.policyIds(),null);
            records.create(a,"STORE","archivist1");var b=complete(f,"B");var duplicate=copy(b,b.operationKey(),0L,f.prefix()+"-ONE",b.medicalRecordNumber(),b.patientName(),b.admissionDate(),b.dischargeDate(),b.firstStoredOn(),b.locationId(),b.policyIds(),null);
            expect(DuplicateKeyException.class,()->records.create(duplicate,"STORE","archivist1"));
        });
        rollback(()->{
            var f=fixture();var a=complete(f,"A");records.create(a,"STORE","archivist1");var b=complete(f,"B");
            var second=copy(b,b.operationKey(),0L,null,b.medicalRecordNumber(),b.patientName(),TODAY.minusDays(40),TODAY.minusDays(30),TODAY,b.locationId(),b.policyIds(),null);
            records.create(second,"STORE","archivist1");check(records.search(filter(a.patientCode(),0,10),false).total()==2,"same patient can have two treatment episodes");
            var duplicate=copy(a,UUID.randomUUID(),0L,null,a.medicalRecordNumber(),a.patientName(),a.admissionDate(),a.dischargeDate(),a.firstStoredOn(),a.locationId(),a.policyIds(),null);
            expect(BusinessException.class,()->records.create(duplicate,"STORE","archivist1"));
        });
        rollback(()->{
            var f=fixture();var a=complete(f,"A");
            var reversed=copy(a,a.operationKey(),0L,null,a.medicalRecordNumber(),a.patientName(),TODAY,TODAY.minusDays(1),TODAY,a.locationId(),a.policyIds(),null);
            expect(BusinessException.class,()->records.create(reversed,"STORE","archivist1"));
            var future=copy(a,a.operationKey(),0L,null,a.medicalRecordNumber(),a.patientName(),a.admissionDate(),a.dischargeDate(),TODAY.plusDays(1),a.locationId(),a.policyIds(),null);
            expect(BusinessException.class,()->records.create(future,"STORE","archivist1"));
            catalogs.deactivatePolicy(f.policy(),"director1");expect(CatalogException.class,()->records.create(a,"STORE","archivist1"));
        });
        rollback(()->{
            var f=fixture();UUID id=records.create(complete(f,"A"),"STORE","archivist1");var before=records.details(id);
            catalogs.deactivatePolicy(f.policy(),"director1");catalogs.updateType(f.type(),"Changed type",false,0,"archivist1");
            records.update(id,edit(before,"Đã đối soát "+f.prefix(),"Sửa thông tin nguồn"),"STORE","archivist1");var after=records.details(id);
            check(after.recordTypeName().equals(before.recordTypeName()) && after.policies().equals(before.policies()),"editing patient data preserves historical inactive catalog snapshot");
            check(records.history(id).size()==2 && after.form().version()==1L,"metadata edit records before/after and increments version");
            expect(BusinessException.class,()->records.update(id,edit(before,"Stale","Reason"),"STORE","archivist1"));
        });
        rollback(()->{
            var f=fixture();UUID id=records.create(complete(f,"MOVE"),"STORE","archivist1");UUID key=UUID.randomUUID();
            records.move(id,f.target(),0,key,"Sắp xếp lại","archivist1");records.move(id,f.target(),0,key,"Sắp xếp lại","archivist1");
            check(records.details(id).form().locationId().equals(f.target()) && records.details(id).form().version()==1L,"move updates current location once");
            check(records.locationHistory(id).size()==2,"move replay does not duplicate history");
            UUID another=records.create(complete(f,"OTHER"),"STORE","archivist1");
            expect(BusinessException.class,()->records.move(another,f.target(),0,UUID.randomUUID(),"Full","archivist1"));
            expect(BusinessException.class,()->storage.update(f.target(),"Target",false,1,0,"archivist1"));
            check(records.details(another).form().locationId().equals(f.source()),"failed move leaves original location");
        });
        rollback(()->{
            var f=fixture();UUID id=records.create(complete(f,"UNAVAILABLE"),"STORE","archivist1");
            records.availability(id,0,"UNAVAILABLE","Chưa tìm thấy giấy","archivist1");
            expect(BusinessException.class,()->records.move(id,f.target(),1,UUID.randomUUID(),"Move","archivist1"));
            records.availability(id,1,"IN_STORAGE","Đã kiểm tra vật lý","archivist1");
            jdbc.update("UPDATE medical_records SET physical_status='READING' WHERE id=?",id);
            expect(BusinessException.class,()->records.move(id,f.target(),2,UUID.randomUUID(),"Move","archivist1"));
            expect(BusinessException.class,()->records.update(id,edit(records.details(id),"Name","Reason"),"STORE","archivist1"));
        });
        rollback(()->{
            var f=fixture();expect(BusinessException.class,()->storage.create(new LocationForm(f.prefix()+"_BAD","Bad","COMPARTMENT",f.warehouse(),null),"archivist1"));
            expect(BusinessException.class,()->storage.update(f.warehouse(),"Warehouse",false,null,0,"archivist1"));
            storage.update(f.other(),"Renamed",false,5,0,"archivist1");
            expect(BusinessException.class,()->storage.update(f.other(),"Stale",true,5,0,"archivist1"));
        });
        rollback(()->{
            var f=fixture();for(int i=0;i<3;i++)records.create(complete(f,"SEARCH"+i),"STORE","archivist1");
            String q="nguyen dang thi anh "+f.prefix().toLowerCase();var p0=records.search(filter(q,0,2),false);var p1=records.search(filter(q,1,2),false);
            check(p0.total()==3 && p0.items().size()==2 && p1.items().size()==1,"accent-insensitive search and pagination");
            check(records.search(new RecordFilter(q,f.department(),f.type(),"IN_STORAGE",null,TODAY.minusDays(1),TODAY,0,10,"name",1980),false).total()==3,"combined department, type, status, dates and birth-year filters");
            check(records.search(new RecordFilter(q,null,null,null,null,null,null,0,10,"name",1981),false).total()==0,"birth-year filter excludes other years");
            var ids=new HashSet<UUID>();p0.items().forEach(r->ids.add(r.id()));p1.items().forEach(r->ids.add(r.id()));check(ids.size()==3,"stable pagination has no duplicates");
            check(records.search(filter("' OR 1=1 --",0,10),false).total()==0,"search input cannot inject SQL");
            check(records.search(filter("%",0,10),false).items().stream().noneMatch(r->r.patientCode().equals("PAT_"+f.prefix())),"wildcard input is treated literally");
            expect(BusinessException.class,()->records.search(new RecordFilter("",null,null,null,null,TODAY,TODAY.minusDays(1),0,10,"newest"),false));
        });
    }

    private static void concurrencyChecks() throws Exception {
        var episode=fixture();var first=complete(episode,"EPISODE");var second=copy(first,UUID.randomUUID(),0L,null,first.medicalRecordNumber(),first.patientName(),first.admissionDate(),first.dischargeDate(),first.firstStoredOn(),first.locationId(),first.policyIds(),null);
        race(()->records.create(first,"STORE","archivist1"),()->records.create(second,"STORE","archivist1"),BusinessException.class,"concurrent suspected duplicate episode: only one intake");
        var f=fixture();var a=complete(f,"RACE1");var b=complete(f,"RACE2");String number=f.prefix()+"-UNIQUE";
        var aa=copy(a,a.operationKey(),0L,number,a.medicalRecordNumber(),a.patientName(),a.admissionDate(),a.dischargeDate(),a.firstStoredOn(),a.locationId(),a.policyIds(),null);
        var bb=copy(b,b.operationKey(),0L,number,b.medicalRecordNumber(),b.patientName(),b.admissionDate(),b.dischargeDate(),b.firstStoredOn(),b.locationId(),b.policyIds(),null);
        race(()->records.create(aa,"STORE","archivist1"),()->records.create(bb,"STORE","archivist1"),DuplicateKeyException.class,"concurrent duplicate archive: exactly one insert");
        UUID id=jdbc.queryForObject("SELECT id FROM medical_records WHERE archive_number=?",UUID.class,number);
        race(()->{records.move(id,f.target(),0,UUID.randomUUID(),"Race A","archivist1");return id;},
                ()->{records.move(id,f.other(),0,UUID.randomUUID(),"Race B","archivist1");return id;},BusinessException.class,"concurrent move: exactly one update");
        check(records.locationHistory(id).size()==2,"concurrent move has one initial and one transfer history");
        var capacity=fixture();UUID one=records.create(complete(capacity,"ONE"),"STORE","archivist1"),two=records.create(complete(capacity,"TWO"),"STORE","archivist1");
        race(()->{records.move(one,capacity.target(),0,UUID.randomUUID(),"Capacity 1","archivist1");return one;},
                ()->{records.move(two,capacity.target(),0,UUID.randomUUID(),"Capacity 2","archivist1");return two;},BusinessException.class,"concurrent capacity: one record in one-capacity compartment");
    }
    private static void race(Callable<UUID> a,Callable<UUID> b,Class<? extends Throwable> conflict,String name) throws Exception {
        var start=new CountDownLatch(1);var ok=new AtomicInteger();var blocked=new AtomicInteger();
        try(var pool=Executors.newFixedThreadPool(2)) {
            var tasks=List.of(a,b).stream().map(task->pool.submit(()->{start.await();try{task.call();ok.incrementAndGet();}catch(Throwable ex){if(conflict.isInstance(ex))blocked.incrementAndGet();else throw ex;}return null;})).toList();
            start.countDown();for(var task:tasks)task.get(30,TimeUnit.SECONDS);
        }
        check(ok.get()==1 && blocked.get()==1,name);
    }

    private static HttpClient client(){return HttpClient.newBuilder().cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(10)).build();}
    private static HttpResponse<String> get(HttpClient c,String url)throws Exception{return c.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    private static HttpResponse<String> post(HttpClient c,String url,String body)throws Exception{return c.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
    private static String enc(Object value){return URLEncoder.encode(Objects.toString(value,""),StandardCharsets.UTF_8);}
    private static String csrf(String html){var m=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);if(!m.find())throw new AssertionError("Missing CSRF");return m.group(1);}
    private static String body(RecordForm f,String mode,String token){return "operationKey="+f.operationKey()+"&version="+f.version()+"&archiveNumber="+enc(f.archiveNumber())+"&medicalRecordNumber="+enc(f.medicalRecordNumber())+"&patientCode="+enc(f.patientCode())
        +"&patientName="+enc(f.patientName())+"&birthYear="+enc(f.birthYear())+"&sex="+enc(f.sex())+"&departmentId="+enc(f.departmentId())+"&recordTypeId="+enc(f.recordTypeId())+"&deceased="+f.deceased()
        +"&admissionDate="+enc(f.admissionDate())+"&dischargeDate="+enc(f.dischargeDate())+"&firstStoredOn="+enc(f.firstStoredOn())+"&locationId="+enc(f.locationId())+"&qualityStatus="+enc(f.qualityStatus())
        +"&sourceNote="+enc(f.sourceNote())+"&note="+enc(f.note())+"&reason="+enc(f.reason())+String.join("",f.policyIds().stream().map(p->"&policyIds="+p).toList())+"&mode="+mode+"&_csrf="+enc(token);}

    private static void httpChecks(String base)throws Exception {
        var f=fixture();String password=UUID.randomUUID().toString();String hash=new mrm.config.SecurityConfig().passwordEncoder().encode(password);
        var sessions=new HashMap<String,HttpClient>();
        for(String role:List.of("READER","ARCHIVIST","DIRECTOR","ADMIN")) {
            String username=ROOT.toLowerCase()+"_"+role.toLowerCase();
            jdbc.update("INSERT INTO app_users(username,password_hash,display_name,role_id,status) SELECT ?,?,'Five feature verification',id,'ACTIVE' FROM roles WHERE code=?",username,hash,role);
            var c=client();var page=get(c,base+"/login");check(page.statusCode()==200,"login renders for "+role);
            var result=post(c,base+"/login","username="+username+"&password="+enc(password)+"&_csrf="+enc(csrf(page.body())));
            check(result.statusCode()==302 && result.headers().firstValue("location").orElse("").endsWith("/home"),"login authenticates "+role);
            var home=get(c,base+"/home");check(home.statusCode()==302 && home.headers().firstValue("location").orElse("").contains("/"+role.toLowerCase()+"/home"),"home redirects by "+role);
            check(get(c,base+"/"+role.toLowerCase()+"/home").statusCode()==200,"role home renders "+role);sessions.put(role,c);
        }
        var anon=client();check(get(anon,base+"/archivist/records").statusCode()==302,"anonymous cannot search");
        var arch=sessions.get("ARCHIVIST");var reader=sessions.get("READER");var director=sessions.get("DIRECTOR");var admin=sessions.get("ADMIN");
        check(get(admin,base+"/reader/records").statusCode()==403,"admin has no clinical record access");
        for(var c:List.of(reader,director))check(get(c,base+"/archivist/records/new").statusCode()==403,"non-archivist cannot open entry form");
        var createPage=get(arch,base+"/archivist/records/new");check(createPage.statusCode()==200,"record entry template renders");String token=csrf(createPage.body());
        check(get(arch,base+"/archivist/locations").statusCode()==200,"storage template renders");
        String warehouseCode=f.prefix()+"_HTTP_WH";
        var added=post(arch,base+"/archivist/locations","code="+warehouseCode+"&name=HTTP+warehouse&type=WAREHOUSE&_csrf="+enc(token));
        check(added.statusCode()==302,"HTTP creates storage warehouse");
        UUID httpWarehouse=jdbc.queryForObject("SELECT id FROM storage_locations WHERE code=?",UUID.class,warehouseCode);
        var changed=post(arch,base+"/archivist/locations/"+httpWarehouse,"name=Renamed+HTTP+warehouse&active=false&version=0&_csrf="+enc(token));
        check(changed.statusCode()==302 && !storage.findAll().stream().filter(l->l.id().equals(httpWarehouse)).findFirst().orElseThrow().active(),"HTTP updates storage name and deactivates empty warehouse");
        check(post(arch,base+"/archivist/locations","code="+warehouseCode+"&name=Duplicate&type=WAREHOUSE&_csrf="+enc(token)).statusCode()==200,"HTTP duplicate storage code redisplays form");
        var input=complete(f,"HTTP");var response=post(arch,base+"/archivist/records",body(input,"STORE",token));
        check(response.statusCode()==302,"HTTP creates complete record (status "+response.statusCode()+")");
        String location=response.headers().firstValue("location").orElseThrow();UUID id=UUID.fromString(location.substring(location.lastIndexOf('/')+1));
        check(post(arch,base+"/archivist/records",body(input,"STORE",token)).headers().firstValue("location").orElse("").endsWith(id.toString()),"HTTP create replay returns same record");
        var operational=get(arch,base+"/archivist/records/"+id);check(operational.statusCode()==200 && operational.body().contains("PRIVATE_NOTE_"+ROOT),"archivist sees full operational detail");
        for(String role:List.of("READER","DIRECTOR")) {
            var basic=get(sessions.get(role),base+"/"+role.toLowerCase()+"/records/"+id);
            check(basic.statusCode()==200,"detail permission route for "+role);
            check(!basic.body().contains("PRIVATE_NOTE_") && !basic.body().contains("PRIVATE_LOCATION_") && !basic.body().contains("PRIVATE_BASIS_") && !basic.body().contains(f.source().toString()),"no internal data in response to "+role);
        }
        var query=get(reader,base+"/reader/records?q="+enc("nguyen dang thi anh "+f.prefix()));check(query.statusCode()==200 && query.body().contains(id.toString()),"HTTP accent-insensitive search");
        check(get(reader,base+"/reader/records?q="+enc(f.prefix())+"&birthYear=1981").body().contains("Tìm thấy <strong>0</strong>"),"HTTP birth-year filter binds and excludes result");
        check(get(reader,base+"/reader/records?from=2026-12-31&to=2026-01-01").statusCode()==400,"invalid search interval handled");
        check(post(arch,base+"/archivist/records",body(complete(f,"NO_CSRF"),"STORE","")).statusCode()==403,"entry POST needs CSRF");
        var editPage=get(arch,base+"/archivist/records/"+id+"/edit");check(editPage.statusCode()==200,"stored edit form renders");String editToken=csrf(editPage.body());
        var edited=edit(records.details(id),"Đã sửa HTTP "+f.prefix(),"Đối soát qua HTTP");
        check(post(arch,base+"/archivist/records/"+id+"/edit",body(edited,"STORE",editToken)).statusCode()==302,"HTTP metadata edit succeeds");
        check(post(arch,base+"/archivist/records/"+id+"/edit",body(edited,"STORE",editToken)).statusCode()==200,"HTTP stale metadata form redisplays error");
        var detail=records.details(id);String moveBody="destination="+f.target()+"&version="+detail.form().version()+"&operationKey="+UUID.randomUUID()+"&reason=HTTP+move&_csrf="+enc(editToken);
        check(post(arch,base+"/archivist/records/"+id+"/move",moveBody).statusCode()==302,"HTTP move succeeds");
        check(post(arch,base+"/archivist/records/"+id+"/move",moveBody).statusCode()==302 && records.locationHistory(id).size()==2,"HTTP move replay does not duplicate history");
        var sparse=new RecordForm(UUID.randomUUID(),0L,null,null,null,"Draft "+f.prefix(),null,"UNKNOWN",null,f.type(),false,null,null,null,List.of(),null,"INCOMPLETE","TEST:"+f.prefix(),null,null);
        var draftResponse=post(arch,base+"/archivist/records",body(sparse,"DRAFT",editToken));check(draftResponse.statusCode()==302,"HTTP stores incomplete draft");
        String draftUrl=draftResponse.headers().firstValue("location").orElseThrow();String draftId=draftUrl.substring(draftUrl.lastIndexOf('/')+1);
        check(get(reader,base+"/reader/records/"+draftId).statusCode()==404,"reader cannot guess draft ID");
        check(get(director,base+"/director/records/"+draftId).statusCode()==404,"director cannot guess draft ID");
        check(post(reader,base+"/archivist/records",body(complete(f,"FORBIDDEN"),"STORE",csrf(get(reader,base+"/reader/home").body()))).statusCode()==403,"reader cannot directly POST record");
        var wrong=client();String wrongToken=csrf(get(wrong,base+"/login").body());
        check(post(wrong,base+"/login","username="+ROOT.toLowerCase()+"_reader&password=wrong&_csrf="+enc(wrongToken)).headers().firstValue("location").orElse("").endsWith("/login?error"),"incorrect password rejected");
        jdbc.update("UPDATE app_users SET status='LOCKED' WHERE username=?",ROOT.toLowerCase()+"_reader");
        var locked=client();String lockedToken=csrf(get(locked,base+"/login").body());
        check(post(locked,base+"/login","username="+ROOT.toLowerCase()+"_reader&password="+enc(password)+"&_csrf="+enc(lockedToken)).headers().firstValue("location").orElse("").endsWith("/login?error"),"locked account rejected on new login");
        String logoutToken=csrf(get(arch,base+"/archivist/home").body());check(post(arch,base+"/logout","_csrf="+enc(logoutToken)).statusCode()==302,"logout succeeds");
        check(get(arch,base+"/archivist/records").statusCode()==302,"protected page requires login after logout");
    }
    private static void cleanup() {
        var ids=jdbc.queryForList("SELECT r.id FROM medical_records r JOIN record_types t ON t.id=r.record_type_id WHERE t.code LIKE ?",UUID.class,ROOT+"%");
        for(UUID id:ids){jdbc.update("DELETE FROM record_events WHERE record_id=?",id);jdbc.update("DELETE FROM record_location_history WHERE record_id=?",id);jdbc.update("DELETE FROM medical_record_policies WHERE record_id=?",id);jdbc.update("DELETE FROM medical_records WHERE id=?",id);}
        jdbc.update("DELETE FROM location_events WHERE location_id IN (SELECT id FROM storage_locations WHERE code LIKE ?)",ROOT+"%");
        for(String type:List.of("COMPARTMENT","SHELF","WAREHOUSE"))jdbc.update("DELETE FROM storage_locations WHERE code LIKE ? AND type=?",ROOT+"%",type);
        jdbc.update("DELETE FROM catalog_events WHERE target_id IN (SELECT id FROM record_types WHERE code LIKE ?) OR target_id IN (SELECT id FROM retention_policies WHERE code LIKE ?)",ROOT+"%",ROOT+"%");
        jdbc.update("DELETE FROM retention_policies WHERE code LIKE ?",ROOT+"%");jdbc.update("DELETE FROM record_types WHERE code LIKE ?",ROOT+"%");
        jdbc.update("DELETE FROM app_users WHERE username LIKE ?",ROOT.toLowerCase()+"_%");
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;System.out.println("PASS: "+message);}
    private static void expect(Class<? extends Throwable> type,Runnable action){try{action.run();}catch(Throwable ex){if(type.isInstance(ex)){check(true,"expected "+type.getSimpleName());return;}throw ex;}throw new AssertionError("Expected "+type.getSimpleName());}
}
