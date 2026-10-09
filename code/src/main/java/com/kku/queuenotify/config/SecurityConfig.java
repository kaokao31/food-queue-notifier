package com.kku.queuenotify.config;

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
  /** Suppresses Boot's generated default user until configured BCrypt login arrives in C-R05. */
  @Bean AuthenticationProvider unavailableStaffAuthentication() {
    return new AuthenticationProvider() {
      @Override public Authentication authenticate(Authentication authentication) {
        throw new BadCredentialsException("Staff authentication is not configured");
      }
      @Override public boolean supports(Class<?> type) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
      }
    };
  }

  @Bean SecurityFilterChain security(HttpSecurity http,AuthenticationProvider unavailableStaffAuthentication) throws Exception {
    var api=new AntPathRequestMatcher("/api/**");
    var login=new LoginUrlAuthenticationEntryPoint("/staff/login");
    http.authenticationProvider(unavailableStaffAuthentication)
        .authorizeHttpRequests(auth->auth
            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
            .requestMatchers(HttpMethod.GET,"/","/queue/*","/staff/login","/assets/**","/sw.js",
                "/api/v1/menu-items","/api/v1/menu-items/*","/api/v1/menu-items/*/image",
                "/api/v1/push/public-key","/actuator/health","/error").permitAll()
            .requestMatchers(HttpMethod.GET,"/api/v1/orders","/api/v1/orders/*/notifications").hasRole("STAFF")
            // The services still check each order's owner token; public routing grants no data access.
            .requestMatchers(HttpMethod.GET,"/api/v1/orders/*","/api/v1/queues/*").permitAll()
            // All mutations remain staff-only in this intermediate step, including customer mutations.
            .requestMatchers("/staff/**","/api/v1/menu-items/**","/api/v1/orders/**","/api/v1/queues/**").hasRole("STAFF")
            .anyRequest().denyAll())
        .formLogin(form->form.disable()).httpBasic(basic->basic.disable()).logout(logout->logout.disable())
        // CSRF stays enabled by default. C-R06 adds the browser token API and customer mutation rules.
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
    response.getWriter().write("{\"status\":"+status+",\"message\":\""+message+"\"}");
  }
}
