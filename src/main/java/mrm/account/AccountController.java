package mrm.account;

import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import mrm.common.BusinessException;
import mrm.department.DepartmentService;

@Controller
public class AccountController {
    private final AccountService accounts;
    private final DepartmentService departments;
    public AccountController(AccountService accounts,DepartmentService departments){this.accounts=accounts;this.departments=departments;}
    @GetMapping("/admin/accounts") public String list(@RequestParam(defaultValue="") String name,@RequestParam(defaultValue="") String role,@RequestParam(defaultValue="") String citizenId,Authentication auth,Model model){
        model.addAttribute("accounts",accounts.search(name,role,citizenId));model.addAttribute("name",name);model.addAttribute("selectedRole",role);model.addAttribute("citizenId",citizenId);
        model.addAttribute("currentUsername",auth.getName());model.addAttribute("activeAdminCount",accounts.activeCount("ADMIN"));model.addAttribute("activeDirectorCount",accounts.activeCount("DIRECTOR"));
        return "account/list";
    }
    @GetMapping("/admin/accounts/new") public String form(Model model){
        model.addAttribute("departments",departments.findAll());return "account/new";
    }
    @GetMapping("/admin/accounts/created") public String created(Model model,jakarta.servlet.http.HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");
        if(!model.containsAttribute("issued"))return "redirect:/admin/accounts/new";
        return "account/created";
    }
    @GetMapping("/admin/accounts/{id}/edit") public String edit(@PathVariable UUID id,Model model){
        model.addAttribute("account",accounts.detail(id));model.addAttribute("departments",departments.findAll());return "account/edit";
    }
    @PostMapping("/admin/accounts") public String create(@RequestParam String displayName,@RequestParam String role,
            @RequestParam(required=false) UUID department,@RequestParam(required=false) String email,
            @RequestParam(required=false) String citizenId,@RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate birthDate,
            @RequestParam(required=false) String gender,Authentication auth,Model model,RedirectAttributes flash) {
        try{
            flash.addFlashAttribute("issued",accounts.createAutomatically(displayName,role,department,email,citizenId,birthDate,gender,auth.getName()));
            flash.addFlashAttribute("successMessage","Đã tạo tài khoản và hồ sơ nhân viên. Hãy lưu mật khẩu ban đầu để bàn giao cho nhân viên.");
            return "redirect:/admin/accounts/created";
        }
        catch(BusinessException ex){model.addAttribute("errorMessage",ex.getMessage());}
        catch(DataIntegrityViolationException ex){model.addAttribute("errorMessage","Căn cước công dân đã tồn tại hoặc dữ liệu không hợp lệ. Không có tài khoản mới được lưu.");}
        if(!model.containsAttribute("issued")) {
            var values=new java.util.HashMap<String,Object>();values.put("displayName",displayName);values.put("role",role);values.put("department",department);values.put("email",email);values.put("citizenId",citizenId);values.put("birthDate",birthDate);values.put("gender",gender);model.addAttribute("form",values);
        }
        flash.addFlashAttribute("errorMessage",model.getAttribute("errorMessage"));
        flash.addFlashAttribute("form",model.getAttribute("form"));
        return "redirect:/admin/accounts/new";
    }
    @PostMapping("/admin/accounts/{id}/delete") public String delete(@PathVariable UUID id,@RequestParam long version,Authentication auth,RedirectAttributes flash){
        try{accounts.delete(id,version,auth.getName());flash.addFlashAttribute("successMessage","Đã xóa tài khoản khỏi danh sách. Tài khoản bị vô hiệu hóa; lịch sử hồ sơ và nhật ký được giữ nguyên.");}
        catch(BusinessException ex){flash.addFlashAttribute("errorMessage",ex.getMessage());}
        return "redirect:/admin/accounts";
    }
    @PostMapping("/admin/accounts/{id}") public String update(@PathVariable UUID id,@RequestParam long version,@RequestParam String displayName,
            @RequestParam String role,@RequestParam String status,@RequestParam(required=false) UUID department,@RequestParam(required=false) String email,
            @RequestParam(required=false) String password,@RequestParam(required=false) String citizenId,
            @RequestParam(required=false) @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate birthDate,
            @RequestParam(required=false) String gender,Authentication auth,RedirectAttributes flash) {
        try{accounts.updateWithEmployee(id,version,displayName,role,status,department,email,password,citizenId,birthDate,gender,auth.getName());flash.addFlashAttribute("successMessage","Đã cập nhật tài khoản; phiên cũ sẽ được kiểm tra lại.");}
        catch(BusinessException ex){flash.addFlashAttribute("errorMessage",ex.getMessage());}
        catch(DataIntegrityViolationException ex){flash.addFlashAttribute("errorMessage","Dữ liệu tài khoản không hợp lệ.");}
        return "redirect:/admin/accounts/"+id+"/edit";
    }
    @GetMapping("/director/approval-secret") public String secret(){return "account/secret";}
    @PostMapping("/director/approval-secret") public String secret(@RequestParam String currentPassword,@RequestParam String secret,Authentication auth,RedirectAttributes flash) {
        try{accounts.setApprovalSecret(auth.getName(),currentPassword,secret);flash.addFlashAttribute("successMessage","Đã thiết lập mật khẩu cấp 2.");}
        catch(BusinessException ex){flash.addFlashAttribute("errorMessage",ex.getMessage());}return "redirect:/director/approval-secret";
    }
}
