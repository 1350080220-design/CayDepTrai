package com.vietgo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/** Cau hinh dang nhap / dang ky / phan quyen. */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/img/**", "/webjars/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/dang-nhap", "/dang-ky").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/tau-hoa/dat-ve/**", "/xe-khach/dat-ve/**", "/don-hang/**",
                                 "/tu-van/**", "/tai-khoan/**").authenticated()
                .anyRequest().permitAll())
            .formLogin(form -> form
                .loginPage("/dang-nhap")
                .loginProcessingUrl("/dang-nhap")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(successHandler())
                .failureHandler(failureHandler())
                .permitAll())
            .rememberMe(rm -> rm
                .key("vietgo-remember-me-key-2026")
                .rememberMeParameter("remember")
                .tokenValiditySeconds(7 * 24 * 3600)
                .userDetailsService(userDetailsService))
            .logout(logout -> logout
                .logoutUrl("/dang-xuat")
                .logoutSuccessUrl("/?logout")
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/403"));
        return http.build();
    }

    /** Sau khi dang nhap: quay lai trang ?redirect=... (chi chap nhan duong dan noi bo). */
    private AuthenticationSuccessHandler successHandler() {
        SavedRequestAwareAuthenticationSuccessHandler userDelegate = new SavedRequestAwareAuthenticationSuccessHandler();
        userDelegate.setDefaultTargetUrl("/");
        SavedRequestAwareAuthenticationSuccessHandler adminDelegate = new SavedRequestAwareAuthenticationSuccessHandler();
        adminDelegate.setDefaultTargetUrl("/admin");
        return (request, response, authentication) -> {
            String target = safeRedirect(request);
            if (target != null) {
                response.sendRedirect(request.getContextPath() + target);
                return;
            }
            boolean admin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            (admin ? adminDelegate : userDelegate).onAuthenticationSuccess(request, response, authentication);
        };
    }

    private AuthenticationFailureHandler failureHandler() {
        return (request, response, exception) -> {
            String code = exception instanceof DisabledException ? "locked" : "bad";
            String target = safeRedirect(request);
            String url = "/dang-nhap?error=" + code;
            if (target != null) {
                url += "&redirect=" + java.net.URLEncoder.encode(target, java.nio.charset.StandardCharsets.UTF_8);
            }
            response.sendRedirect(request.getContextPath() + url);
        };
    }

    private static String safeRedirect(HttpServletRequest request) {
        String r = request.getParameter("redirect");
        if (r == null || r.isBlank()) return null;
        if (!r.startsWith("/") || r.startsWith("//") || r.contains("\\") || r.contains("\r") || r.contains("\n")) {
            return null;
        }
        return r;
    }
}
