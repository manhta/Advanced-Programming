package mrm.features;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import mrm.MedicalRecordApplication;
import mrm.account.AccountService;

/** Actual login, CSRF and authorization tests in a disposable schema; no public data changed. */
public class InitialCredentialSetupCheck {
    private static final String INITIAL="Synthetic-initial-only-2048!";
    private static final String NEW="Synthetic-user-selected-2048!";
    private static final String SECRET="Synthetic-director-second-2048!";
    private static int checks;
    private static String base;
    private static JdbcTemplate jdbc;
    private static PasswordEncoder encoder;
    private static void check(boolean value,String description){if(!value)throw new AssertionError(description);checks++;System.out.println("PASS: "+description);}

    public static void main(String[] args)throws Exception {
        String dbPassword=System.getenv("DB_PASSWORD");
        if(dbPassword==null || dbPassword.isBlank())dbPassword=Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$","");
        String url=System.getenv().getOrDefault("DB_URL","jdbc:postgresql://127.0.0.1:5433/mrm");
        String username=System.getenv().getOrDefault("DB_USERNAME","postgres");
        String schema="initial_setup_"+UUID.randomUUID().toString().replace("-","");
        try(var connection=DriverManager.getConnection(url,username,dbPassword)) {
            try(var s=connection.createStatement()){s.execute("CREATE SCHEMA "+schema);}
            try(var app=new SpringApplicationBuilder(MedicalRecordApplication.class).run(
                    "--spring.profiles.active=dev,seed-users","--server.port=0",
                    "--spring.datasource.url="+url+(url.contains("?")?"&":"?")+"currentSchema="+schema,
                    "--spring.datasource.username="+username,"--spring.datasource.password="+dbPassword,
                    "--spring.flyway.schemas="+schema,"--spring.flyway.default-schema="+schema,
                    "--SEED_READER_PASSWORD="+INITIAL,"--SEED_ARCHIVIST_PASSWORD="+INITIAL,
                    "--SEED_DIRECTOR_PASSWORD="+INITIAL,"--SEED_ADMIN_PASSWORD="+INITIAL)) {
                jdbc=app.getBean(JdbcTemplate.class);encoder=app.getBean(PasswordEncoder.class);
                base="http://localhost:"+((WebServerApplicationContext)app).getWebServer().getPort();
                check(jdbc.queryForObject("SELECT current_schema()",String.class).equals(schema),"tests confined to disposable schema");
                check(get(browser(),"/account/initial-setup").statusCode()==302,"anonymous user cannot access setup");
                for(String role:List.of("reader","archivist","director","admin"))testRole(role);

                AccountService accounts=app.getBean(AccountService.class);
                accounts.create("new.doctor01012000@mrm.com","New Doctor","READER",null,null,INITIAL,"admin1");
                check(jdbc.queryForObject("SELECT must_change_password FROM app_users WHERE username='new.doctor01012000@mrm.com'",Boolean.class),"Admin-created account requires first password change by default");
                var newAccount=login("new.doctor01012000@mrm.com",INITIAL);
                check(get(newAccount,"/reader/home").headers().firstValue("location").orElse("").endsWith("/account/initial-setup"),"new account cannot bypass setup by opening its home");
                accounts.completeInitialSetup("new.doctor01012000@mrm.com",NEW,NEW,null,null);
                UUID id=jdbc.queryForObject("SELECT id FROM app_users WHERE username='new.doctor01012000@mrm.com'",UUID.class);
                long version=jdbc.queryForObject("SELECT version FROM app_users WHERE id=?",Long.class,id);
                accounts.update(id,version,"New Doctor","READER","ACTIVE",null,null,INITIAL,"admin1");
                check(jdbc.queryForObject("SELECT must_change_password FROM app_users WHERE id=?",Boolean.class,id),"Admin password reset requires user to replace temporary password again");

                jdbc.update("UPDATE app_users SET approval_secret_hash=NULL,version=version+1 WHERE username='director1'");
                var director=login("director1",NEW);
                String secretOnly=get(director,"/account/initial-setup").body();
                check(secretOnly.contains("name=\"approvalSecret\"") && !secretOnly.contains("name=\"newPassword\""),"Director missing second password is forced to a secret-only setup");
                String token=csrf(secretOnly);
                check(post(director,"/account/initial-setup",token,"approvalSecret",SECRET,"confirmApprovalSecret",SECRET).headers().firstValue("location").orElse("").endsWith("/home"),"Director can set mandatory approval password without changing completed login password");
                check(encoder.matches(NEW,jdbc.queryForObject("SELECT password_hash FROM app_users WHERE username='director1'",String.class)),"secret-only setup preserves login password");
                check(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE details LIKE ? OR details LIKE ? OR details LIKE ?",Integer.class,"%"+INITIAL+"%","%"+NEW+"%","%"+SECRET+"%")==0,"audit never contains passwords");
            }finally{try(var s=connection.createStatement()){s.execute("DROP SCHEMA "+schema+" CASCADE");}}
            try(var s=connection.prepareStatement("SELECT to_regnamespace(?) IS NULL")){s.setString(1,schema);try(var rs=s.executeQuery()){rs.next();check(rs.getBoolean(1),"temporary schema removed");}}
        }
        System.out.println("PASSED "+checks+" initial credential setup checks.");
    }

    private static void testRole(String role)throws Exception {
        String name=role+"1";boolean director=role.equals("director");
        var client=login(name,INITIAL);var stale=login(name,INITIAL);
        String path="/"+role+"/home";
        var blocked=get(client,path);
        check(blocked.headers().firstValue("location").orElse("").endsWith("/account/initial-setup"),role+": direct business access blocked before setup; status="+blocked.statusCode()+", location="+blocked.headers().firstValue("location").orElse("none"));
        var page=get(client,"/account/initial-setup");String token=csrf(page.body());
        check(page.statusCode()==200 && !page.body().contains("name=\"currentPassword\""),role+": first-time form requires no old password");
        check(page.body().contains("name=\"approvalSecret\"")==director,role+": second password fields shown only for Director");
        check(post(client,"/account/initial-setup","wrong-token","newPassword",NEW,"confirmPassword",NEW).statusCode()==403,role+": CSRF is enforced");
        check(post(client,"/account/password",token,"currentPassword",INITIAL,"newPassword",NEW,"confirmPassword",NEW).headers().firstValue("location").orElse("").endsWith("/account/initial-setup"),role+": ordinary password endpoint cannot bypass initial setup");
        String oldHash=jdbc.queryForObject("SELECT password_hash FROM app_users WHERE username=?",String.class,name);
        for(var bad:List.of(new String[]{"short","short"},new String[]{NEW,"mismatch"},new String[]{INITIAL,INITIAL})) {
            post(client,"/account/initial-setup",token,"newPassword",bad[0],"confirmPassword",bad[1],"approvalSecret",SECRET,"confirmApprovalSecret",SECRET);
            check(jdbc.queryForObject("SELECT must_change_password FROM app_users WHERE username=?",Boolean.class,name),role+": invalid initial password leaves setup incomplete");
        }
        if(director) {
            for(var bad:List.of(new String[]{"",""},new String[]{NEW,NEW},new String[]{INITIAL,INITIAL},new String[]{SECRET,"mismatch"})) {
                post(client,"/account/initial-setup",token,"newPassword",NEW,"confirmPassword",NEW,"approvalSecret",bad[0],"confirmApprovalSecret",bad[1]);
                check(jdbc.queryForObject("SELECT password_hash FROM app_users WHERE username=?",String.class,name).equals(oldHash),"Director: invalid/missing second password rolls back login password change");
            }
        }
        var result=post(client,"/account/initial-setup",token,"newPassword",NEW,"confirmPassword",NEW,"approvalSecret",SECRET,"confirmApprovalSecret",SECRET);
        check(result.statusCode()==302 && result.headers().firstValue("location").orElse("").endsWith("/home"),role+": both required credentials saved without old password; status="+result.statusCode()+", location="+result.headers().firstValue("location").orElse("none"));
        check(!jdbc.queryForObject("SELECT must_change_password FROM app_users WHERE username=?",Boolean.class,name),role+": first-login flag cleared");
        check(get(client,path).statusCode()==200,role+": current session continues to business page");
        check(get(stale,path).headers().firstValue("location").orElse("").endsWith("/login?expired"),role+": other pre-setup sessions revoked");
        check(get(client,"/account/initial-setup").headers().firstValue("location").orElse("").endsWith("/home"),role+": completed accounts no longer see first-time form");
        var rejected=browser();String loginToken=csrf(get(rejected,"/login").body());
        check(post(rejected,"/login",loginToken,"username",name,"password",INITIAL).headers().firstValue("location").orElse("").endsWith("/login?error"),role+": temporary password stops working");
        var returning=login(name,NEW);
        check(get(returning,path).statusCode()==200,role+": subsequent login uses selected password");
        if(director)check(encoder.matches(SECRET,jdbc.queryForObject("SELECT approval_secret_hash FROM app_users WHERE username=?",String.class,name)),"Director: independent approval password stored as hash");
        else check(jdbc.queryForObject("SELECT approval_secret_hash IS NULL FROM app_users WHERE username=?",Boolean.class,name),role+": non-Director cannot inject approval password");
        String hash=jdbc.queryForObject("SELECT password_hash FROM app_users WHERE username=?",String.class,name);
        post(client,"/account/initial-setup",token,"newPassword",INITIAL,"confirmPassword",INITIAL,"approvalSecret",SECRET,"confirmApprovalSecret",SECRET);
        check(jdbc.queryForObject("SELECT password_hash FROM app_users WHERE username=?",String.class,name).equals(hash),role+": repeat setup cannot bypass normal password verification");
        if(!role.equals("admin")) {
            String normalToken=csrf(get(returning,"/account/password").body());
            post(returning,"/account/password",normalToken,"currentPassword","wrong","newPassword",INITIAL,"confirmPassword",INITIAL);
            check(jdbc.queryForObject("SELECT password_hash FROM app_users WHERE username=?",String.class,name).equals(hash),role+": ordinary password change still checks old password");
        }
    }
    private static HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NEVER).build();}
    private static HttpResponse<String> get(HttpClient c,String path)throws Exception{return c.send(HttpRequest.newBuilder(URI.create(base+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    private static String csrf(String html){var m=Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);if(!m.find())throw new AssertionError("CSRF missing");return m.group(1);}
    private static String enc(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private static HttpResponse<String> post(HttpClient c,String path,String token,String...pairs)throws Exception {
        var body=new StringBuilder("_csrf="+enc(token));for(int i=0;i<pairs.length;i+=2)body.append("&"+enc(pairs[i])+"="+enc(pairs[i+1]));
        return c.send(HttpRequest.newBuilder(URI.create(base+path)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
    }
    private static HttpClient login(String name,String password)throws Exception {
        var c=browser();String token=csrf(get(c,"/login").body());var response=post(c,"/login",token,"username",name,"password",password);
        check(response.statusCode()==302 && !response.headers().firstValue("location").orElse("").contains("error"),"authenticated "+name);return c;
    }
}
