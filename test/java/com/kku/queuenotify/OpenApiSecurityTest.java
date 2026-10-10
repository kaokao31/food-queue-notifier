package com.kku.queuenotify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.config.OpenApiConfiguration;
import com.kku.queuenotify.config.SecurityConfig;
import com.kku.queuenotify.controller.api.*;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.response.QueueResponse;
import com.kku.queuenotify.service.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real production filter chain and generated documents; business services are test doubles. */
@SpringBootTest(classes=OpenApiSecurityTest.Web.class,properties={
    "staff.username=docs-staff-fixture","staff.password=docs-test-password-31"})
@AutoConfigureMockMvc
class OpenApiSecurityTest {
  @Configuration(proxyBeanMethods=false)
  @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class,
      FlywayAutoConfiguration.class})
  @Import({SecurityConfig.class,OpenApiConfiguration.class,MenuController.class,OrderController.class,
      QueueController.class,CsrfController.class,OrderSubscriptionController.class})
  static class Web {}
  @MockBean MenuService menus;
  @MockBean OrderService orders;
  @MockBean QueueService queues;
  @MockBean OrderSubscriptionService subscriptions;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  private final String[] documentationPaths={"/swagger-ui.html","/swagger-ui/index.html",
      "/swagger-ui/swagger-ui.css","/swagger-ui/swagger-ui-bundle.js",
      "/v3/api-docs","/v3/api-docs/swagger-config","/v3/api-docs.yaml"};

  @Test void anonymousDocumentationRequestsRedirectToStaffLogin() throws Exception {
    for(String path:documentationPaths)
      mvc.perform(get(path)).andExpect(status().isFound())
          .andExpect(redirectedUrl("http://localhost/staff/login"));
  }
  @Test void orderTokenOrNonStaffUserCannotReadDocumentation() throws Exception {
    for(String path:documentationPaths) {
      mvc.perform(get(path).header("X-Queue-Token","owner-test-fixture"))
          .andExpect(status().isFound());
      mvc.perform(get(path).with(user("customer-fixture").roles("CUSTOMER")))
          .andExpect(status().isForbidden());
    }
  }
  MockHttpSession login() throws Exception {
    var session=new MockHttpSession();
    mvc.perform(post("/staff/login").session(session).with(csrf())
        .param("username","docs-staff-fixture").param("password","docs-test-password-31"))
        .andExpect(status().isFound()).andExpect(redirectedUrl("/staff"));
    return session;
  }
  @Test void configuredStaffSessionReadsSwaggerAssetsAndGeneratedDocuments() throws Exception {
    var session=login();
    mvc.perform(get("/swagger-ui.html").session(session)).andExpect(status().is3xxRedirection());
    for(String path:documentationPaths) if(!path.equals("/swagger-ui.html"))
      mvc.perform(get(path).session(session)).andExpect(status().isOk());
    mvc.perform(get("/v3/api-docs").session(session))
        .andExpect(jsonPath("$.info.title").value("Food Queue Notifier API"));
    // Only GET documentation routes are granted; unknown application paths remain denied.
    mvc.perform(post("/v3/api-docs").session(session).with(csrf())).andExpect(status().isForbidden());
    mvc.perform(get("/unknown-fixture").session(session)).andExpect(status().isForbidden());
  }
  @Test void documentationAccessKeepsMutationCsrfAndStaffRequirements() throws Exception {
    var session=login();
    mvc.perform(patch("/api/v1/queues/8/advance").session(session)).andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/orders")).andExpect(status().isForbidden());
    mvc.perform(patch("/api/v1/queues/8/advance").with(csrf())).andExpect(status().isUnauthorized());
    mvc.perform(patch("/api/v1/queues/8/advance").with(user("customer-fixture").roles("CUSTOMER"))
        .with(csrf())).andExpect(status().isForbidden());
    verifyNoInteractions(queues,orders);
    // Use the token returned by the real endpoint, as Swagger users do after login.
    var token=json.readTree(mvc.perform(get("/api/v1/csrf").session(session)).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
    when(queues.advance(8L)).thenReturn(new QueueResponse(8L,4,LocalDate.of(2026,10,10),
        QueueStatus.PREPARING,LocalDateTime.of(2026,10,10,12,0)));
    mvc.perform(patch("/api/v1/queues/8/advance").session(session)
        .header(token.path("headerName").asText(),token.path("token").asText()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PREPARING"));
    verify(queues).advance(8L);
  }
}
