package com.kku.queuenotify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService staff(
      @Value("${staff.username:staff}") String user,
      @Value("${staff.password}") String password,
      PasswordEncoder encoder) {
    if (password.length() < 12)
      throw new IllegalStateException("STAFF_PASSWORD must contain at least 12 characters");
    return new InMemoryUserDetailsManager(
        User.withUsername(user)
            .password(encoder.encode(password))
            .roles("STAFF")
            .build());
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/staff/login",
                        "/",
                        "/queue/**",
                        "/assets/**",
                        "/sw.js",
                        "/push-demo.html",
                        "/api/v1/csrf",
                        "/api/v1/push/**",
                        "/actuator/health",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/error")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/menu-items/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/orders")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/orders")
                    .hasRole("STAFF")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/*")
                    .hasRole("STAFF")
                    .requestMatchers(
                        "/api/v1/queues/*/advance", "/api/v1/notifications/**", "/staff/**")
                    .hasRole("STAFF")
                    .requestMatchers("/api/v1/orders/**", "/api/v1/queues/**")
                    .permitAll()
                    .anyRequest()
                    .hasRole("STAFF"))
        .formLogin(
            f ->
                f.loginPage("/staff/login")
                    .loginProcessingUrl("/staff/login")
                    .defaultSuccessUrl("/staff", true)
                    .failureUrl("/staff/login?error")
                    .permitAll())
        .logout(l -> l.logoutUrl("/staff/logout").logoutSuccessUrl("/staff/login"))
        .exceptionHandling(
            e ->
                e.defaultAuthenticationEntryPointFor(
                    (req, res, ex) -> {
                      res.setStatus(401);
                      res.setContentType("application/json;charset=UTF-8");
                      res.getWriter()
                          .write("{\"message\":\"กรุณาเข้าสู่ระบบพนักงาน\",\"status\":401}");
                    },
                    new AntPathRequestMatcher("/api/**")))
        .headers(
            h ->
                h.contentTypeOptions(c -> {})
                    .referrerPolicy(
                        r ->
                            r.policy(
                                org.springframework.security.web.header.writers
                                    .ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
    return http.build();
  }
}
