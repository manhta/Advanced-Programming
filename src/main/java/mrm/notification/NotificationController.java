package mrm.notification;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import mrm.common.BusinessException;

@Controller
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service){this.service=service;}
    @GetMapping public String inbox(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="false") boolean unread,@RequestParam(defaultValue="pending") String scope,Authentication auth,Model model){
        model.addAttribute("inbox",service.inbox(auth.getName(),page,unread,scope));model.addAttribute("unreadOnly",unread);model.addAttribute("scope",scope);return "notification/inbox";
    }
    @PostMapping("/{id}/read") public String read(@PathVariable UUID id,Authentication auth){service.read(id,auth.getName());return "redirect:/notifications";}
    @PostMapping("/{id}/open") public String open(@PathVariable UUID id,Authentication auth){
        String path=service.openPath(id,auth.getName());service.read(id,auth.getName());
        return "redirect:"+path;
    }
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id,@RequestParam(defaultValue="pending") String scope,Authentication auth,RedirectAttributes flash){
        NotificationService.scope(scope);
        try{service.deleteHandled(id,auth.getName());flash.addFlashAttribute("successMessage","Đã xóa thông báo đã xử lý.");}
        catch(BusinessException ex){flash.addFlashAttribute("errorMessage",ex.getMessage());}
        return "redirect:/notifications?scope="+scope;
    }
    @PostMapping("/delete-handled")
    public String deleteHandled(@RequestParam(defaultValue="pending") String scope,Authentication auth,RedirectAttributes flash){
        NotificationService.scope(scope);int count=service.deleteHandled(auth.getName());
        flash.addFlashAttribute("successMessage","Đã xóa "+count+" thông báo đã xử lý; các thông báo đang chờ được giữ lại.");
        return "redirect:/notifications?scope="+scope;
    }
    @PostMapping("/read-all") public String readAll(Authentication auth){service.readAll(auth.getName());return "redirect:/notifications";}
}
