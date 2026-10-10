package com.kku.queuenotify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.config.OpenApiConfiguration;
import com.kku.queuenotify.controller.api.*;
import com.kku.queuenotify.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Isolated documentation context; production SecurityConfig is deliberately not imported.
 * Documentation authorization is tested separately when its routes are enabled. */
@SpringBootTest(classes=OpenApiDocumentationTest.Web.class)
@AutoConfigureMockMvc
class OpenApiDocumentationTest {
  @Configuration(proxyBeanMethods=false)
  @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class,
      FlywayAutoConfiguration.class,SecurityAutoConfiguration.class,UserDetailsServiceAutoConfiguration.class,
      ManagementWebSecurityAutoConfiguration.class})
  @Import({OpenApiConfiguration.class,MenuController.class,OrderController.class,QueueController.class,
      CsrfController.class,OrderSubscriptionController.class})
  static class Web {}
  @MockBean MenuService menus;
  @MockBean OrderService orders;
  @MockBean QueueService queues;
  @MockBean OrderSubscriptionService subscriptions;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  JsonNode documentation() throws Exception {
    return json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
  }
  @Test void generatesSchemasAndDocumentsActualHeaderAndCookieNames() throws Exception {
    mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    var doc=documentation();
    assertTrue(doc.path("openapi").asText().startsWith("3."));
    assertEquals("Food Queue Notifier API",doc.at("/info/title").asText());
    assertEquals("JSESSIONID",doc.at("/components/securitySchemes/staffSession/name").asText());
    assertEquals("cookie",doc.at("/components/securitySchemes/staffSession/in").asText());
    assertEquals("X-Queue-Token",doc.at("/components/securitySchemes/orderToken/name").asText());
    assertEquals("X-CSRF-TOKEN",doc.at("/components/securitySchemes/csrfToken/name").asText());
    assertTrue(doc.path("components").path("schemas").has("OrderRequest"));
    assertTrue(doc.path("components").path("schemas").has("QueueResponse"));
    doc.path("paths").fieldNames().forEachRemaining(path -> assertTrue(path.startsWith("/api/v1/")));
  }
  @Test void publicReadsAndOrderCreationDoNotRequireStaffAuthentication() throws Exception {
    var paths=documentation().path("paths");
    assertEquals(0,paths.path("/api/v1/menu-items").path("get").path("security").size());
    var create=paths.path("/api/v1/orders").path("post").path("security");
    assertEquals(1,create.size());assertTrue(create.get(0).has("csrfToken"));
    assertFalse(create.get(0).has("staffSession"));
    assertTrue(paths.path("/api/v1/orders").path("get").path("security").get(0).has("staffSession"));
  }
  @Test void orderAccessHasAlternativesAndMutationsRequireCsrfInEachAlternative() throws Exception {
    var paths=documentation().path("paths");
    var read=paths.path("/api/v1/orders/{id}").path("get").path("security");
    assertEquals(2,read.size());assertTrue(read.get(0).has("orderToken"));
    assertTrue(read.get(1).has("staffSession"));assertFalse(read.get(0).has("csrfToken"));
    for (String path:new String[]{"/api/v1/orders/{id}","/api/v1/queues/{id}/cancel",
        "/api/v1/orders/{id}/subscription"}) {
      String method=path.endsWith("/cancel")?"patch":path.endsWith("/subscription")?"post":"put";
      var security=paths.path(path).path(method).path("security");
      assertEquals(2,security.size());
      for(var alternative:security) assertTrue(alternative.has("csrfToken"));
    }
    var advance=paths.path("/api/v1/queues/{id}/advance").path("patch").path("security");
    assertEquals(1,advance.size());assertTrue(advance.get(0).has("staffSession"));
    assertTrue(advance.get(0).has("csrfToken"));
  }
}
