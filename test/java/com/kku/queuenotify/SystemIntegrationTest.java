package com.kku.queuenotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;
import com.kku.queuenotify.service.WebPushSender;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "staff.username=staff",
      "staff.password=integration-only-password",
      "spring.profiles.active=push-demo"
    })
@AutoConfigureMockMvc
class SystemIntegrationTest {
  static final boolean H2 = "h2".equals(System.getProperty("test.database"));
  static final PostgresTestDatabase PG = H2 ? null : start();

  static PostgresTestDatabase start() {
    try {
      return PostgresTestDatabase.start();
    } catch (Exception ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  @DynamicPropertySource
  static void db(DynamicPropertyRegistry r) {
    if (H2) {
      r.add(
          "spring.datasource.url",
          () -> "jdbc:h2:mem:integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
      r.add("spring.datasource.username", () -> "sa");
      r.add("spring.datasource.password", () -> "");
      r.add("spring.flyway.enabled", () -> false);
      r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    } else {
      r.add("spring.datasource.url", PG::url);
      r.add("spring.datasource.username", PG::username);
      r.add("spring.datasource.password", PG::password);
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @MockBean WebPushSender sender;
  @MockBean java.time.Clock clock;
  @Autowired com.kku.queuenotify.service.QueueNumberService queueNumbers;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactions;

  @AfterAll
  static void stop() throws Exception {
    if (PG != null) PG.close();
  }

  @BeforeEach
  void seed() {
    when(clock.instant()).thenReturn(java.time.Instant.parse("2035-01-01T05:00:00Z"));
    if (H2) {
      jdbc.execute("CREATE SEQUENCE IF NOT EXISTS queue_number_seq");
      if (jdbc.queryForObject("select count(*) from menu_item", Integer.class) == 0) {
        for (int i = 1; i <= 6; i++)
          jdbc.update(
              "insert into menu_item(id,name,category,price,is_available) values(?,?,?,?,true)",
              i,
              "Menu " + i,
              "อาหารจานเดียว",
              55);
        jdbc.execute("ALTER TABLE menu_item ALTER COLUMN id RESTART WITH 100");
      }
    }
  }

  JsonNode response(org.springframework.test.web.servlet.ResultActions a) throws Exception {
    return json.readTree(
        a.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
  }

  JsonNode order(int quantity) throws Exception {
    return response(
        mvc.perform(
                post("/api/v1/orders")
                    .with(csrf())
                    .contentType("application/json")
                    .content("{\"items\":[{\"menuItemId\":1,\"quantity\":" + quantity + "}]}"))
            .andExpect(status().isCreated()));
  }

  byte[] menuPicture(String format, int color) throws Exception {
    var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
    image.setRGB(0, 0, color);
    var out = new java.io.ByteArrayOutputStream();
    assertTrue(javax.imageio.ImageIO.write(image, format, out));
    return out.toByteArray();
  }

  org.springframework.mock.web.MockMultipartFile menuPart(String name) {
    return new org.springframework.mock.web.MockMultipartFile(
        "menu",
        "",
        "application/json",
        ("{\"name\":\"" + name + "\",\"price\":50,\"category\":\"Test\",\"isAvailable\":true}")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void staffMenuImageCreateReplaceAndDelete() throws Exception {
    byte[] png = menuPicture("png", 0x00ff00);
    var file =
        new org.springframework.mock.web.MockMultipartFile("file", "photo.png", "image/png", png);
    var created =
        response(
            mvc.perform(
                    multipart("/api/v1/menu-items/with-image")
                        .file(menuPart("Image menu"))
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isCreated()));
    long id = created.get("id").asLong();
    String oldUrl = created.get("imageUrl").asText();
    assertTrue(oldUrl.startsWith("/api/v1/menu-items/" + id + "/image?v="));
    mvc.perform(get(oldUrl).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(content().bytes(png));
    byte[] jpg = menuPicture("jpg", 0xff0000);
    var replacement =
        new org.springframework.mock.web.MockMultipartFile("file", "photo.jpg", "image/jpeg", jpg);
    var updated =
        response(
            mvc.perform(
                    multipart("/api/v1/menu-items/" + id + "/with-image")
                        .file(menuPart("Image menu updated"))
                        .file(replacement)
                        .with(
                            req -> {
                              req.setMethod("PUT");
                              return req;
                            })
                        .with(csrf()))
                .andExpect(status().isOk()));
    assertNotEquals(oldUrl, updated.get("imageUrl").asText());
    mvc.perform(get(updated.get("imageUrl").asText()).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/jpeg"))
        .andExpect(content().bytes(jpg));
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from menu_item_image where menu_item_id=?", Integer.class, id));
    mvc.perform(delete("/api/v1/menu-items/" + id).with(csrf())).andExpect(status().isNoContent());
    assertEquals(
        0,
        jdbc.queryForObject(
            "select count(*) from menu_item_image where menu_item_id=?", Integer.class, id));
    mvc.perform(get("/api/v1/menu-items/" + id + "/image")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void menuUploadRequiresStaffCsrfAndRejectsInvalidFilesWithoutCreatingMenu() throws Exception {
    var valid =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "photo.png", "image/png", menuPicture("png", 0x0000ff));
    mvc.perform(
            multipart("/api/v1/menu-items/with-image")
                .file(menuPart("No auth"))
                .file(valid)
                .with(anonymous())
                .with(csrf()))
        .andExpect(status().isUnauthorized());
    mvc.perform(multipart("/api/v1/menu-items/with-image").file(menuPart("No CSRF")).file(valid))
        .andExpect(status().isForbidden());
    int before = jdbc.queryForObject("select count(*) from menu_item", Integer.class);
    var svg =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "fake.png", "image/png", "<svg onload='alert(1)'></svg>".getBytes());
    mvc.perform(
            multipart("/api/v1/menu-items/with-image")
                .file(menuPart("Invalid"))
                .file(svg)
                .with(csrf()))
        .andExpect(status().isBadRequest());
    byte[] corrupt = Arrays.copyOf(valid.getBytes(), 16);
    mvc.perform(
            multipart("/api/v1/menu-items/with-image")
                .file(menuPart("Corrupt"))
                .file(
                    new org.springframework.mock.web.MockMultipartFile(
                        "file", "corrupt.png", "image/png", corrupt))
                .with(csrf()))
        .andExpect(status().isBadRequest());
    var large =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "large.jpg", "image/jpeg", new byte[2 * 1024 * 1024 + 1]);
    mvc.perform(
            multipart("/api/v1/menu-items/with-image")
                .file(menuPart("Too large"))
                .file(large)
                .with(csrf()))
        .andExpect(status().isPayloadTooLarge());
    mvc.perform(multipart("/api/v1/menu-items/with-image").file(menuPart("No file")).with(csrf()))
        .andExpect(status().isBadRequest());
    assertEquals(before, jdbc.queryForObject("select count(*) from menu_item", Integer.class));
  }

  @Test
  void queuesRestartAtBangkokMidnightWithoutChangingExistingOrders() throws Exception {
    when(clock.instant()).thenReturn(java.time.Instant.parse("2040-06-01T16:59:59Z"));
    var old = order(1);
    assertEquals("2040-06-01", old.get("queue").get("queueDate").asText());
    assertEquals(1, old.get("queue").get("queueNumber").asInt());
    when(clock.instant()).thenReturn(java.time.Instant.parse("2040-06-01T17:00:00Z"));
    var fresh = order(1);
    assertEquals("2040-06-02", fresh.get("queue").get("queueDate").asText());
    assertEquals(1, fresh.get("queue").get("queueNumber").asInt());
    assertNotEquals(old.get("id"), fresh.get("id"));
    assertEquals(2, order(1).get("queue").get("queueNumber").asInt());
    var preserved =
        response(
            mvc.perform(
                    get("/api/v1/orders/" + old.get("id").asLong())
                        .header("X-Queue-Token", old.get("queueToken").asText()))
                .andExpect(status().isOk()));
    for (var field : List.of("id", "queueNumber", "queueDate", "status"))
      assertEquals(old.get("queue").get(field), preserved.get("queue").get(field));
    assertEquals(old.get("items"), preserved.get("items"));
    assertEquals("WAITING", preserved.get("queue").get("status").asText());
  }

  @Test
  void queueCounterContinuesAbove999AndRollsBackWithOrderTransaction() throws Exception {
    when(clock.instant()).thenReturn(java.time.Instant.parse("2041-01-01T05:00:00Z"));
    jdbc.update(
        "INSERT INTO queue_daily_counter(queue_date,last_number) VALUES (?,999)",
        java.time.LocalDate.parse("2041-01-01"));
    var tx = new org.springframework.transaction.support.TransactionTemplate(transactions);
    assertThrows(
        IllegalStateException.class,
        () ->
            tx.execute(
                status -> {
                  assertEquals(1000, queueNumbers.next().value());
                  throw new IllegalStateException("test rollback");
                }));
    assertEquals(
        999,
        jdbc.queryForObject(
            "SELECT last_number FROM queue_daily_counter WHERE queue_date=?",
            Integer.class,
            java.time.LocalDate.parse("2041-01-01")));
    assertEquals(1000, order(1).get("queue").get("queueNumber").asInt());
    assertEquals(1001, order(1).get("queue").get("queueNumber").asInt());
  }

  @Test
  void freshMigrationsAndHibernateValidate() {
    if (!H2)
      assertEquals(
          5,
          jdbc.queryForObject(
              "select count(*) from flyway_schema_history where success", Integer.class));
    assertTrue(jdbc.queryForObject("select count(*) from menu_item", Integer.class) >= 6);
  }

  @Test
  void pagesRenderAndMissingAssetsAre404() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("ตะกร้าของคุณ")));
    mvc.perform(get("/staff/login")).andExpect(status().isOk());
    mvc.perform(get("/assets/not-present.png")).andExpect(status().isNotFound());
  }

  @Test
  void publicMenuPaginationAndSortValidation() throws Exception {
    mvc.perform(get("/api/v1/menu-items?size=2&sort=price,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2));
    mvc.perform(get("/api/v1/menu-items?sort=password,asc")).andExpect(status().isBadRequest());
  }

  @Test
  void anonymousCannotWriteMenuReadStaffListOrAdvance() throws Exception {
    mvc.perform(
            post("/api/v1/menu-items").with(csrf()).contentType("application/json").content("{}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
    mvc.perform(patch("/api/v1/queues/1/advance").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void csrfIsRequired() throws Exception {
    mvc.perform(post("/api/v1/orders").contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/csrf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isString());
  }

  @Test
  void createOrderUsesServerPriceAndTokenIsolation() throws Exception {
    var a = order(2);
    var b = order(1);
    assertEquals(
        0, new java.math.BigDecimal("110.00").compareTo(a.get("totalAmount").decimalValue()));
    assertEquals(a.get("id").asLong(), a.get("queue").get("id").asLong());
    assertNotEquals(a.get("queue").get("queueNumber"), b.get("queue").get("queueNumber"));
    mvc.perform(
            get("/api/v1/orders/" + a.get("id").asLong())
                .header("X-Queue-Token", b.get("queueToken").asText()))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/orders/" + a.get("id").asLong())
                .header("X-Queue-Token", a.get("queueToken").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.queueToken").isEmpty());
  }

  @Test
  void invalidOrDuplicateItemsDoNotCreatePartialOrders() throws Exception {
    var before = jdbc.queryForObject("select count(*) from orders", Integer.class);
    mvc.perform(
            post("/api/v1/orders")
                .with(csrf())
                .contentType("application/json")
                .content("{\"items\":[{\"menuItemId\":1,\"quantity\":0}]}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/orders")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"items\":[{\"menuItemId\":1,\"quantity\":1},{\"menuItemId\":999999,\"quantity\":1}]}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/v1/orders")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"items\":[{\"menuItemId\":1,\"quantity\":1},{\"menuItemId\":1,\"quantity\":2}]}"))
        .andExpect(status().isBadRequest());
    assertEquals(before, jdbc.queryForObject("select count(*) from orders", Integer.class));
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void menuCrudAndHistoryProtection() throws Exception {
    var menu =
        response(
            mvc.perform(
                    post("/api/v1/menu-items")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"name\":\"test-menu\",\"price\":10,\"isAvailable\":true}"))
                .andExpect(status().isCreated()));
    long id = menu.get("id").asLong();
    mvc.perform(
            put("/api/v1/menu-items/" + id)
                .with(csrf())
                .contentType("application/json")
                .content("{\"name\":\"edited-menu\",\"price\":12,\"isAvailable\":false}"))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/v1/menu-items/" + id).with(csrf())).andExpect(status().isNoContent());
    order(1);
    mvc.perform(delete("/api/v1/menu-items/1").with(csrf())).andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void stateAndOrderCrudRules() throws Exception {
    var o = order(1);
    long id = o.get("id").asLong();
    mvc.perform(
            put("/api/v1/orders/" + id)
                .with(csrf())
                .contentType("application/json")
                .content("{\"items\":[{\"menuItemId\":1,\"quantity\":3}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalAmount").value(165));
    mvc.perform(patch("/api/v1/queues/" + id + "/advance").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PREPARING"));
    mvc.perform(
            put("/api/v1/orders/" + id)
                .with(csrf())
                .contentType("application/json")
                .content("{\"items\":[{\"menuItemId\":1,\"quantity\":1}]}"))
        .andExpect(status().isConflict());
    mvc.perform(patch("/api/v1/queues/" + id + "/cancel").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    mvc.perform(patch("/api/v1/queues/" + id + "/advance").with(csrf()))
        .andExpect(status().isConflict());
    var fresh = order(1);
    mvc.perform(delete("/api/v1/orders/" + fresh.get("id").asLong()).with(csrf()))
        .andExpect(status().isNoContent());
    assertEquals(
        0,
        jdbc.queryForObject(
            "select count(*) from queue where id=?", Integer.class, fresh.get("id").asLong()));
  }

  String subscription() {
    byte[] key = new byte[65];
    key[0] = 4;
    return "{\"endpoint\":\"https://fcm.googleapis.com/wp/integration-"
        + UUID.randomUUID()
        + "\",\"keys\":{\"p256dh\":\""
        + Base64.getUrlEncoder().withoutPadding().encodeToString(key)
        + "\",\"auth\":\""
        + Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16])
        + "\"}}";
  }

  @Test
  void endpointValidationRejectsInternalUrls() throws Exception {
    var o = order(1);
    var body = subscription().replace("https://fcm.googleapis.com/wp/", "https://127.0.0.1/wp/");
    mvc.perform(
            put("/api/v1/orders/" + o.get("id").asLong() + "/push-subscription")
                .with(csrf())
                .header("X-Queue-Token", o.get("queueToken").asText())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void readyPushTargetsOnlyAttachedOrderAndLogsAcceptance() throws Exception {
    var first = order(1);
    var second = order(1);
    String body = subscription();
    long id = first.get("id").asLong();
    mvc.perform(
            put("/api/v1/orders/" + id + "/push-subscription")
                .with(csrf())
                .header("X-Queue-Token", first.get("queueToken").asText())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isNoContent());
    when(sender.send(any(PushSubscriptionRequest.class), any(PushPayload.class))).thenReturn(201);
    mvc.perform(patch("/api/v1/queues/" + id + "/advance").with(csrf())).andExpect(status().isOk());
    verify(sender, never()).send(any(), any());
    mvc.perform(patch("/api/v1/queues/" + id + "/advance").with(csrf())).andExpect(status().isOk());
    verify(sender, timeout(5000).times(1))
        .send(
            argThat(s -> s.endpoint().equals(uncheckedEndpoint(body))),
            argThat(p -> p.url().equals("/queue/" + id)));
    for (int attempt = 0; attempt < 50; attempt++) {
      if (jdbc.queryForObject(
              "select count(*) from notification_log where queue_id=? and"
                  + " delivery_status='ACCEPTED'",
              Integer.class,
              id)
          == 1) break;
      Thread.sleep(100);
    }
    assertEquals(
        1,
        jdbc.queryForObject(
            "select count(*) from notification_log where queue_id=? and delivery_status='ACCEPTED'",
            Integer.class,
            id));
    assertEquals(
        0,
        jdbc.queryForObject(
            "select count(*) from notification_log where queue_id=?",
            Integer.class,
            second.get("id").asLong()));
  }

  String uncheckedEndpoint(String b) {
    try {
      return json.readTree(b).get("endpoint").asText();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Test
  void concurrentCreationHasUniqueQueueNumbers() throws Exception {
    when(clock.instant()).thenReturn(java.time.Instant.parse("2042-01-01T05:00:00Z"));
    var executor = Executors.newFixedThreadPool(4);
    try {
      List<Future<Long>> futures = new ArrayList<>();
      for (int i = 0; i < 8; i++)
        futures.add(executor.submit(() -> order(1).get("queue").get("queueNumber").asLong()));
      Set<Long> numbers = new HashSet<>();
      for (var f : futures) assertTrue(numbers.add(f.get(15, TimeUnit.SECONDS)));
      assertEquals(Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), numbers);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void staffPagesAndSwaggerAreAvailable() throws Exception {
    mvc.perform(get("/staff")).andExpect(status().isOk());
    mvc.perform(get("/staff/menu")).andExpect(status().isOk());
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/v1/orders']").exists());
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void qrEncodesOnlyValidatedPublicMenuUrl() throws Exception {
    String url = "https://example.onrender.com/";
    byte[] png =
        mvc.perform(get("/staff/qr.png").param("url", url))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
    int[] pixels =
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    var source =
        new com.google.zxing.RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels);
    var decoded =
        new com.google.zxing.qrcode.QRCodeReader()
            .decode(
                new com.google.zxing.BinaryBitmap(
                    new com.google.zxing.common.HybridBinarizer(source)));
    assertEquals(url, decoded.getText());
    mvc.perform(get("/staff/qr.png").param("url", "http://localhost:8080/"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/staff/qr.png").param("url", "https://example.com/?token=secret"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void staffCanActuallyAuthenticateWithConfiguredPassword() throws Exception {
    var result =
        mvc.perform(
                post("/staff/login")
                    .with(csrf())
                    .param("username", "staff")
                    .param("password", "integration-only-password"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/staff"))
            .andReturn();
    var session =
        (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false);
    assertNotNull(session);
    mvc.perform(get("/staff").session(session)).andExpect(status().isOk());
  }
}
