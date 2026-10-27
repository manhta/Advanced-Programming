package mrm.authentication;

import java.util.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import mrm.account.AccountService;
import mrm.workflow.ReadingLabels;

@Service
public class DashboardService {
    public record Action(String title,String description,String url,String icon,String tone){}
    public record Stat(String label,long value,String url){public Stat(String label,long value){this(label,value,null);}}
    public record Recent(String code,String name,String state,String date,String url){}
    public record Dashboard(List<Action> actions,List<Stat> stats,List<Recent> recent,String recentTitle,String searchUrl,String searchParameter){}
    private final JdbcTemplate jdbc;private final AccountService accounts;
    public DashboardService(JdbcTemplate jdbc,AccountService accounts){this.jdbc=jdbc;this.accounts=accounts;}
    private long count(String sql,Object...args){return jdbc.queryForObject(sql,Long.class,args);}
    private Action action(String title,String description,String url,String icon,String tone){return new Action(title,description,url,icon,tone);}
    private static String date(Object value){return value instanceof Timestamp t?t.toInstant().atZone(ZoneId.of("Asia/Bangkok")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")):Objects.toString(value,"");}
    public Dashboard load(String by){
        var actor=accounts.actor(by,"READER","ARCHIVIST","DIRECTOR","ADMIN");String role=(String)actor.get("role");Object id=actor.get("id");String base="/"+role.toLowerCase(Locale.ROOT);
        var actions=new ArrayList<Action>();var stats=new ArrayList<Stat>();var recent=new ArrayList<Recent>();
        if(role.equals("ADMIN")){
            actions.add(action("Tạo tài khoản","Cấp tài khoản và khai báo thông tin nhân viên.","/admin/accounts/new","plus","green"));
            actions.add(action("Quản lý tài khoản","Tìm kiếm, cập nhật thông tin và vai trò nhân viên.","/admin/accounts","users","teal"));
            actions.add(action("Báo cáo / nhật ký","Theo dõi hoạt động quản trị và nhật ký hệ thống.","/admin/reports","chart","purple"));
            stats.add(new Stat("Tài khoản",count("SELECT COUNT(*) FROM app_users")));stats.add(new Stat("Đang hoạt động",count("SELECT COUNT(*) FROM app_users WHERE status='ACTIVE'")));stats.add(new Stat("Khóa / ngừng dùng",count("SELECT COUNT(*) FROM app_users WHERE status<>'ACTIVE'")));
            for(var u:jdbc.queryForList("SELECT id,username,display_name,status,created_at FROM app_users WHERE deleted_at IS NULL ORDER BY created_at DESC,id LIMIT 5"))recent.add(new Recent((String)u.get("username"),(String)u.get("display_name"),switch((String)u.get("status")){case "ACTIVE" -> "Hoạt động";case "LOCKED" -> "Đã khóa";default -> "Ngừng dùng";},date(u.get("created_at")),"/admin/accounts/"+u.get("id")+"/edit"));
            return new Dashboard(actions,stats,recent,"Tài khoản gần đây","/admin/accounts","name");
        }
        actions.add(action("Tra cứu hồ sơ","Tìm bệnh án giấy và điện tử theo mã, người bệnh và đợt điều trị.",base+"/records","search","green"));
        if(role.equals("READER")){
            actions.add(action("Đăng ký mượn hồ sơ","Chọn hồ sơ giấy và gửi yêu cầu tới bộ phận lưu trữ.","/reader/requests/new","record","teal"));
            actions.add(action("Thêm bệnh án mới","Khai báo người bệnh, đợt điều trị và ghi chú.","/reader/records/new","plus","green"));
            stats.add(new Stat("Đang mượn",count("SELECT COUNT(DISTINCT q.id) FROM reading_sessions s JOIN reading_requests q ON q.id=s.request_id WHERE q.requester_id=? AND s.state='READING'",id),"/reader/requests?status=BORROWED&from=1800-01-01"));
            stats.add(new Stat("Chờ nhận",count("SELECT COUNT(DISTINCT q.id) FROM reading_sessions s JOIN reading_requests q ON q.id=s.request_id WHERE q.requester_id=? AND q.state='CONFIRMED' AND s.state='CONFIRMED'",id),"/reader/requests?status=CONFIRMED&from=1800-01-01"));
            stats.add(new Stat("Chờ xử lý",count("SELECT COUNT(*) FROM reading_requests WHERE requester_id=? AND state IN ('SUBMITTED','NEEDS_INFO','PENDING_APPROVAL','APPROVED')",id)+count("SELECT COUNT(*) FROM electronic_records WHERE created_by=? AND state IN ('SUBMITTED','NEEDS_INFO','PENDING_APPROVAL')",id),"/reader/work"));
            stats.add(new Stat("Bệnh án đã tạo",count("SELECT COUNT(*) FROM electronic_records WHERE created_by=?",id),"/reader/records?scope=DECLARATIONS"));
        }else if(role.equals("ARCHIVIST")){
            actions.add(action("Nhập hồ sơ giấy","Khai báo bệnh án giấy, đối soát và xác nhận nhập kho.","/archivist/records/new","plus","teal"));
            actions.add(action("Yêu cầu mượn hồ sơ","Kiểm tra yêu cầu, xếp lịch và xử lý giao–trả.","/archivist/requests","record","green"));
            actions.add(action("Quản lý vị trí","Theo dõi kho, kệ, ngăn và sức chứa.","/archivist/locations","archive","teal"));
            actions.add(action("Danh mục khoa/phòng","Xem các khoa điều trị được sử dụng trong bệnh án.","/archivist/departments","users","green"));
            actions.add(action("Loại bệnh án","Quản lý các loại hồ sơ trong hệ thống.","/archivist/record-types","record","purple"));
            actions.add(action("Chính sách lưu","Quản lý chính sách bảo quản cho hồ sơ giấy.","/archivist/retention-policies","archive","teal"));
            actions.add(action("Kiểm kê và báo cáo","Kiểm kê vị trí và đối soát hồ sơ thực tế.","/archivist/inventories","chart","green"));
            stats.add(new Stat("Chờ kiểm tra",count("SELECT COUNT(*) FROM reading_requests WHERE state='SUBMITTED'")+count("SELECT COUNT(*) FROM electronic_records WHERE state='SUBMITTED'"),"/archivist/work?queue=review"));
            stats.add(new Stat("Hồ sơ trong kho",count("SELECT COUNT(*) FROM medical_records WHERE record_state='STORED' AND physical_status='IN_STORAGE'"),"/archivist/records?state=STORED&physicalStatus=IN_STORAGE"));
            stats.add(new Stat("Chờ giao",count("SELECT COUNT(*) FROM reading_sessions WHERE state='CONFIRMED'"),"/archivist/work?queue=handover"));
        }else{
            actions.add(action("Yêu cầu chờ duyệt","Xem và quyết định các yêu cầu đọc hồ sơ.","/director/requests","record","teal"));
            actions.add(action("Chính sách bảo quản","Kiểm tra và xác nhận chính sách lưu hồ sơ giấy.","/director/retention-policies","archive","green"));
            stats.add(new Stat("Chờ duyệt",count("SELECT COUNT(*) FROM reading_requests WHERE state='PENDING_APPROVAL'")+count("SELECT COUNT(*) FROM electronic_records WHERE state='PENDING_APPROVAL'"),"/director/work?queue=pending"));
            stats.add(new Stat("Đã duyệt",count("SELECT COUNT(*) FROM reading_requests WHERE approver_id=? AND state IN ('APPROVED','CONFIRMED')",id)+count("SELECT COUNT(*) FROM electronic_records WHERE approver_id=? AND state='APPROVED'",id),"/director/work?queue=approved"));
            stats.add(new Stat("Điện tử đã công bố",count("SELECT COUNT(*) FROM electronic_records WHERE state='APPROVED'"),"/director/work?queue=published"));
        }
        actions.add(action("Bệnh án đã khai báo","Theo dõi khai báo và quá trình xét duyệt trong bảng riêng.",base+"/records?scope=DECLARATIONS","record","purple"));
        String where=role.equals("READER")?" WHERE created_by=?":"";
        var rows=role.equals("READER")?jdbc.queryForList("SELECT id,record_code,patient_name,state,created_at FROM electronic_records"+where+" ORDER BY created_at DESC,id LIMIT 5",id):jdbc.queryForList("SELECT id,record_code,patient_name,state,created_at FROM electronic_records ORDER BY created_at DESC,id LIMIT 5");
        var labels=new ReadingLabels();for(var r:rows)recent.add(new Recent((String)r.get("record_code"),(String)r.get("patient_name"),labels.electronicState((String)r.get("state")),date(r.get("created_at")),base+"/records/"+r.get("id")));
        return new Dashboard(actions,stats,recent,"Bệnh án mới khai báo gần đây",base+"/records?scope=DECLARATIONS","q");
    }
    public record ReaderWork(List<Recent> requests,List<Recent> declarations){}
    public ReaderWork readerWork(String by){
        var actor=accounts.actor(by,"READER");Object id=actor.get("id");var labels=new ReadingLabels();
        var requests=new ArrayList<Recent>();var declarations=new ArrayList<Recent>();
        for(var r:jdbc.queryForList("SELECT q.id,r.record_code,q.state,q.created_at FROM reading_requests q JOIN medical_records r ON r.id=q.record_id WHERE q.requester_id=? AND q.state IN ('SUBMITTED','NEEDS_INFO','PENDING_APPROVAL','APPROVED') ORDER BY q.created_at,q.id",id))requests.add(new Recent((String)r.get("record_code"),"",labels.requestState((String)r.get("state")),date(r.get("created_at")),"/reader/requests/"+r.get("id")));
        for(var r:jdbc.queryForList("SELECT id,record_code,state,created_at FROM electronic_records WHERE created_by=? AND state IN ('SUBMITTED','NEEDS_INFO','PENDING_APPROVAL') ORDER BY created_at,id",id))declarations.add(new Recent((String)r.get("record_code"),"",labels.electronicState((String)r.get("state")),date(r.get("created_at")),"/reader/records/"+r.get("id")));
        return new ReaderWork(List.copyOf(requests),List.copyOf(declarations));
    }
    public ReaderWork archivistWork(String by,String queue){
        accounts.actor(by,"ARCHIVIST");
        if(!List.of("review","handover").contains(queue))throw new mrm.common.BusinessException("Danh mục công việc không hợp lệ.");
        var requests=new ArrayList<Recent>();var declarations=new ArrayList<Recent>();var labels=new ReadingLabels();
        if(queue.equals("review")){
            for(var q:jdbc.queryForList("SELECT q.id,r.record_code,q.state,q.created_at FROM reading_requests q JOIN medical_records r ON r.id=q.record_id WHERE q.state='SUBMITTED' ORDER BY q.created_at,q.id"))
                requests.add(new Recent((String)q.get("record_code"),"",labels.requestState((String)q.get("state")),date(q.get("created_at")),"/archivist/requests/"+q.get("id")));
            for(var r:jdbc.queryForList("SELECT id,record_code,state,created_at FROM electronic_records WHERE state='SUBMITTED' ORDER BY created_at,id"))
                declarations.add(new Recent((String)r.get("record_code"),"",labels.electronicState((String)r.get("state")),date(r.get("created_at")),"/archivist/records/"+r.get("id")));
        }else{
            for(var q:jdbc.queryForList("SELECT q.id,r.record_code,to_char(s.reading_on,'DD/MM/YYYY') AS pickup FROM reading_sessions s JOIN reading_requests q ON q.id=s.request_id JOIN medical_records r ON r.id=s.record_id WHERE s.state='CONFIRMED' ORDER BY s.reading_on,s.id"))
                requests.add(new Recent((String)q.get("record_code"),"","Chờ giao",(String)q.get("pickup"),"/archivist/requests/"+q.get("id")+"/circulation"));
        }
        return new ReaderWork(List.copyOf(requests),List.copyOf(declarations));
    }

    public ReaderWork directorWork(String by,String queue) {
        var actor=accounts.actor(by,"DIRECTOR");
        if(!List.of("pending","approved","published").contains(queue))throw new mrm.common.BusinessException("Danh mục phê duyệt không hợp lệ.");
        var requests=new ArrayList<Recent>();var declarations=new ArrayList<Recent>();var labels=new ReadingLabels();
        if(!queue.equals("published")) {
            String condition=queue.equals("pending")?"q.state='PENDING_APPROVAL'":"q.approver_id=? AND q.state IN ('APPROVED','CONFIRMED')";
            Object[] params=queue.equals("pending")?new Object[]{}:new Object[]{actor.get("id")};
            for(var q:jdbc.queryForList("SELECT q.id,r.record_code,q.state,COALESCE(q.decided_at,q.created_at) AS processed FROM reading_requests q JOIN medical_records r ON r.id=q.record_id WHERE "+condition+" ORDER BY q.created_at,q.id",params))
                requests.add(new Recent((String)q.get("record_code"),"",labels.requestState((String)q.get("state")),date(q.get("processed")),"/director/requests/"+q.get("id")));
        }
        String condition=switch(queue){case "pending" -> "state='PENDING_APPROVAL'";case "approved" -> "approver_id=? AND state='APPROVED'";default -> "state='APPROVED'";};
        Object[] params=queue.equals("approved")?new Object[]{actor.get("id")}:new Object[]{};
        for(var r:jdbc.queryForList("SELECT id,record_code,state,COALESCE(decided_at,created_at) AS processed FROM electronic_records WHERE "+condition+" ORDER BY created_at,id",params))
            declarations.add(new Recent((String)r.get("record_code"),"",labels.electronicState((String)r.get("state")),date(r.get("processed")),"/director/records/"+r.get("id")));
        return new ReaderWork(List.copyOf(requests),List.copyOf(declarations));
    }

}
