package mrm.config;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revokes old sessions after account lock, role change or administrative password reset. */
public class AccountStateFilter extends OncePerRequestFilter {
    private final JdbcTemplate jdbc;
    public AccountStateFilter(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            var rows=jdbc.queryForList("SELECT u.status,u.deleted_at,u.version,u.must_change_password,(u.approval_secret_hash IS NULL) AS needs_approval_secret,r.code FROM app_users u JOIN roles r ON r.id=u.role_id WHERE username=?",auth.getName());
            var session=request.getSession(false);Object stamp=session==null?null:session.getAttribute("accountVersion");
            boolean valid=!rows.isEmpty() && rows.getFirst().get("status").equals("ACTIVE") && rows.getFirst().get("deleted_at")==null && auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_"+rows.getFirst().get("code")))
                && (stamp==null || ((Number)stamp).longValue()==((Number)rows.getFirst().get("version")).longValue());
            if(!valid){SecurityContextHolder.clearContext();if(session!=null)session.invalidate();response.sendRedirect(request.getContextPath()+"/login?expired");return;}
            if(session!=null && stamp==null)session.setAttribute("accountVersion",rows.getFirst().get("version"));
            boolean setup=Boolean.TRUE.equals(rows.getFirst().get("must_change_password"))
                    || ("DIRECTOR".equals(rows.getFirst().get("code")) && Boolean.TRUE.equals(rows.getFirst().get("needs_approval_secret")));
            String path=request.getServletPath();
            boolean setupResource=path.equals("/account/initial-setup") || path.equals("/logout")
                    || path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/") || path.equals("/favicon.ico");
            if(setup && !setupResource){response.sendRedirect(request.getContextPath()+"/account/initial-setup");return;}
        }
        chain.doFilter(request,response);
    }
}
