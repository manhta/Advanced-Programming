package mrm.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import mrm.authentication.DatabaseUserDetailsService;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        String encoderId = "pbkdf2@SpringSecurity_v5_8";

        return new DelegatingPasswordEncoder(
                encoderId,
                Map.of(
                        encoderId,
                        Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()
                )
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DatabaseUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            org.springframework.jdbc.core.JdbcTemplate jdbc,
            mrm.audit.AuditService audit
    ) throws Exception {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        http
                .authenticationProvider(provider)
                .addFilterAfter(new AccountStateFilter(jdbc), org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
                .exceptionHandling(errors -> errors.accessDeniedHandler((request,response,exception) -> {
                    // A stale login tab must obtain a fresh form, never bypass CSRF or replay the password.
                    if(exception instanceof org.springframework.security.web.csrf.CsrfException
                            && "POST".equals(request.getMethod()) && "/login".equals(request.getServletPath())) {
                        response.sendRedirect(request.getContextPath()+"/login?expired");
                        return;
                    }
                    new org.springframework.security.web.access.AccessDeniedHandlerImpl().handle(request,response,exception);
                }))

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/",
                                "/login",
                                "/error",
                                "/favicon.ico",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        .requestMatchers("/reader/**")
                        .hasRole("READER")

                        .requestMatchers("/archivist/**")
                        .hasRole("ARCHIVIST")

                        .requestMatchers("/director/inventories", "/director/inventories/**").denyAll()
                        .requestMatchers("/director/reports", "/director/reports/**").denyAll()

                        .requestMatchers("/director/**")
                        .hasRole("DIRECTOR")

                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/account/profile", "/account/password").hasAnyRole("READER","ARCHIVIST","DIRECTOR")

                        .requestMatchers("/account/profile/edit", "/account/change-requests", "/account/change-requests/**").hasAnyRole("READER","ARCHIVIST","DIRECTOR")

                        .requestMatchers("/account/initial-setup").authenticated()

                        .requestMatchers("/home", "/notifications", "/notifications/**")
                        .authenticated()

                        .anyRequest()
                        .denyAll()
                )

                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/login?error")
                        .successHandler((request,response,authentication) -> {
                            request.getSession().setAttribute("accountVersion",jdbc.queryForObject("SELECT version FROM app_users WHERE username=?",Long.class,authentication.getName()));
                            request.getSession().setAttribute("showLoginToast",Boolean.TRUE);
                            audit.log(authentication.getName(),"LOGIN","AUTH",authentication.getName(),"Đăng nhập thành công.");
                            Boolean setup=jdbc.queryForObject("SELECT u.must_change_password OR (r.code='DIRECTOR' AND u.approval_secret_hash IS NULL) FROM app_users u JOIN roles r ON r.id=u.role_id WHERE u.username=?",Boolean.class,authentication.getName());
                            response.sendRedirect(request.getContextPath()+(Boolean.TRUE.equals(setup)?"/account/initial-setup":"/home"));
                        })
                        .failureHandler((request,response,exception) -> {
                            audit.failure(request.getParameter("username"),"LOGIN","AUTH",null,"Đăng nhập không thành công.");
                            response.sendRedirect(request.getContextPath()+"/login?error");
                        })
                        .permitAll()
                )

                .logout(logout -> logout
                        .addLogoutHandler((request,response,authentication) -> {
                            if(authentication!=null)audit.log(authentication.getName(),"LOGOUT","AUTH",authentication.getName(),"Đăng xuất.");
                        })
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}
