package mrm.account;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import mrm.common.BusinessException;
import mrm.department.DepartmentService;

@Controller
public class StaffChangeController {
    private final StaffChangeService changes;
    private final AccountService accounts;
    private final DepartmentService departments;
    public StaffChangeController(StaffChangeService changes,AccountService accounts,DepartmentService departments){this.changes=changes;this.accounts=accounts;this.departments=departments;}
    @GetMapping("/account/change-requests") public String mine(Authentication auth,Model model){
        model.addAttribute("requests",changes.mine(auth.getName()));model.addAttribute("profile",accounts.ownProfile(auth.getName()));
        model.addAttribute("departments",departments.findAll());model.addAttribute("token",UUID.randomUUID());model.addAttribute("mode","SELF");return "account/change-list";
    }
    @PostMapping("/account/change-requests") public String submit(Authentication auth,@RequestParam String role,@RequestParam(required=false) UUID department,
            @RequestParam String reason,@RequestParam UUID token,RedirectAttributes flash){
        try{UUID id=changes.submit(auth.getName(),role,department,reason,token);flash.addFlashAttribute("successMessage","Đã gửi đề nghị đến Giám đốc; vai trò và khoa hiện tại chưa thay đổi.");return "redirect:/account/change-requests/"+id;}
        catch(BusinessException ex){flash.addFlashAttribute("errorMessage",ex.getMessage());}
        catch(org.springframework.dao.DataIntegrityViolationException ex){flash.addFlashAttribute("errorMessage","Bạn đã có đề nghị đang xử lý. Kiểm tra danh sách trước khi gửi lại.");}
        return "redirect:/account/change-requests";
    }
    @GetMapping({"/director/staff-change-requests","/admin/staff-change-requests","/admin/accounts/changes"}) public String queue(Authentication auth,Model model){
        model.addAttribute("requests",changes.queue(auth.getName()));model.addAttribute("mode",auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"))?"ADMIN":"DIRECTOR");return "account/change-list";
    }
    @GetMapping({"/account/change-requests/{id}","/director/staff-change-requests/{id}","/admin/staff-change-requests/{id}","/admin/accounts/changes/{id}"})
    public String detail(@PathVariable UUID id,Authentication auth,Model model){model.addAttribute("change",changes.detail(id,auth.getName()));model.addAttribute("viewerUsername",auth.getName());return "account/change-detail";}
    @PostMapping("/director/staff-change-requests/{id}/decide")
    public String decide(@PathVariable UUID id,Authentication auth,@RequestParam long version,@RequestParam String decision,
            @RequestParam(required=false) String note,@RequestParam String approvalSecret,RedirectAttributes flash){
        try{
            if(!java.util.List.of("APPROVE","REJECT").contains(decision))throw new BusinessException("Kết quả xét duyệt không hợp lệ.");
            changes.decide(id,version,decision.equals("APPROVE"),note,approvalSecret,auth.getName());flash.addFlashAttribute("successMessage","Đã xét duyệt và gửi thông báo nội bộ.");
        }catch(BusinessException ex){flash.addFlashAttribute("errorMessage",ex.getMessage());}
        return "redirect:/director/staff-change-requests/"+id;
    }
    @PostMapping({"/admin/staff-change-requests/{id}/apply","/admin/accounts/changes/{id}/apply"})
    public String apply(@PathVariable UUID id,Authentication auth,@RequestParam long version,RedirectAttributes flash){
        try{changes.apply(id,version,auth.getName());flash.addFlashAttribute("successMessage","Đã áp dụng vai trò/khoa được duyệt. Nhân viên cần đăng nhập lại.");}
        catch(BusinessException ex){flash.addFlashAttribute("errorMessage",ex.getMessage());}
        return "redirect:/admin/accounts/changes/"+id;
    }
}
