package com.c7smile.clinic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public pages
                .requestMatchers("/", "/services", "/dich-vu/**", "/bac-si/**",
                        "/about", "/contact", "/news",
                        "/register", "/verify-email",
                        "/css/**", "/js/**", "/images/**", "/uploads/**",
                        "/h2-console/**").permitAll()
                // Role-based protection
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/customer/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers("/reception/**").hasAnyRole("RECEPTION", "ADMIN")
                .requestMatchers("/doctor/**").hasAnyRole("DOCTOR", "ADMIN")
                // Authenticated pages (any logged-in user)
                .requestMatchers("/booking", "/profile").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(authSuccessHandler())
                .failureHandler(authFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(request -> request.getServletPath().equals("/logout") && request.getMethod().equalsIgnoreCase("GET"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // H2 console support
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    /**
     * Redirect người dùng sau khi đăng nhập thành công dựa trên Role.
     * Nếu có SavedRequest (người dùng bị redirect từ trang khác), ưu tiên quay lại trang đó.
     */
    @Bean
    public AuthenticationSuccessHandler authSuccessHandler() {
        return (request, response, authentication) -> {
            // Check if there's a saved request (user was redirected to login)
            HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
            SavedRequest savedRequest = requestCache.getRequest(request, response);

            if (savedRequest != null) {
                requestCache.removeRequest(request, response);
                response.sendRedirect(savedRequest.getRedirectUrl());
                return;
            }

            // Redirect based on role
            String role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_CUSTOMER");

            String redirectUrl = switch (role) {
                case "ROLE_ADMIN" -> "/admin/dashboard";
                case "ROLE_RECEPTION" -> "/reception/dashboard";
                case "ROLE_DOCTOR" -> "/doctor/dashboard";
                default -> "/customer/dashboard";
            };

            response.sendRedirect(redirectUrl);
        };
    }

    @Bean
    public AuthenticationFailureHandler authFailureHandler() {
        return (request, response, exception) -> {
            if (exception instanceof DisabledException) {
                response.sendRedirect("/login?error=disabled");
            } else {
                response.sendRedirect("/login?error=true");
            }
        };
    }
}
