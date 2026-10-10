package com.kku.queuenotify.config;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration(proxyBeanMethods=false)
@EnableWebSecurity
@ConditionalOnWebApplication(type=ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {
  @Bean PasswordEncoder staffPasswordEncoder() {return new BCryptPasswordEncoder();}

  /** Blank password leaves login unavailable; no default credential is created. */
  @Bean AuthenticationProvider staffAuthentication(
      @Value("${staff.username:staff}") String username,
      @Value("${staff.password:}") String password, PasswordEncoder staffPasswordEncoder) {
    if (!password.isEmpty()) {
      if (username.isBlank() || username.length()>100 || password.length()<12
          || password.getBytes(StandardCharsets.UTF_8).length>72)
        throw new IllegalArgumentException("Staff configuration requires a username and a password of at least 12 characters and at most 72 UTF-8 bytes");
      var users=new InMemoryUserDetailsManager(User.withUsername(username)
          .password(staffPasswordEncoder.encode(password)).roles("STAFF").build());
      var provider=new DaoAuthenticationProvider();
      provider.setUserDetailsService(users);provider.setPasswordEncoder(staffPasswordEncoder);
      return provider;
    }
    return new AuthenticationProvider() {
      @Override public Authentication authenticate(Authentication authentication) {
        throw new BadCredentialsException("Staff authentication is not configured");
      }
      @Override public boolean supports(Class<?> type) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
      }
    };
  }

  @Bean SecurityFilterChain security(HttpSecurity http,AuthenticationProvider staffAuthentication,org.springframework.core.env.Environment environment) throws Exception {
    var api=new AntPathRequestMatcher("/api/**");
    var login=new LoginUrlAuthenticationEntryPoint("/staff/login");
    http.authenticationProvider(staffAuthentication)
        .authorizeHttpRequests(auth->auth
            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
            .requestMatchers(HttpMethod.GET,"/swagger-ui.html","/swagger-ui/**",
                "/v3/api-docs","/v3/api-docs/**","/v3/api-docs.yaml").hasRole("STAFF")
            .requestMatchers(HttpMethod.GET,"/","/queue/*","/staff/login","/assets/**","/sw.js",
                "/api/v1/menu-items","/api/v1/menu-items/*","/api/v1/menu-items/*/image",
                "/api/v1/push/public-key","/api/v1/csrf","/actuator/health","/error").permitAll()
            .requestMatchers(HttpMethod.GET,"/api/v1/orders","/api/v1/orders/*/notifications").hasRole("STAFF")
            // The services still check each order's owner token; public routing grants no data access.
            .requestMatchers(HttpMethod.GET,"/api/v1/orders/*","/api/v1/queues/*").permitAll()
            // CSRF is mandatory; existing services separately validate each order owner token.
            .requestMatchers(HttpMethod.POST,"/api/v1/orders","/api/v1/orders/*/subscription").permitAll()
            .requestMatchers(HttpMethod.PUT,"/api/v1/orders/*").permitAll()
            .requestMatchers(HttpMethod.DELETE,"/api/v1/orders/*/subscription").permitAll()
            .requestMatchers(HttpMethod.PATCH,"/api/v1/queues/*/cancel").permitAll()
            // Menu mutations, order deletion and queue advancement require STAFF.
            .requestMatchers("/staff/**","/api/v1/menu-items/**","/api/v1/orders/**","/api/v1/queues/**").hasRole("STAFF")
            .requestMatchers("/push-demo.html","/api/v1/push-demo/**").access((authentication,context)->
                new org.springframework.security.authorization.AuthorizationDecision(
                    environment.acceptsProfiles(org.springframework.core.env.Profiles.of("push-demo"))
                    && authentication.get().getAuthorities().stream().anyMatch(role->role.getAuthority().equals("ROLE_STAFF"))))
            .anyRequest().denyAll())
        .formLogin(form->form.loginPage("/staff/login").loginProcessingUrl("/staff/login")
            .defaultSuccessUrl("/staff",true).failureUrl("/staff/login?error").permitAll())
        .httpBasic(basic->basic.disable())
        .logout(logout->logout.logoutUrl("/staff/logout").logoutSuccessUrl("/staff/login?logout")
            .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID"))
        // CSRF stays enabled for every mutation, including public customer routes.
        .exceptionHandling(errors->errors
            .authenticationEntryPoint((request,response,error)->{
              if(api.matches(request)) json(response,401,"Staff login is required");
              else login.commence(request,response,error);
            })
            .accessDeniedHandler((request,response,error)->{
              if(api.matches(request)) json(response,403,"Access denied or CSRF token is missing");
              else response.sendError(403);
            }))
        .headers(headers->headers.referrerPolicy(policy->policy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
    return http.build();
  }

  private static void json(HttpServletResponse response,int status,String message) throws java.io.IOException {
    response.setStatus(status);response.setHeader("Cache-Control","no-store");
    response.setContentType("application/json;charset=UTF-8");
    new com.fasterxml.jackson.databind.ObjectMapper().writeValue(response.getWriter(),
        com.kku.queuenotify.exception.GlobalExceptionHandler.body(org.springframework.http.HttpStatus.valueOf(status),message));
  }
}
