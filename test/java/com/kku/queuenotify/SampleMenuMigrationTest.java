package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.DriverManager;
import java.math.BigDecimal;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
class SampleMenuMigrationTest {
 Flyway flyway(PostgresTestDatabase db,String target){return Flyway.configure().dataSource(db.url(),db.username(),db.password()).locations("classpath:db/migration").target(target).baselineOnMigrate(false).cleanDisabled(true).load();}
 @Test void emptyCatalogGetsSixMenusAndMigrationDoesNotRepeat() throws Exception {
  try(var db=PostgresTestDatabase.start()){
   var migration=flyway(db,"3");assertEquals(3,migration.migrate().migrationsExecuted);migration.validate();assertEquals(0,migration.migrate().migrationsExecuted);
   try(var connection=DriverManager.getConnection(db.url(),db.username(),db.password());var statement=connection.createStatement()){
    try(var result=statement.executeQuery("SELECT count(*),count(DISTINCT name),min(price),bool_and(is_available) FROM menu_item")){assertTrue(result.next());assertEquals(6,result.getInt(1));assertEquals(6,result.getInt(2));assertTrue(result.getBigDecimal(3).compareTo(BigDecimal.ZERO)>0);assertTrue(result.getBoolean(4));}
    try(var result=statement.executeQuery("SELECT name,price FROM menu_item ORDER BY id LIMIT 1")){assertTrue(result.next());assertEquals("ข้าวกะเพราไก่",result.getString(1));assertEquals(new BigDecimal("55.00"),result.getBigDecimal(2));}
    try(var result=statement.executeQuery("SELECT count(*) FROM customer")){result.next();assertEquals(0,result.getInt(1));}
   }
  }
 }
 @Test void existingCatalogIsPreservedIncludingClosedMenuAndIdentity() throws Exception {
  try(var db=PostgresTestDatabase.start()){
   flyway(db,"2").migrate();
   try(var connection=DriverManager.getConnection(db.url(),db.username(),db.password());var statement=connection.createStatement()){
    statement.executeUpdate("INSERT INTO menu_item(name,category,price,prep_time_minutes,is_available) VALUES ('Own menu','Own category',17.25,9,false)");
    long id;try(var result=statement.executeQuery("SELECT id FROM menu_item")){result.next();id=result.getLong(1);}
    var migration=flyway(db,"3");assertEquals(1,migration.migrate().migrationsExecuted);migration.validate();
    try(var result=statement.executeQuery("SELECT id,name,category,price,prep_time_minutes,is_available FROM menu_item")){assertTrue(result.next());assertEquals(id,result.getLong(1));assertEquals("Own menu",result.getString(2));assertEquals("Own category",result.getString(3));assertEquals(new BigDecimal("17.25"),result.getBigDecimal(4));assertEquals(9,result.getInt(5));assertFalse(result.getBoolean(6));assertFalse(result.next());}
    statement.executeUpdate("INSERT INTO menu_item(name,price) VALUES ('Next menu',1)");try(var result=statement.executeQuery("SELECT max(id) FROM menu_item")){result.next();assertTrue(result.getLong(1)>id);}
   }
  }
 }
}
