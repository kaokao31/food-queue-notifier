package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
class MigrationTest {
 Flyway flyway(PostgresTestDatabase db,String target){return Flyway.configure().dataSource(db.url(),db.username(),db.password()).locations("classpath:db/migration").target(target).cleanDisabled(true).load();}
 @Test void freshSchemaAppliesAllFiveMigrationsAndIsIdempotent() throws Exception {
  try(var db=PostgresTestDatabase.start()){var migration=flyway(db,"5");assertEquals(5,migration.migrate().migrationsExecuted);migration.validate();assertEquals("5",migration.info().current().getVersion().getVersion());assertEquals(0,migration.migrate().migrationsExecuted);
   try(var c=DriverManager.getConnection(db.url(),db.username(),db.password());var s=c.createStatement();var result=s.executeQuery("SELECT (SELECT count(*) FROM menu_item),(SELECT count(*) FROM queue_daily_counter),(SELECT count(*) FROM menu_item_image)")){assertTrue(result.next());assertEquals(6,result.getInt(1));assertEquals(0,result.getInt(2));assertEquals(0,result.getInt(3));}}
 }
 @Test void v1CatalogSurvivesFullUpgradeWithoutSampleMenuInjection() throws Exception {
  try(var db=PostgresTestDatabase.start()){flyway(db,"1").migrate();try(var c=DriverManager.getConnection(db.url(),db.username(),db.password());var s=c.createStatement()){s.executeUpdate("INSERT INTO menu_item(name,price) VALUES ('Legacy',42.50)");}var migration=flyway(db,"5");assertEquals(4,migration.migrate().migrationsExecuted);migration.validate();
   try(var c=DriverManager.getConnection(db.url(),db.username(),db.password());var s=c.createStatement();var result=s.executeQuery("SELECT name,price,is_available FROM menu_item")){assertTrue(result.next());assertEquals("Legacy",result.getString(1));assertEquals(new java.math.BigDecimal("42.50"),result.getBigDecimal(2));assertTrue(result.getBoolean(3));assertFalse(result.next());}}
 }
}
