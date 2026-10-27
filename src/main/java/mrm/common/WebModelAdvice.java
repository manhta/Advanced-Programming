package mrm.common;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import mrm.catalog.CatalogException;

@ControllerAdvice
public class WebModelAdvice {
    private final mrm.notification.NotificationService notifications;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public WebModelAdvice(mrm.notification.NotificationService notifications,org.springframework.jdbc.core.JdbcTemplate jdbc){this.notifications=notifications;this.jdbc=jdbc;}
    @ModelAttribute
    public void navigation(Authentication authentication, Model model) {
        String role = authentication == null ? "" : authentication.getAuthorities().stream().map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).findFirst().orElse("");
        model.addAttribute("currentRole", role);
        model.addAttribute("currentRoleLabel",switch(role){case "READER" -> "Bác sĩ";case "ARCHIVIST" -> "Nhân viên lưu trữ";case "DIRECTOR" -> "Giám đốc";case "ADMIN" -> "Quản trị viên";default -> "Khách";});
        model.addAttribute("currentUsername", authentication == null ? "" : authentication.getName());
        String displayName="";
        if(!role.isEmpty()) {
            var names=jdbc.queryForList("""
                SELECT COALESCE(NULLIF(btrim(e.full_name),''),NULLIF(btrim(u.display_name),''),u.username)
                FROM app_users u LEFT JOIN employees e ON e.account_id=u.id WHERE u.username=?
                """,String.class,authentication.getName());
            displayName=names.isEmpty()?authentication.getName():names.getFirst();
        }
        model.addAttribute("currentDisplayName",displayName);
        model.addAttribute("unreadNotificationCount",role.isEmpty()?0L:notifications.unread(authentication.getName()));
        model.addAttribute("recordBase", switch (role) {
            case "ARCHIVIST" -> "/archivist/records"; case "READER" -> "/reader/records";
            case "DIRECTOR" -> "/director/records"; default -> "";
        });
    }
    @ExceptionHandler({BusinessException.class, CatalogException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String business(RuntimeException exception, Model model) {
        model.addAttribute("message", exception.getMessage()); return "common/error";
    }
}
