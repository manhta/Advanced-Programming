package mrm.account;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mrm.audit.AuditService;
import mrm.common.BusinessException;

@Service
public class AccountService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    public AccountService(JdbcTemplate jdbc,PasswordEncoder encoder,AuditService audit) { this.jdbc=jdbc;this.encoder=encoder;this.audit=audit; }
    public List<Map<String,Object>> list() {
        return jdbc.queryForList("SELECT u.id,u.username,u.display_name,u.status,u.version,u.email,u.department_id,r.code AS role,e.citizen_id,e.birth_date,e.gender FROM app_users u JOIN roles r ON r.id=u.role_id LEFT JOIN employees e ON e.account_id=u.id WHERE u.deleted_at IS NULL ORDER BY u.username");
    }
    public int activeCount(String role) {
        checkRole(role);
        return jdbc.queryForObject("SELECT COUNT(*) FROM app_users u JOIN roles r ON r.id=u.role_id WHERE r.code=? AND u.status='ACTIVE' AND u.deleted_at IS NULL",Integer.class,role);
    }
    public List<Map<String,Object>> search(String name,String role) {
        return search(name,role,"");
    }
    public List<Map<String,Object>> search(String name,String role,String citizenId) {
        name=name==null?"":name.strip();role=role==null?"":role.strip();
        citizenId=citizenId==null?"":citizenId.strip();
        if(!citizenId.isEmpty() && !citizenId.matches("[0-9]{10}"))throw new BusinessException("Căn cước tìm kiếm phải gồm đúng 10 chữ số.");
        if(name.length()>200)throw new BusinessException("Tên tìm kiếm tối đa 200 ký tự.");
        if(!role.isEmpty())checkRole(role);
        String pattern="%"+name.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        return jdbc.queryForList("""
            SELECT u.id,u.username,u.display_name,u.status,u.version,r.code AS role FROM app_users u JOIN roles r ON r.id=u.role_id
            LEFT JOIN employees e ON e.account_id=u.id
            WHERE u.deleted_at IS NULL AND (u.username ILIKE ? ESCAPE '!' OR u.display_name ILIKE ? ESCAPE '!')
            AND (?='' OR r.code=?) AND (?='' OR e.citizen_id=?) ORDER BY lower(u.display_name),u.username
            """,pattern,pattern,role,role,citizenId,citizenId);
    }
    public Map<String,Object> detail(UUID id) {
        var rows=jdbc.queryForList("""
            SELECT u.id,u.username,u.display_name,u.status,u.version,u.email,u.department_id,r.code AS role,
                e.citizen_id,e.birth_date,e.gender FROM app_users u JOIN roles r ON r.id=u.role_id
            LEFT JOIN employees e ON e.account_id=u.id WHERE u.id=? AND u.deleted_at IS NULL
            """,id);
        if(rows.isEmpty())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Không tìm thấy tài khoản.");
        return rows.getFirst();
    }
    public Map<String,Object> actor(String username,String... allowed) {
        var users=jdbc.queryForList("SELECT u.id,u.username,u.status,r.code AS role FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.username=? AND u.deleted_at IS NULL",username);
        if(users.isEmpty() || !users.getFirst().get("status").equals("ACTIVE") || !Arrays.asList(allowed).contains(users.getFirst().get("role")))
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản không có quyền thực hiện.");
        return users.getFirst();
    }
    private static String text(String value,int max,String label) {
        if(value==null || value.isBlank() || value.strip().length()>max) throw new BusinessException(label+" không hợp lệ.");
        return value.strip();
    }
    /** Step-up verification shared by reading approvals and electronic record publication. Call inside a transaction. */
    public void verifyApprovalSecret(String by,String value,UUID target) {
        actor(by,"DIRECTOR");
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",rs->{},"mrm:approval:"+by);
        int failed=jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE actor=? AND action='APPROVAL_VERIFY' AND outcome='FAILURE' AND occurred_at>CURRENT_TIMESTAMP-INTERVAL '15 minutes'",Integer.class,by);
        if(failed>=5)throw new BusinessException("Xác thực cấp 2 tạm khóa sau 5 lần sai; thử lại sau cửa sổ 15 phút.");
        String hash=jdbc.queryForObject("SELECT approval_secret_hash FROM app_users WHERE username=?",String.class,by);
        if(hash==null)throw new BusinessException("Thiết lập mật khẩu cấp 2 trước khi phê duyệt.");
        if(value==null || value.length()>128 || !encoder.matches(value,hash)) {
            audit.failure(by,"APPROVAL_VERIFY","RECORD",target,"Mật khẩu cấp 2 không hợp lệ.");throw new BusinessException("Mật khẩu cấp 2 không đúng.");
        }
    }
    private static void password(String value) { if(value==null || value.length()<12 || value.length()>128)throw new BusinessException("Mật khẩu cần 12–128 ký tự."); }
    private static void validateEmployee(String citizenId,java.time.LocalDate birthDate,String gender) {
        if(citizenId==null || !citizenId.matches("[0-9]{10}"))throw new BusinessException("Số căn cước phải gồm đúng 10 chữ số theo quy ước dự án.");
        if(birthDate==null || birthDate.isBefore(java.time.LocalDate.of(1800,1,1)) || birthDate.isAfter(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok"))))throw new BusinessException("Ngày sinh không hợp lệ hoặc ở tương lai.");
        if(gender==null || !List.of("MALE","FEMALE","OTHER").contains(gender))throw new BusinessException("Cần chọn giới tính hợp lệ.");
    }
    @Transactional public void createWithEmployee(String username,String display,String role,UUID department,String email,String password,
            String citizenId,java.time.LocalDate birthDate,String gender,String by) {
        actor(by,"ADMIN");validateEmployee(citizenId,birthDate,gender);
        create(username,display,role,department,email,password,by);
        UUID id=jdbc.queryForObject("SELECT id FROM app_users WHERE username=?",UUID.class,username.strip().toLowerCase(Locale.ROOT));
        saveEmployee(id,citizenId,birthDate,gender,by);
    }
    @Transactional public void updateWithEmployee(UUID id,long version,String display,String role,String status,UUID department,String email,String password,
            String citizenId,java.time.LocalDate birthDate,String gender,String by) {
        actor(by,"ADMIN");
        boolean supplied=(citizenId!=null && !citizenId.isBlank()) || birthDate!=null || (gender!=null && !gender.isBlank());
        if(supplied)validateEmployee(citizenId,birthDate,gender);
        update(id,version,display,role,status,department,email,password,by);
        if(supplied)saveEmployee(id,citizenId,birthDate,gender,by);
    }
    private void saveEmployee(UUID account,String citizenId,java.time.LocalDate birthDate,String gender,String by) {
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM employees WHERE citizen_id=? AND account_id<>?)",Boolean.class,citizenId,account)))
            throw new BusinessException("Căn cước công dân đã được sử dụng bởi một nhân viên khác.");
        jdbc.update("""
            INSERT INTO employees(citizen_id,account_id,full_name,birth_date,gender,role)
            SELECT ?,u.id,u.display_name,?,?,r.code FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.id=?
            ON CONFLICT(account_id) DO UPDATE SET citizen_id=EXCLUDED.citizen_id,full_name=EXCLUDED.full_name,
                birth_date=EXCLUDED.birth_date,gender=EXCLUDED.gender,role=EXCLUDED.role
            """,citizenId,birthDate,gender,account);
        audit.log(by,"EMPLOYEE_SAVE","ACCOUNT",account,"Lưu hồ sơ nhân viên liên kết; không ghi căn cước/ngày sinh vào nhật ký.");
    }
    @Transactional public void create(String username,String display,String role,UUID department,String email,String password,String by) {
        actor(by,"ADMIN"); username=text(username,64,"Tên đăng nhập").toLowerCase(Locale.ROOT);
        if(!username.matches("[a-z0-9._-]{3,64}|[a-z0-9._-]+@[a-z0-9.-]+\\.[a-z]{2,}"))throw new BusinessException("Tên đăng nhập dùng 3–64 ký tự chữ thường, số, dấu chấm, gạch nối, gạch dưới hoặc định dạng email.");
        checkRole(role); password(password); display=text(display,200,"Họ tên"); checkEmail(email);
        jdbc.update("INSERT INTO app_users(username,password_hash,display_name,role_id,department_id,email) SELECT ?,?,?,id,?,? FROM roles WHERE code=?",
            username,encoder.encode(password),display,department,empty(email),role);
        audit.log(by,"ACCOUNT_CREATE","ACCOUNT",username,"Tạo tài khoản một vai trò: "+role);
    }
    public record IssuedAccount(String username,String initialPassword) {}

    @Transactional public IssuedAccount createAutomatically(String display,String role,UUID department,String email,
            String citizenId,java.time.LocalDate birthDate,String gender,String by) {
        actor(by,"ADMIN");validateEmployee(citizenId,birthDate,gender);display=text(display,200,"Họ tên");
        if(department==null || !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM departments WHERE id=? AND active)",Boolean.class,department)))
            throw new BusinessException("Cần chọn khoa hợp lệ khi tạo tài khoản.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM employees WHERE citizen_id=?)",Boolean.class,citizenId)))
            throw new BusinessException("Căn cước công dân đã tồn tại. Không thể tạo thêm tài khoản cho căn cước này.");
        String normalized=java.text.Normalizer.normalize(display,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","")
                .replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT).strip();
        if(!normalized.matches("[a-z]+(?:[ '-]+[a-z]+)*"))throw new BusinessException("Họ tên cần gồm các chữ cái để tạo tên đăng nhập tự động.");
        String[] parts=normalized.split("[ '-]+");
        String initials="";for(int i=0;i<parts.length-1;i++)initials+=parts[i].charAt(0);
        String stem=parts[parts.length-1]+"."+initials+birthDate.format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"));
        if(stem.length()>49)throw new BusinessException("Họ tên quá dài để tạo tên đăng nhập theo quy ước; vui lòng rút gọn hợp lệ.");
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",rs->{},"mrm:username:"+stem);
        String username=stem+"@mrm.com";int suffix=2;
        while(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app_users WHERE username=?)",Boolean.class,username)))username=stem+"-"+(suffix++)+"@mrm.com";
        byte[] bytes=new byte[18];new java.security.SecureRandom().nextBytes(bytes);
        String password=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)+"!";
        createWithEmployee(username,display,role,department,email,password,citizenId,birthDate,gender,by);
        return new IssuedAccount(username,password);
    }
    @Transactional public void update(UUID id,long version,String display,String role,String status,UUID department,String email,String newPassword,String by) {
        actor(by,"ADMIN"); checkRole(role); checkEmail(email);
        if(!List.of("ACTIVE","LOCKED","DISABLED").contains(status))throw new BusinessException("Trạng thái tài khoản không hợp lệ.");
        var rows=jdbc.queryForList("SELECT u.*,r.code AS role FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.id=? AND u.deleted_at IS NULL FOR UPDATE OF u",id);
        if(rows.isEmpty())throw new BusinessException("Không có tài khoản."); var old=rows.getFirst();
        if(((Number)old.get("version")).longValue()!=version)throw new BusinessException("Tài khoản đã thay đổi. Tải lại trang.");
        if(old.get("username").equals(by) && (!role.equals("ADMIN") || !status.equals("ACTIVE")))throw new BusinessException("Không tự khóa hoặc đổi vai trò quản trị đang dùng.");
        if(old.get("role").equals("ADMIN") && old.get("status").equals("ACTIVE") && (!role.equals("ADMIN") || !status.equals("ACTIVE"))) {
            jdbc.execute("SELECT pg_advisory_xact_lock(hashtextextended('mrm:admin',0))");
            if(jdbc.queryForObject("SELECT COUNT(*) FROM app_users u JOIN roles r ON r.id=u.role_id WHERE r.code='ADMIN' AND u.status='ACTIVE'",Integer.class)<=1)throw new BusinessException("Cần giữ ít nhất một Admin hoạt động.");
        }
        if(old.get("role").equals("DIRECTOR") && old.get("status").equals("ACTIVE") && (!role.equals("DIRECTOR") || !status.equals("ACTIVE"))) {
            jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",rs->{},"mrm:active-director");
            if(jdbc.queryForObject("SELECT COUNT(*) FROM app_users u JOIN roles r ON r.id=u.role_id WHERE r.code='DIRECTOR' AND u.status='ACTIVE' AND u.deleted_at IS NULL",Integer.class)<=1)throw new BusinessException("Cần giữ ít nhất một Giám đốc hoạt động để xét duyệt.");
        }
        boolean reset=newPassword!=null && !newPassword.isBlank(); if(reset)password(newPassword);
        jdbc.update("UPDATE app_users SET display_name=?,role_id=(SELECT id FROM roles WHERE code=?),status=?,department_id=?,email=?,password_hash=?,must_change_password=must_change_password OR ?,approval_secret_hash=CASE WHEN ? OR role_id<>(SELECT id FROM roles WHERE code=?) THEN NULL ELSE approval_secret_hash END,version=version+1 WHERE id=?",
            text(display,200,"Họ tên"),role,status,department,empty(email),reset?encoder.encode(newPassword):old.get("password_hash"),reset,reset,role,id);
        audit.log(by,"ACCOUNT_UPDATE","ACCOUNT",id,"Vai trò "+old.get("role")+" → "+role+"; trạng thái "+old.get("status")+" → "+status+"; đổi mật khẩu: "+reset);
    }

    /** Archives an account without breaking clinical and audit-history foreign keys. */
    @Transactional public void delete(UUID id,long version,String by) {
        actor(by,"ADMIN");
        var rows=jdbc.queryForList("SELECT u.*,r.code AS role FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.id=? AND u.deleted_at IS NULL FOR UPDATE OF u",id);
        if(rows.isEmpty())throw new BusinessException("Không tìm thấy tài khoản đang hoạt động để xóa.");
        var target=rows.getFirst();
        if(((Number)target.get("version")).longValue()!=version)throw new BusinessException("Tài khoản đã thay đổi. Tải lại trang.");
        if(target.get("username").equals(by))throw new BusinessException("Không thể xóa tài khoản Admin đang đăng nhập.");

        String role=(String)target.get("role");
        if(role.equals("ADMIN") || role.equals("DIRECTOR")) {
            String lockKey=role.equals("ADMIN")?"mrm:admin":"mrm:active-director";
            jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",rs->{},lockKey);
            Integer active=jdbc.queryForObject("SELECT COUNT(*) FROM app_users u JOIN roles r ON r.id=u.role_id WHERE r.code=? AND u.status='ACTIVE' AND u.deleted_at IS NULL",Integer.class,role);
            if(active!=null && active<=1)throw new BusinessException(role.equals("ADMIN")?"Cần giữ ít nhất một Admin hoạt động.":"Cần giữ ít nhất một Giám đốc hoạt động để xét duyệt.");
        }

        UUID targetId=(UUID)target.get("id");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM staff_change_requests WHERE requester_id=? AND state IN ('SUBMITTED','APPROVED'))",Boolean.class,targetId)))
            throw new BusinessException("Tài khoản còn đề nghị thay đổi vai trò/khoa đang chờ xử lý. Hoàn tất hoặc từ chối đề nghị trước khi xóa.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM reading_requests q WHERE q.requester_id=? AND (q.state IN ('SUBMITTED','NEEDS_INFO','PENDING_APPROVAL','APPROVED') OR (q.state='CONFIRMED' AND EXISTS(SELECT 1 FROM reading_sessions s WHERE s.request_id=q.id AND s.state IN ('REQUESTED','CONFIRMED','READING')))))",Boolean.class,targetId)))
            throw new BusinessException("Tài khoản còn yêu cầu mượn hồ sơ đang xử lý hoặc đang mượn. Hoàn tất yêu cầu trước khi xóa.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM reading_requests WHERE reviewer_id=? AND state IN ('SUBMITTED','NEEDS_INFO','PENDING_APPROVAL'))",Boolean.class,targetId)))
            throw new BusinessException("Tài khoản đang phụ trách yêu cầu cần kiểm tra. Hoàn tất các yêu cầu trước khi xóa.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM medical_records WHERE created_by=? AND submission_state IN ('SUBMITTED','NEEDS_INFO','PENDING_APPROVAL'))",Boolean.class,targetId)))
            throw new BusinessException("Tài khoản còn hồ sơ điện tử đang chờ xử lý. Hoàn tất hoặc từ chối khai báo trước khi xóa.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM inventory_batches WHERE created_by=? AND state='OPEN')",Boolean.class,targetId)))
            throw new BusinessException("Tài khoản còn đợt kiểm kê đang mở. Đóng đợt kiểm kê trước khi xóa.");

        int changed=jdbc.update("UPDATE app_users SET status='DISABLED',deleted_at=CURRENT_TIMESTAMP,approval_secret_hash=NULL,must_change_password=FALSE,version=version+1 WHERE id=? AND deleted_at IS NULL",targetId);
        if(changed!=1)throw new BusinessException("Tài khoản đã thay đổi. Tải lại trang.");
        audit.log(by,"ACCOUNT_DELETE","ACCOUNT",targetId,"Tài khoản được lưu trữ mềm và vô hiệu hóa; giữ nguyên liên kết lịch sử.");
    }

    @Transactional public void setApprovalSecret(String username,String currentPassword,String secret) {
        actor(username,"DIRECTOR"); password(secret);
        String current=jdbc.queryForObject("SELECT password_hash FROM app_users WHERE username=?",String.class,username);
        if(currentPassword==null || !encoder.matches(currentPassword,current))throw new BusinessException("Mật khẩu đăng nhập hiện tại không đúng.");
        if(encoder.matches(secret,current))throw new BusinessException("Mật khẩu cấp 2 cần khác mật khẩu đăng nhập.");
        jdbc.update("UPDATE app_users SET approval_secret_hash=? WHERE username=?",encoder.encode(secret),username);
        audit.log(username,"APPROVAL_SECRET_SET","ACCOUNT",username,"Thiết lập mật khẩu cấp 2; không lưu mật khẩu vào nhật ký.");
    }
    public Map<String,Object> ownProfile(String by) {
        var user=actor(by,"READER","ARCHIVIST","DIRECTOR");
        return jdbc.queryForMap("""
            SELECT u.username,u.version,u.department_id,COALESCE(NULLIF(e.full_name,''),u.display_name) AS full_name,u.email,u.status,
                r.code AS role,e.citizen_id,e.birth_date,e.gender,dep.name AS department_name
            FROM app_users u JOIN roles r ON r.id=u.role_id LEFT JOIN employees e ON e.account_id=u.id
            LEFT JOIN departments dep ON dep.id=u.department_id WHERE u.id=?
            """,user.get("id"));
    }
    @Transactional public long updateOwnProfile(String by,long version,String display,String citizenId,java.time.LocalDate birthDate,String gender) {
        var actor=actor(by,"READER","ARCHIVIST","DIRECTOR");
        validateEmployee(citizenId,birthDate,gender);display=text(display,200,"Họ tên");
        var row=jdbc.queryForMap("SELECT version FROM app_users WHERE id=? FOR UPDATE",actor.get("id"));
        if(((Number)row.get("version")).longValue()!=version)throw new BusinessException("Thông tin đã thay đổi. Tải lại trang trước khi lưu.");
        jdbc.update("UPDATE app_users SET display_name=?,version=version+1 WHERE id=?",display,actor.get("id"));
        saveEmployee((UUID)actor.get("id"),citizenId,birthDate,gender,by);
        audit.log(by,"SELF_PROFILE_UPDATE","ACCOUNT",actor.get("id"),"Nhân viên tự cập nhật thông tin cá nhân; không thay đổi tên đăng nhập, vai trò hoặc khoa.");
        return version+1;
    }
    public Map<String,Object> initialSetup(String by) {
        actor(by,"READER","ARCHIVIST","DIRECTOR","ADMIN");
        return jdbc.queryForMap("SELECT u.display_name,u.must_change_password,r.code AS role,(r.code='DIRECTOR' AND (u.must_change_password OR u.approval_secret_hash IS NULL)) AS needs_approval_secret FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.username=?",by);
    }

    /** An authenticated initial session replaces its temporary password, never an arbitrary account's password. */
    @Transactional public long completeInitialSetup(String by,String newPassword,String confirmation,String approvalSecret,String approvalConfirmation) {
        actor(by,"READER","ARCHIVIST","DIRECTOR","ADMIN");
        var user=jdbc.queryForMap("SELECT u.*,r.code AS role FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.username=? FOR UPDATE OF u",by);
        boolean change=Boolean.TRUE.equals(user.get("must_change_password"));
        boolean director="DIRECTOR".equals(user.get("role"));
        boolean secretRequired=director && (change || user.get("approval_secret_hash")==null);
        if(!change && !secretRequired)throw new BusinessException("Tài khoản đã hoàn thành thiết lập ban đầu. Đổi mật khẩu tại menu tài khoản.");
        String hash=(String)user.get("password_hash");
        if(change) {
            password(newPassword);
            if(!Objects.equals(newPassword,confirmation))throw new BusinessException("Xác nhận mật khẩu mới không khớp.");
            if(encoder.matches(newPassword,hash))throw new BusinessException("Mật khẩu mới phải khác mật khẩu ban đầu.");
            String oldSecret=(String)user.get("approval_secret_hash");
            if(oldSecret!=null && encoder.matches(newPassword,oldSecret))throw new BusinessException("Mật khẩu đăng nhập phải khác mật khẩu cấp 2.");
            hash=encoder.encode(newPassword);
        }
        String second=(String)user.get("approval_secret_hash");
        if(secretRequired) {
            password(approvalSecret);
            if(!Objects.equals(approvalSecret,approvalConfirmation))throw new BusinessException("Xác nhận mật khẩu cấp 2 không khớp.");
            if(encoder.matches(approvalSecret,hash) || encoder.matches(approvalSecret,(String)user.get("password_hash")))throw new BusinessException("Mật khẩu cấp 2 phải khác mật khẩu đăng nhập và mật khẩu ban đầu.");
            second=encoder.encode(approvalSecret);
        }
        long version=((Number)user.get("version")).longValue()+1;
        jdbc.update("UPDATE app_users SET password_hash=?,approval_secret_hash=?,must_change_password=FALSE,version=? WHERE id=?",hash,second,version,user.get("id"));
        audit.log(by,"INITIAL_CREDENTIAL_SETUP","ACCOUNT",user.get("id"),"Hoàn thành thiết lập ban đầu; đổi mật khẩu: "+change+"; thiết lập cấp 2: "+secretRequired+". Không ghi mật khẩu.");
        return version;
    }
    @Transactional public long changeOwnPassword(String by,String currentPassword,String newPassword,String confirmation) {
        actor(by,"READER","ARCHIVIST","DIRECTOR");
        var user=jdbc.queryForMap("SELECT u.*,r.code AS role FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.username=? FOR UPDATE OF u",by);
        if(!"ACTIVE".equals(user.get("status")) || !List.of("READER","ARCHIVIST","DIRECTOR").contains(user.get("role")))
            throw new org.springframework.security.access.AccessDeniedException("Tài khoản không có quyền thực hiện.");
        int failures=jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE actor=? AND action='PASSWORD_CHANGE_VERIFY' AND outcome='FAILURE' AND occurred_at>CURRENT_TIMESTAMP-INTERVAL '15 minutes'",Integer.class,by);
        if(failures>=5)throw new BusinessException("Đã nhập sai mật khẩu hiện tại 5 lần; thử lại sau 15 phút.");
        if(currentPassword==null || currentPassword.length()>128 || !encoder.matches(currentPassword,(String)user.get("password_hash"))) {
            audit.failure(by,"PASSWORD_CHANGE_VERIFY","ACCOUNT",user.get("id"),"Mật khẩu hiện tại không đúng.");
            throw new BusinessException("Mật khẩu hiện tại không đúng.");
        }
        password(newPassword);
        if(!Objects.equals(newPassword,confirmation))throw new BusinessException("Xác nhận mật khẩu mới không khớp.");
        if(encoder.matches(newPassword,(String)user.get("password_hash")))throw new BusinessException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        String second=(String)user.get("approval_secret_hash");
        if(second!=null && encoder.matches(newPassword,second))throw new BusinessException("Mật khẩu đăng nhập phải khác mật khẩu cấp 2.");
        long version=((Number)user.get("version")).longValue()+1;
        jdbc.update("UPDATE app_users SET password_hash=?,version=? WHERE id=?",encoder.encode(newPassword),version,user.get("id"));
        audit.log(by,"PASSWORD_CHANGE","ACCOUNT",user.get("id"),"Người dùng đổi mật khẩu đăng nhập; thu hồi các phiên khác ở lần truy cập tiếp theo.");
        return version;
    }
    private static void checkRole(String role) { if(!List.of("READER","ARCHIVIST","DIRECTOR","ADMIN").contains(role))throw new BusinessException("Vai trò không hợp lệ."); }
    private static String empty(String value){return value==null || value.isBlank()?null:value.strip();}
    private static void checkEmail(String email){if(empty(email)!=null && (email.length()>254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))throw new BusinessException("Email không hợp lệ.");}
}
