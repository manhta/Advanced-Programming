package mrm.features;

import java.nio.file.*;
import java.security.SecureRandom;
import java.sql.*;
import java.time.*;
import java.util.*;
import mrm.config.SecurityConfig;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Explicit one-time reset requested by the owner. Never runs during application startup. */
public class OperationalBaselineMaintenance {
    private static final List<String> HISTORY = List.of("notifications", "reading_sessions", "request_events",
            "reading_requests", "reading_request_batches", "inventory_items", "inventory_batches",
            "electronic_record_events", "record_events", "record_location_history", "location_events",
            "catalog_events", "audit_events");
    private record Person(String role, String username, String name, String citizenId, LocalDate birthDate, String gender) {}
    private static final List<Person> PEOPLE = List.of(
            new Person("READER", "an.nv12041990@mrm.com", "Nguyễn Văn An", "0000001001", LocalDate.of(1990,4,12), "MALE"),
            new Person("ARCHIVIST", "linh.tt18091993@mrm.com", "Trần Thị Linh", "0000001002", LocalDate.of(1993,9,18), "FEMALE"),
            new Person("DIRECTOR", "minh.lv05031980@mrm.com", "Lê Văn Minh", "0000001003", LocalDate.of(1980,3,5), "MALE"));

    public static void main(String[] args) throws Exception {
        if(args.length==1 && args[0].equals("--checksum-inspect")) {
            var f=mrm.database.migration.V21__AddNotificationDismissal.class.getDeclaredField("SQL");f.setAccessible(true);
            String sql=(String)f.get(null);var crc=new java.util.zip.CRC32();
            crc.update(sql.substring(0,sql.indexOf("INSERT INTO notifications")).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("V21 schema-only checksum: "+(int)crc.getValue());return;
        }
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) password = Files.readString(Path.of(".secrets/db-password.txt")).replaceFirst("[\\r\\n]+$", "");
        if (args.length==2 && args[0].equals("--reconcile-v21")) {
            if(Files.size(Path.of(args[1]))<1000) throw new IllegalArgumentException("Backup required");
            try(var c=DriverManager.getConnection(System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://127.0.0.1:5433/mrm"),System.getenv().getOrDefault("DB_USERNAME", "postgres"),password)) {
                c.setAutoCommit(false);
                try {
                    if(!scalar(c,"SELECT checksum FROM flyway_schema_history WHERE version='21' AND success").equals("822223776")) throw new IllegalStateException("Unexpected V21 checksum; no repair");
                    if(!scalar(c,"SELECT count(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='notifications' AND column_name='deleted_at' AND data_type='timestamp with time zone' AND is_nullable='YES'").equals("1"))throw new IllegalStateException("V21 column mismatch");
                    String constraint=scalar(c,"SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid='notifications'::regclass AND conname='notification_deleted_time'");
                    if(!constraint.equals("CHECK (((deleted_at IS NULL) OR (deleted_at >= created_at)))"))throw new IllegalStateException("V21 constraint mismatch: "+constraint);
                    String index=scalar(c,"SELECT indexdef FROM pg_indexes WHERE schemaname=current_schema() AND indexname='ix_notifications_visible'");
                    if(!index.contains("(recipient_id, created_at DESC, id) WHERE (deleted_at IS NULL)"))throw new IllegalStateException("V21 index mismatch: "+index);
                    var f=mrm.database.migration.V21__AddNotificationDismissal.class.getDeclaredField("SQL");f.setAccessible(true);String sql=(String)f.get(null);
                    execute(c,sql.substring(sql.indexOf("INSERT INTO notifications")));
                    // Scoped reconciliation of the verified pre-existing V21 only. No other migration is repaired.
                    try(var s=c.prepareStatement("UPDATE flyway_schema_history SET checksum=? WHERE version='21' AND checksum=822223776 AND success")) {
                        s.setInt(1,new mrm.database.migration.V21__AddNotificationDismissal().getChecksum());if(s.executeUpdate()!=1)throw new IllegalStateException("V21 changed concurrently");
                    }
                    c.commit();System.out.println("Verified V21 column, constraint, index and idempotent backfill; reconciled only V21 checksum.");
                }catch(Exception ex){c.rollback();throw ex;}
            }
            return;
        }
        if (args.length==1 && args[0].equals("--migrate")) {
            org.flywaydb.core.Flyway.configure().dataSource(System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://127.0.0.1:5433/mrm"),
                    System.getenv().getOrDefault("DB_USERNAME", "postgres"),password).locations("classpath:mrm/database/migration").load().migrate();
            return;
        }
        try (var c = DriverManager.getConnection(System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://127.0.0.1:5433/mrm"),
                System.getenv().getOrDefault("DB_USERNAME", "postgres"), password)) {
            c.setAutoCommit(false);
            try {
                print(c, "SELECT u.username,r.code AS role,u.status FROM app_users u JOIN roles r ON r.id=u.role_id ORDER BY r.code,u.username");
                print(c, "SELECT record_code,archive_number,record_state,physical_status,submission_state FROM medical_records ORDER BY record_code");
                print(c, "SELECT conrelid::regclass::text AS child,pg_get_constraintdef(oid) FROM pg_constraint WHERE confrelid='app_users'::regclass ORDER BY 1,2");
                if(args.length==1 && args[0].equals("--verify")) {
                    var ds=new org.springframework.jdbc.datasource.DriverManagerDataSource(System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://127.0.0.1:5433/mrm"),System.getenv().getOrDefault("DB_USERNAME", "postgres"),password);
                    var jdbc=new org.springframework.jdbc.core.JdbcTemplate(ds);
                    var users=new mrm.authentication.DatabaseUserDetailsService(jdbc);
                    String credentials=Files.readString(Path.of(".secrets/operational-accounts.txt"));
                    for(Person person:PEOPLE) {
                        var user=users.loadUserByUsername(person.username.toUpperCase(Locale.ROOT));
                        String block=credentials.substring(credentials.indexOf("Tên đăng nhập: "+person.username));
                        String loginPassword=block.split("Mật khẩu: ",2)[1].split("\\R",2)[0];
                        if(!new SecurityConfig().passwordEncoder().matches(loginPassword,user.getPassword()) || !user.isEnabled()
                                || user.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_"+person.role)))throw new IllegalStateException("Account verification failed");
                        try(var s=c.prepareStatement("SELECT full_name,birth_date,citizen_id FROM employees WHERE account_id=(SELECT id FROM app_users WHERE username=?)")) {
                            s.setString(1,person.username);try(var rs=s.executeQuery()){if(!rs.next() || !person.name.equals(rs.getString(1)) || !person.birthDate.equals(rs.getObject(2,LocalDate.class)) || !person.citizenId.equals(rs.getString(3)))throw new IllegalStateException("Employee mismatch");}
                        }
                    }
                    if(!mrm.devtools.DemoUserInitializer.seedingDisabled(jdbc))throw new IllegalStateException("Demo seed marker missing");
                    if(!scalar(c,"SELECT count(*) FROM app_users").equals("4"))throw new IllegalStateException("Account count mismatch");
                    for(String table:HISTORY)if(!scalar(c,"SELECT count(*) FROM "+table).equals(table.equals("audit_events")?"1":"0"))throw new IllegalStateException("Unexpected history in "+table);
                    if(!scalar(c,"SELECT count(*) FROM medical_records WHERE record_code LIKE 'DEMO%' OR archive_number LIKE 'DEMO%'").equals("0"))throw new IllegalStateException("Demo codes remain");
                    System.out.println("POST-COMMIT VERIFIED: account credentials, roles, employee Unicode names/dates/IDs, seed guard, cleared history, standardized codes.");
                    c.rollback();return;
                }
                if (args.length == 0) { c.rollback(); return; }
                boolean apply = args[0].equals("--apply");
                if (!apply && !args[0].equals("--rehearse")) throw new IllegalArgumentException("Use --rehearse or --apply <backup.dump>");
                if (apply && (args.length != 2 || Files.size(Path.of(args[1])) < 1000)) throw new IllegalArgumentException("A full database backup is required.");
                execute(c, "SET LOCAL lock_timeout='10s'");
                execute(c, "LOCK TABLE app_users,employees,medical_records,medical_record_policies,departments,record_types,retention_policies,storage_locations,record_code_counters,archive_counters," + String.join(",",HISTORY) + " IN ACCESS EXCLUSIVE MODE");
                if (scalar(c,"SELECT to_regclass('data_reset_history') IS NOT NULL").equals("t")) {
                    throw new IllegalStateException("Reset marker already exists; refusing to delete new work.");
                }
                int count = Integer.parseInt(scalar(c,"SELECT count(*) FROM medical_records"));
                var admins = query(c,"SELECT u.id::text,u.username FROM app_users u JOIN roles r ON r.id=u.role_id WHERE r.code='ADMIN' AND u.status='ACTIVE'");
                if (admins.size()!=1) throw new IllegalStateException("Expected exactly one active administrator.");
                UUID admin = UUID.fromString(admins.getFirst()[0]);
                String adminName = admins.getFirst()[1];
                String adminFingerprint=scalar(c,"SELECT md5(row_to_json(u)::text) FROM app_users u WHERE id='"+admin+"'");
                PasswordEncoder encoder = new SecurityConfig().passwordEncoder();
                var replacements = new HashMap<String,UUID>();
                replacements.put("ADMIN", admin);
                var secrets = new StringBuilder("TÀI KHOẢN MRM — dữ liệu nhân viên giả lập\nTrạng thái: CHỜ COMMIT\nAdmin được giữ nguyên: "+adminName+" (mật khẩu không đổi)\n\n");
                for (Person p : PEOPLE) {
                    String loginPassword = randomSecret();
                    String approvalPassword = p.role.equals("DIRECTOR") ? randomSecret() : null;
                    UUID id = UUID.randomUUID();
                    try(var s=c.prepareStatement("INSERT INTO app_users(id,username,password_hash,display_name,role_id,email,approval_secret_hash) SELECT ?,?,?,?,id,?,? FROM roles WHERE code=?")) {
                        s.setObject(1,id);s.setString(2,p.username);s.setString(3,encoder.encode(loginPassword));s.setString(4,p.name);
                        s.setString(5,p.username);s.setString(6,approvalPassword==null?null:encoder.encode(approvalPassword));s.setString(7,p.role);s.executeUpdate();
                    }
                    try(var s=c.prepareStatement("INSERT INTO employees(citizen_id,account_id,full_name,birth_date,gender,role) VALUES(?,?,?,?,?,?)")) {
                        s.setString(1,p.citizenId);s.setObject(2,id);s.setString(3,p.name);s.setObject(4,p.birthDate);s.setString(5,p.gender);s.setString(6,p.role);s.executeUpdate();
                    }
                    // Verify the stored hashes using the actual application encoder without printing any secret.
                    try(var s=c.prepareStatement("SELECT password_hash,approval_secret_hash FROM app_users WHERE id=?")) {
                        s.setObject(1,id);try(var rs=s.executeQuery()){rs.next();
                            if(!encoder.matches(loginPassword,rs.getString(1)) || (approvalPassword!=null && !encoder.matches(approvalPassword,rs.getString(2)))) throw new IllegalStateException("Password verification failed");
                        }
                    }
                    replacements.put(p.role,id);
                    secrets.append(p.role+" — "+p.name+"\nNgày sinh: "+p.birthDate+"; CCCD: "+p.citizenId+"\nTên đăng nhập: "+p.username+"\nMật khẩu: "+loginPassword+"\n");
                    if(approvalPassword!=null) secrets.append("Mật khẩu cấp 2: "+approvalPassword+"\n");
                    secrets.append("\n");
                }
                for(String table:HISTORY) execute(c,"DELETE FROM "+table);
                // Retained clinical/catalog records now belong to replacement accounts of the same role.
                // This is an explicit reset of the learning dataset, not a reconstruction of historical authorship.
                var foreignKeys = query(c,"""
                    SELECT DISTINCT cl.relname,a.attname FROM pg_constraint co
                    JOIN pg_class cl ON cl.oid=co.conrelid JOIN pg_namespace n ON n.oid=cl.relnamespace
                    JOIN pg_attribute a ON a.attrelid=co.conrelid AND a.attnum=co.conkey[1]
                    WHERE co.confrelid='app_users'::regclass AND co.contype='f' AND cardinality(co.conkey)=1
                    AND n.nspname=current_schema() ORDER BY 1,2
                    """);
                execute(c,"ALTER TABLE departments DISABLE TRIGGER departments_fixed_catalog");
                for(var fk:foreignKeys) {
                    if(fk[0].equals("employees")) continue;
                    for(var entry:replacements.entrySet()) {
                        try(var s=c.prepareStatement("UPDATE \""+fk[0]+"\" SET \""+fk[1]+"\"=? WHERE \""+fk[1]+"\" IN (SELECT u.id FROM app_users u JOIN roles r ON r.id=u.role_id WHERE r.code=? AND u.id<>?)")) {
                            s.setObject(1,entry.getValue());s.setString(2,entry.getKey());s.setObject(3,entry.getValue());s.executeUpdate();
                        }
                    }
                }
                execute(c,"ALTER TABLE departments ENABLE TRIGGER departments_fixed_catalog");
                execute(c,"UPDATE medical_records SET physical_status='IN_STORAGE',version=version+1 WHERE has_paper_copy AND record_state='STORED' AND physical_status='READING'");
                execute(c,"UPDATE medical_records SET review_note=NULL,decision_note=NULL");
                StringBuilder mapping = new StringBuilder("id,old_record_code,new_record_code,old_archive_number,new_archive_number\n");
                // Only this authorized maintenance transaction temporarily lifts the code immutability trigger.
                // The table is exclusively locked; the trigger is restored before validation and commit.
                execute(c,"ALTER TABLE medical_records DISABLE TRIGGER assign_record_code");
                for(var row:query(c,"SELECT id::text,record_code,COALESCE(archive_number,''),COALESCE(first_stored_on::text,''),EXTRACT(YEAR FROM created_at)::int::text FROM medical_records WHERE record_code LIKE 'DEMO%' OR archive_number LIKE 'DEMO%' ORDER BY record_code,id")) {
                    int year=Integer.parseInt(row[4]);
                    String code=row[1];
                    if(code.startsWith("DEMO")) {
                        do {
                            String seq=scalar(c,"INSERT INTO record_code_counters(year,last_value) VALUES("+year+",1) ON CONFLICT(year) DO UPDATE SET last_value=record_code_counters.last_value+1 RETURNING last_value");
                            code="MRM-REC-"+year+"-"+String.format(Locale.ROOT,"%06d",Long.parseLong(seq));
                        } while(exists(c,"record_code",code));
                    }
                    String archive=row[2];
                    if(archive.startsWith("DEMO")) {
                        String period=LocalDate.parse(row[3]).toString().substring(0,7);
                        do {
                            String seq=scalar(c,"INSERT INTO archive_counters(period,last_value) VALUES('"+period+"',1) ON CONFLICT(period) DO UPDATE SET last_value=archive_counters.last_value+1 RETURNING last_value");
                            archive=period+"-"+String.format(Locale.ROOT,"%06d",Integer.parseInt(seq));
                        } while(exists(c,"archive_number",archive));
                    }
                    try(var s=c.prepareStatement("UPDATE medical_records SET record_code=?,archive_number=NULLIF(?,''),medical_record_number=CASE WHEN medical_record_number LIKE 'DEMO%' THEN ? ELSE medical_record_number END,patient_code=CASE WHEN patient_code LIKE 'DEMO%' THEN ? ELSE patient_code END,version=version+1 WHERE id=?")) {
                        s.setString(1,code);s.setString(2,archive);s.setString(3,code);s.setString(4,"MRM-PAT-"+row[0].replace("-", "").toUpperCase(Locale.ROOT));s.setObject(5,UUID.fromString(row[0]));s.executeUpdate();
                    }
                    mapping.append(String.join(",",row[0],row[1],code,row[2],archive)+"\n");
                }
                execute(c,"ALTER TABLE medical_records ENABLE TRIGGER assign_record_code");
                try(var s=c.prepareStatement("DELETE FROM app_users WHERE id<>? AND id<>? AND id<>? AND id<>?")) {
                    s.setObject(1,admin);s.setObject(2,replacements.get("READER"));s.setObject(3,replacements.get("ARCHIVIST"));s.setObject(4,replacements.get("DIRECTOR"));s.executeUpdate();
                }
                execute(c,"CREATE TABLE data_reset_history(reset_key VARCHAR(64) PRIMARY KEY,performed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,details TEXT NOT NULL)");
                execute(c,"INSERT INTO data_reset_history(reset_key,details) VALUES('OPERATIONAL_BASELINE','Owner-approved reset: records and catalogs retained; demo codes replaced; test transactions removed; admin preserved; three replacement employees created. Demo seeding disabled.')");
                try(var s=c.prepareStatement("INSERT INTO audit_events(actor,action,entity_type,outcome,details) VALUES(?,'OPERATIONAL_RESET','DATABASE','SUCCESS','Owner-approved learning dataset reset. Historical authors replaced by role; full pre-reset backup retained locally.')")) {s.setString(1,adminName);s.executeUpdate();}
                if(Integer.parseInt(scalar(c,"SELECT count(*) FROM medical_records"))!=count) throw new IllegalStateException("Record count changed");
                if(!scalar(c,"SELECT count(*) FROM app_users").equals("4")) throw new IllegalStateException("Expected four accounts");
                if(!scalar(c,"SELECT count(DISTINCT role_id) FROM app_users").equals("4")) throw new IllegalStateException("Expected one account per role");
                if(!scalar(c,"SELECT md5(row_to_json(u)::text) FROM app_users u WHERE id='"+admin+"'").equals(adminFingerprint))throw new IllegalStateException("Retained admin was modified");
                if(!scalar(c,"SELECT count(*) FROM medical_records WHERE record_code LIKE 'DEMO%' OR archive_number LIKE 'DEMO%' OR medical_record_number LIKE 'DEMO%' OR patient_code LIKE 'DEMO%'").equals("0")) throw new IllegalStateException("Demo codes remain");
                for(String table:HISTORY) if(!table.equals("audit_events") && !scalar(c,"SELECT count(*) FROM "+table).equals("0")) throw new IllegalStateException("History remains: "+table);
                if(!scalar(c,"SELECT count(*) FROM pg_trigger WHERE tgrelid='medical_records'::regclass AND tgname='assign_record_code' AND tgenabled='O'").equals("1")) throw new IllegalStateException("Code trigger not enabled");
                // Confirm ordinary updates cannot change the record code after maintenance.
                Savepoint guard=c.setSavepoint();
                try { execute(c,"UPDATE medical_records SET record_code=record_code||'-INVALID' WHERE id=(SELECT id FROM medical_records LIMIT 1)");throw new IllegalStateException("Immutable code guard failed"); }
                catch(SQLException expected) {if(!expected.getMessage().contains("Record code is immutable"))throw expected;c.rollback(guard);}
                System.out.println("CHECKS PASS: records retained="+count+", four roles, password hashes, no demo codes, no old transactions, immutable code restored");
                if(!apply) {c.rollback();System.out.println("REHEARSAL ROLLED BACK: no data or credentials persisted");return;}
                Path credentials=Path.of(".secrets/operational-accounts.txt");
                Path map=Path.of("target/verification/operational-record-code-mapping.csv");
                Files.createDirectories(credentials.getParent());Files.createDirectories(map.getParent());
                Files.writeString(credentials,secrets.toString(),StandardOpenOption.CREATE_NEW);
                Files.writeString(map,mapping.toString(),StandardOpenOption.CREATE_NEW);
                c.commit();
                Files.writeString(credentials,secrets.toString().replace("CHỜ COMMIT","ĐÃ COMMIT"),StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("COMMITTED; credentials saved locally: "+credentials.toAbsolutePath());
                System.out.println("Code mapping: "+map.toAbsolutePath());
            } catch(SQLException ex) {c.rollback();throw new IllegalStateException("Database operation failed; rolled back. SQL state="+ex.getSQLState());}
            catch(Exception ex) {c.rollback();throw ex;}
        }
    }
    private static boolean exists(Connection c,String column,String value)throws SQLException {
        try(var s=c.prepareStatement("SELECT EXISTS(SELECT 1 FROM medical_records WHERE "+column+"=?)")){s.setString(1,value);try(var rs=s.executeQuery()){rs.next();return rs.getBoolean(1);}}
    }
    private static String randomSecret() {byte[] bytes=new byte[18];new SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)+"!";}
    private static void execute(Connection c,String sql)throws SQLException {try(var s=c.createStatement()){int n=s.executeUpdate(sql);System.out.println(sql.split(" ",3)[0]+" "+sql.split(" ",3)[1]+": "+n);}}
    private static String scalar(Connection c,String sql)throws SQLException {try(var s=c.createStatement();var rs=s.executeQuery(sql)){rs.next();return rs.getString(1);}}
    private static List<String[]> query(Connection c,String sql)throws SQLException {var out=new ArrayList<String[]>();try(var s=c.createStatement();var rs=s.executeQuery(sql)){while(rs.next()){String[] row=new String[rs.getMetaData().getColumnCount()];for(int i=0;i<row.length;i++)row[i]=rs.getString(i+1);out.add(row);}}return out;}
    private static void print(Connection c,String sql)throws SQLException {for(var row:query(c,sql))System.out.println(String.join(" | ",row));}
}
