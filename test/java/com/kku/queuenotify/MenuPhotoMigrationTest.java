package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
class MenuPhotoMigrationTest {
  @Test void upgradeAssignsPhotosAndPreservesUploadKeysAndRenames() throws Exception {
    try(var db=PostgresTestDatabase.start()) {
      Flyway.configure().dataSource(db.url(),db.username(),db.password())
          .locations("classpath:db/migration").target("5").load().migrate();
      try(var c=DriverManager.getConnection(db.url(),db.username(),db.password());var s=c.createStatement()) {
        String upload="a".repeat(64);
        s.executeUpdate("UPDATE menu_item SET image_key='"+upload+"' WHERE name='ข้าวไก่ทอด'");
        var migration=Flyway.configure().dataSource(db.url(),db.username(),db.password())
            .locations("classpath:db/migration").target("6").load();
        assertEquals(1,migration.migrate().migrationsExecuted);
        try(var r=s.executeQuery("SELECT image_key FROM menu_item WHERE name='ข้าวกะเพราไก่'")) {
          assertTrue(r.next());assertEquals("asset:basil-chicken-v2.webp",r.getString(1));
        }
        try(var r=s.executeQuery("SELECT image_key FROM menu_item WHERE name='ข้าวไก่ทอด'")) {
          assertTrue(r.next());assertEquals(upload,r.getString(1));
        }
        s.executeUpdate("UPDATE menu_item SET name='test' WHERE name='ข้าวกะเพราไก่'");
        assertEquals(0,migration.migrate().migrationsExecuted);
        try(var r=s.executeQuery("SELECT image_key FROM menu_item WHERE name='test'")) {
          assertTrue(r.next());assertEquals("asset:basil-chicken-v2.webp",r.getString(1));
        }
      }
    }
  }
}
