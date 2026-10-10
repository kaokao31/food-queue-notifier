package com.kku.queuenotify.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentation only: actual authorization remains in SecurityConfig and services. */
@Configuration(proxyBeanMethods=false)
public class OpenApiConfiguration {
  @Bean
  OpenAPI queueOpenApi() {
    return new OpenAPI().info(new Info().title("Food Queue Notifier API").version("1.0")
        .description("Menu, order, queue and Web Push APIs. Log in at /staff/login in the same browser "
            + "to use the STAFF session (JSESSIONID); Swagger cannot create a staff session via Authorize. "
            + "Customer order access uses X-Queue-Token returned when creating that order; it is not a staff credential. "
            + "Before POST/PUT/PATCH/DELETE, GET /api/v1/csrf and enter its token in Authorize > csrfToken "
            + "(header X-CSRF-TOKEN). Fetch a fresh token after login/logout. "
            + "Use only test orders: Try it out changes real data. Documentation access will be configured separately."))
        .components(new Components()
            .addSecuritySchemes("staffSession", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE).name("JSESSIONID")
                .description("Log in at /staff/login; the browser sends the session cookie automatically. STAFF role required."))
            .addSecuritySchemes("orderToken", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER).name("X-Queue-Token")
                .description("Token for this specific order, returned on order creation. Never use another order's token."))
            .addSecuritySchemes("csrfToken", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER).name("X-CSRF-TOKEN")
                .description("GET /api/v1/csrf in this session; copy token here. Refresh after login/logout.")));
  }

  @Bean
  OpenApiCustomizer queueAccessDocumentation() {
    return api -> {
      if (api.getPaths()==null) return;
      api.getPaths().entrySet().removeIf(entry -> !entry.getKey().startsWith("/api/v1/"));
      api.getPaths().forEach((path,item) -> item.readOperationsMap().forEach((method,operation) -> {
        boolean mutation=method!=PathItem.HttpMethod.GET && method!=PathItem.HttpMethod.HEAD
            && method!=PathItem.HttpMethod.OPTIONS;
        boolean owner=path.equals("/api/v1/orders/{id}") && method!=PathItem.HttpMethod.DELETE
            || path.equals("/api/v1/queues/{id}")
            || path.equals("/api/v1/queues/{id}/cancel")
            || path.equals("/api/v1/orders/{id}/subscription");
        boolean staff=path.startsWith("/api/v1/push-demo/")
            || path.equals("/api/v1/orders") && method==PathItem.HttpMethod.GET
            || path.equals("/api/v1/orders/{id}") && method==PathItem.HttpMethod.DELETE
            || path.endsWith("/notifications")
            || path.equals("/api/v1/queues/{id}/advance")
            || path.startsWith("/api/v1/menu-items") && mutation;
        List<SecurityRequirement> requirements=new ArrayList<>();
        if (owner) {
          requirements.add(requirement("orderToken",mutation));
          requirements.add(requirement("staffSession",mutation));
        } else if (staff) {
          requirements.add(requirement("staffSession",mutation));
        } else if (mutation) {
          requirements.add(new SecurityRequirement().addList("csrfToken"));
        }
        operation.setSecurity(requirements);
      }));
    };
  }

  private static SecurityRequirement requirement(String access,boolean mutation) {
    var result=new SecurityRequirement().addList(access);
    if (mutation) result.addList("csrfToken");
    return result;
  }
}
