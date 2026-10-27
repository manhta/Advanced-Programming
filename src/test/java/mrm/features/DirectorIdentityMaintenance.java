package mrm.features;

import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;

/** One-time owner-requested identity update; preserves account ID and all business data. */
public class DirectorIdentityMaintenance {
    private static final String OLD="minh.lv05031980@mrm.com";
    private static final String NEW="manh.td24102003@mrm.com";
    public static void main(String[] args)throws Exception {
        String password=System.getenv("DB_PASSWORD");
        if(password==null || password.isBlank())password=Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$","");
        boolean apply=args.length==2 && args[0].equals("--apply");
        boolean rehearse=args.length==1 && args[0].equals("--rehearse");
        if(apply && Files.size(Path.of(args[1]))<1000)throw new IllegalArgumentException("Full backup required");
        try(var c=DriverManager.getConnection(System.getenv().getOrDefault("DB_URL","jdbc:postgresql://127.0.0.1:5433/mrm"),System.getenv().getOrDefault("DB_USERNAME","postgres"),password)) {
            c.setAutoCommit(false);
            try {
                try(var s=c.createStatement();var rs=s.executeQuery("SELECT u.username,u.display_name,r.code,e.birth_date,u.must_change_password,u.approval_secret_hash IS NULL FROM app_users u JOIN roles r ON r.id=u.role_id LEFT JOIN employees e ON e.account_id=u.id ORDER BY r.code")) {
                    while(rs.next())System.out.println(rs.getString(1)+" | "+rs.getString(2)+" | "+rs.getString(3)+" | birth="+rs.getString(4)+" | initial_setup="+rs.getBoolean(5)+" | no_second_password="+rs.getBoolean(6));
                }
                if(args.length==1 && args[0].equals("--verify")) {
                    String credentials=Files.readString(Path.of(".secrets/operational-accounts.txt"));
                    var encoder=new mrm.config.SecurityConfig().passwordEncoder();
                    for(String name:java.util.List.of("an.nv12041990@mrm.com","linh.tt18091993@mrm.com",NEW)) {
                        String block=credentials.substring(credentials.indexOf("Tên đăng nhập: "+name));
                        String initialPassword=block.split("Mật khẩu: ",2)[1].split("\\R",2)[0];
                        try(var s=c.prepareStatement("SELECT password_hash,must_change_password,approval_secret_hash IS NULL FROM app_users WHERE username=?")) {
                            s.setString(1,name);try(var rs=s.executeQuery()){if(!rs.next() || !encoder.matches(initialPassword,rs.getString(1)) || !rs.getBoolean(2) || !rs.getBoolean(3))throw new IllegalStateException("Initial credentials/flags mismatch");}
                        }
                    }
                    try(var s=c.prepareStatement("SELECT u.display_name,e.full_name,e.birth_date FROM app_users u JOIN employees e ON e.account_id=u.id WHERE u.username=?")) {
                        s.setString(1,NEW);try(var rs=s.executeQuery()){if(!rs.next() || !"Tạ Đức Mạnh".equals(rs.getString(1)) || !"Tạ Đức Mạnh".equals(rs.getString(2)) || !LocalDate.of(2003,10,24).equals(rs.getObject(3,LocalDate.class)))throw new IllegalStateException("Director identity mismatch");}
                    }
                    try(var s=c.prepareStatement("SELECT count(*) FROM app_users WHERE username=?")){s.setString(1,OLD);try(var rs=s.executeQuery()){rs.next();if(rs.getInt(1)!=0)throw new IllegalStateException("Old Director username remains");}}
                    c.rollback();System.out.println("POST-COMMIT PASS: Director Unicode identity/date, three initial password hashes and flags, cleared approval secret, updated credential file; no data mutated.");return;
                }
                if(!apply && !rehearse){c.rollback();return;}
                try(var s=c.createStatement()){s.execute("SET LOCAL lock_timeout='10s'");s.execute("LOCK TABLE app_users,employees,data_reset_history IN SHARE ROW EXCLUSIVE MODE");}
                try(var s=c.createStatement();var rs=s.executeQuery("SELECT EXISTS(SELECT 1 FROM data_reset_history WHERE reset_key='DIRECTOR_IDENTITY_20261026')")) {
                    rs.next();if(rs.getBoolean(1))throw new IllegalStateException("Identity update already applied; refusing to reset user-selected passwords/setup flags again.");
                }
                String adminFingerprint;
                try(var s=c.createStatement();var rs=s.executeQuery("SELECT md5(row_to_json(u)::text) FROM app_users u JOIN roles r ON r.id=u.role_id WHERE r.code='ADMIN'")){rs.next();adminFingerprint=rs.getString(1);}
                String id;
                try(var s=c.prepareStatement("SELECT u.id::text FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.username=? AND r.code='DIRECTOR' FOR UPDATE OF u")) {
                    s.setString(1,OLD);try(var rs=s.executeQuery()){if(!rs.next())throw new IllegalStateException("Expected original Director account");id=rs.getString(1);}
                }
                try(var s=c.prepareStatement("UPDATE app_users SET username=?,email=?,display_name=?,approval_secret_hash=NULL,must_change_password=TRUE,version=version+1 WHERE id=?::uuid")) {
                    s.setString(1,NEW);s.setString(2,NEW);s.setString(3,"Tạ Đức Mạnh");s.setString(4,id);s.executeUpdate();
                }
                try(var s=c.prepareStatement("UPDATE employees SET birth_date=? WHERE account_id=?::uuid")) {
                    s.setObject(1,LocalDate.of(2003,10,24));s.setString(2,id);if(s.executeUpdate()!=1)throw new IllegalStateException("Director employee record missing");
                }
                try(var s=c.createStatement()) {
                    int count=s.executeUpdate("UPDATE app_users SET must_change_password=TRUE,version=version+1 WHERE username IN ('an.nv12041990@mrm.com','linh.tt18091993@mrm.com')");
                    if(count!=2)throw new IllegalStateException("Expected two recently provisioned staff accounts");
                    s.executeUpdate("INSERT INTO data_reset_history(reset_key,details) VALUES('DIRECTOR_IDENTITY_20261026','Owner requested Director identity update and initial password setup for three operational staff accounts; existing Admin preserved.')");
                }
                try(var s=c.prepareStatement("INSERT INTO audit_events(actor,action,entity_type,entity_id,outcome,details) SELECT u.username,'IDENTITY_INITIAL_SETUP','ACCOUNT',?,'SUCCESS','Owner requested Director rename to Ta Duc Manh, initial password setup, and new Director approval password. No existing business data removed.' FROM app_users u JOIN roles r ON r.id=u.role_id WHERE r.code='ADMIN'")) {s.setString(1,id);s.executeUpdate();}
                try(var s=c.createStatement();var rs=s.executeQuery("SELECT md5(row_to_json(u)::text) FROM app_users u JOIN roles r ON r.id=u.role_id WHERE r.code='ADMIN'")){rs.next();if(!rs.getString(1).equals(adminFingerprint))throw new IllegalStateException("Admin changed");}
                try(var s=c.prepareStatement("SELECT u.display_name,e.full_name,e.birth_date,u.must_change_password,u.approval_secret_hash IS NULL FROM app_users u JOIN employees e ON e.account_id=u.id WHERE u.id=?::uuid AND u.username=?")) {
                    s.setString(1,id);s.setString(2,NEW);try(var rs=s.executeQuery()){if(!rs.next() || !"Tạ Đức Mạnh".equals(rs.getString(1)) || !"Tạ Đức Mạnh".equals(rs.getString(2)) || !LocalDate.of(2003,10,24).equals(rs.getObject(3,LocalDate.class)) || !rs.getBoolean(4) || !rs.getBoolean(5))throw new IllegalStateException("Identity verification failed");}
                }
                if(!apply){c.rollback();System.out.println("REHEARSAL PASS; rolled back identity and flags; Admin preserved");return;}
                Path file=Path.of(".secrets/operational-accounts.txt");
                String before=Files.readString(file);
                if(!before.contains(OLD))throw new IllegalStateException("Credential file does not contain original Director account");
                String after=before.replace(OLD,NEW).replace("Lê Văn Minh","Tạ Đức Mạnh").replace("1980-03-05","2003-10-24")
                        .replaceAll("(?m)^Mật khẩu cấp 2: .*", "Mật khẩu cấp 2: Chưa thiết lập; Giám đốc tự tạo ở lần đăng nhập đầu tiên.");
                after="THIẾT LẬP LẦN ĐẦU — 26/10/2026\nBa nhân viên phải thay mật khẩu ban đầu bên dưới sau khi đăng nhập. Không nhập mật khẩu cũ ở bước thiết lập. Admin giữ nguyên.\n\n"+after;
                Files.writeString(Path.of(".secrets/operational-accounts-before-director-update.txt"),before,StandardOpenOption.CREATE_NEW);
                // Write a separate pending file before commit, preserving the existing credential file on rollback.
                Path pending=Path.of(".secrets/operational-accounts-updated.pending.txt");
                Files.writeString(pending,after,StandardOpenOption.CREATE_NEW);
                c.commit();
                Files.move(pending,file,StandardCopyOption.REPLACE_EXISTING);
                System.out.println("COMMITTED: "+NEW+"; Director and employee identity verified; three staff setup flags enabled; Admin unchanged; credentials updated locally.");
            }catch(SQLException ex){c.rollback();throw new IllegalStateException("Maintenance rolled back; SQL state="+ex.getSQLState());}
            catch(Exception ex){c.rollback();throw ex;}
        }
    }
}
