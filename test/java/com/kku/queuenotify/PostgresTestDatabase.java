package com.kku.queuenotify;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.File;
import java.sql.DriverManager;
import java.util.UUID;

/** Embedded PostgreSQL by default, or a fresh schema in the dedicated local test container. */
final class PostgresTestDatabase implements AutoCloseable {
  private final EmbeddedPostgres embedded;
  private final String url;
  private final String username;
  private final String password;

  private PostgresTestDatabase(EmbeddedPostgres embedded, String url, String user, String password) {
    this.embedded = embedded;
    this.url = url;
    this.username = user;
    this.password = password;
  }

  static PostgresTestDatabase start() throws Exception {
    var external = System.getProperty("test.postgres.url");
    if (external == null) {
      var pg = EmbeddedPostgres.builder()
          .setOverrideWorkingDirectory(new File("target/embedded-pg"))
          .setPort(0).start();
      return new PostgresTestDatabase(pg, pg.getJdbcUrl("postgres", "postgres"), "postgres", "postgres");
    }
    // Refuse application/Render URLs and the preserved V2 test database.
    if (!external.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):55433/team_integration_test")) {
      throw new IllegalArgumentException("External tests require the dedicated local team_integration_test database on port 55433");
    }
    var user = System.getProperty("test.postgres.username", "integration_test");
    var password = System.getProperty("test.postgres.password", "integration_test_local");
    var schema = "handoff_test_" + UUID.randomUUID().toString().replace("-", "");
    try (var c = DriverManager.getConnection(external, user, password);
        var s = c.createStatement()) {
      s.execute("CREATE SCHEMA " + schema);
    }
    System.out.println("Created isolated PostgreSQL test schema: " + schema);
    return new PostgresTestDatabase(null, external + "?currentSchema=" + schema, user, password);
  }

  String url() { return url; }
  String username() { return username; }
  String password() { return password; }

  @Override
  public void close() throws Exception {
    if (embedded != null) embedded.close();
    // Keep external test schemas as evidence. No DROP/TRUNCATE or existing data deletion.
  }
}
