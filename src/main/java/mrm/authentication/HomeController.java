package mrm.authentication;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final DashboardService dashboard;
    public HomeController(DashboardService dashboard){this.dashboard=dashboard;}

    @GetMapping("/reader/work") public String readerWork(Authentication authentication,Model model){model.addAttribute("work",dashboard.readerWork(authentication.getName()));return "reading/work";}

    @GetMapping("/archivist/work")
    public String archivistWork(Authentication auth,Model model,@org.springframework.web.bind.annotation.RequestParam(defaultValue="review") String queue){
        model.addAttribute("work",dashboard.archivistWork(auth.getName(),queue));model.addAttribute("queue",queue);return "reading/archivist-work";
    }

    @GetMapping("/director/work")
    public String directorWork(Authentication auth,Model model,@org.springframework.web.bind.annotation.RequestParam(defaultValue="pending") String queue){
        model.addAttribute("work",dashboard.directorWork(auth.getName(),queue));model.addAttribute("queue",queue);return "reading/director-work";
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/home")
    public String home(Authentication authentication) {
        String role = getRole(authentication);

        return switch (role) {
            case "READER" -> "redirect:/reader/home";
            case "ARCHIVIST" -> "redirect:/archivist/home";
            case "DIRECTOR" -> "redirect:/director/home";
            case "ADMIN" -> "redirect:/admin/home";
            default -> throw new AccessDeniedException("Invalid role");
        };
    }

    @GetMapping({
            "/reader/home",
            "/archivist/home",
            "/director/home",
            "/admin/home"
    })
    public String roleHome(
            Authentication authentication,
            Model model,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        String role = getRole(authentication);

        String title = switch (role) {
            case "READER" -> "Trang người đọc";
            case "ARCHIVIST" -> "Trang nhân viên lưu trữ";
            case "DIRECTOR" -> "Trang giám đốc";
            case "ADMIN" -> "Trang quản trị hệ thống";
            default -> throw new AccessDeniedException("Invalid role");
        };

        model.addAttribute("username", authentication.getName());
        model.addAttribute("role", role);
        model.addAttribute("title", title);
        model.addAttribute("dashboard",dashboard.load(authentication.getName()));
        var session=request.getSession(false);
        if(session!=null) {
            synchronized(session) {
                model.addAttribute("showLoginToast",Boolean.TRUE.equals(session.getAttribute("showLoginToast")));
                session.removeAttribute("showLoginToast");
            }
        }

        return "home";
    }

    private String getRole(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElseThrow(
                        () -> new AccessDeniedException("Role not found")
                );
    }
}
