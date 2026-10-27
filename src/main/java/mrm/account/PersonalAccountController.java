package mrm.account;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import mrm.common.BusinessException;

@Controller
@RequestMapping("/account")
public class PersonalAccountController {
    private final AccountService accounts;
    public PersonalAccountController(AccountService accounts){this.accounts=accounts;}

    @GetMapping("/profile/edit")
    public String editProfile(Authentication auth,Model model){model.addAttribute("profile",accounts.ownProfile(auth.getName()));return "account/profile-edit";}

    @PostMapping("/profile/edit")
    public String editProfile(Authentication auth,@RequestParam long version,@RequestParam String displayName,
            @RequestParam String citizenId,@RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate birthDate,
            @RequestParam String gender,HttpServletRequest request,RedirectAttributes flash){
        try {
            long next=accounts.updateOwnProfile(auth.getName(),version,displayName,citizenId,birthDate,gender);
            request.getSession().setAttribute("accountVersion",next);
            flash.addFlashAttribute("successMessage","Đã cập nhật thông tin cá nhân. Tên tài khoản được giữ nguyên.");
            return "redirect:/account/profile";
        }catch(BusinessException ex){flash.addFlashAttribute("errorMessage",ex.getMessage());}
        catch(org.springframework.dao.DataIntegrityViolationException ex){flash.addFlashAttribute("errorMessage","Căn cước công dân đã tồn tại hoặc thông tin chưa hợp lệ.");}
        return "redirect:/account/profile/edit";
    }

    @GetMapping("/initial-setup")
    public String initialSetup(Authentication auth,Model model) {
        var setup=accounts.initialSetup(auth.getName());
        if(!Boolean.TRUE.equals(setup.get("must_change_password")) && !Boolean.TRUE.equals(setup.get("needs_approval_secret")))return "redirect:/home";
        model.addAttribute("setup",setup);return "account/initial-setup";
    }

    @PostMapping("/initial-setup")
    public String initialSetup(Authentication auth,@RequestParam(required=false) String newPassword,
            @RequestParam(required=false) String confirmPassword,@RequestParam(required=false) String approvalSecret,
            @RequestParam(required=false) String confirmApprovalSecret,HttpServletRequest request,RedirectAttributes flash) {
        try {
            long version=accounts.completeInitialSetup(auth.getName(),newPassword,confirmPassword,approvalSecret,confirmApprovalSecret);
            var session=request.getSession(false);
            if(session!=null)synchronized(session){session.setAttribute("accountVersion",version);}
            flash.addFlashAttribute("successMessage","Đã hoàn thành thiết lập mật khẩu.");
            return "redirect:/home";
        }catch(BusinessException ex) {
            flash.addFlashAttribute("errorMessage",ex.getMessage());return "redirect:/account/initial-setup";
        }
    }

    @GetMapping("/profile")
    public String profile(Authentication auth,Model model){
        model.addAttribute("profile",accounts.ownProfile(auth.getName()));return "account/profile";
    }
    @GetMapping("/password")
    public String password(Authentication auth){accounts.ownProfile(auth.getName());return "account/password";}

    @PostMapping("/password")
    public String password(Authentication auth,@RequestParam String currentPassword,@RequestParam String newPassword,
            @RequestParam String confirmPassword,HttpServletRequest request,RedirectAttributes flash){
        try {
            long version=accounts.changeOwnPassword(auth.getName(),currentPassword,newPassword,confirmPassword);
            var session=request.getSession(false);
            if(session!=null)synchronized(session){session.setAttribute("accountVersion",version);}
            flash.addFlashAttribute("successMessage","Đã đổi mật khẩu. Các phiên đăng nhập khác sẽ hết hiệu lực ở lần truy cập tiếp theo.");
        }catch(BusinessException ex){flash.addFlashAttribute("errorMessage",ex.getMessage());}
        return "redirect:/account/password";
    }
}
