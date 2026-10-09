package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
class DailyQueueMigrationTest {
 Flyway flyway(PostgresTestDatabase db,String target){return Flyway.configure().dataSource(db.url(),db.username(),db.password()).locations("classpath:db/migration").target(target).baselineOnMigrate(false).cleanDisabled(true).load();}
 @Test void preservesLegacyQueuesTokensAndLogsAndSeedsDailyMaximum() throws Exception {
  try(var db=PostgresTestDatabase.start()){
   flyway(db,"4").migrate();
   try(var connection=DriverManager.getConnection(db.url(),db.username(),db.password());var sql=connection.createStatement()){
    sql.executeUpdate("INSERT INTO orders(id,total_amount,created_at,updated_at) VALUES (10,10,'2026-10-09 16:59:59','2026-10-09 16:59:59'),(11,20,'2026-10-09 17:00:00','2026-10-09 17:00:00')");
    sql.executeUpdate("INSERT INTO queue(id,queue_number,status,token_hash) VALUES (10,99,'WAITING','"+"a".repeat(64)+"'),(11,1000,'READY','"+"b".repeat(64)+"')");
    sql.executeUpdate("INSERT INTO notification_log(queue_id,channel,message,success,event_type) VALUES (11,'WEB_PUSH','Keep log',true,'READY')");
    var migration=flyway(db,"5");assertEquals(1,migration.migrate().migrationsExecuted);migration.validate();assertEquals(0,migration.migrate().migrationsExecuted);
    try(var result=sql.executeQuery("SELECT id,queue_number,status,token_hash,queue_date FROM queue ORDER BY id")){
     assertTrue(result.next());assertEquals(10,result.getLong(1));assertEquals(99,result.getInt(2));assertEquals("WAITING",result.getString(3));assertEquals("a".repeat(64),result.getString(4));assertEquals("2026-10-09",result.getString(5));
     assertTrue(result.next());assertEquals(11,result.getLong(1));assertEquals(1000,result.getInt(2));assertEquals("READY",result.getString(3));assertEquals("b".repeat(64),result.getString(4));assertEquals("2026-10-10",result.getString(5));assertFalse(result.next());
    }
    try(var result=sql.executeQuery("SELECT last_number FROM queue_daily_counter ORDER BY queue_date")){assertTrue(result.next());assertEquals(99,result.getInt(1));assertTrue(result.next());assertEquals(1000,result.getInt(1));assertFalse(result.next());}
    try(var result=sql.executeQuery("SELECT queue_id,message,success,event_type FROM notification_log")){assertTrue(result.next());assertEquals(11,result.getLong(1));assertEquals("Keep log",result.getString(2));assertTrue(result.getBoolean(3));assertEquals("READY",result.getString(4));}
    sql.executeUpdate("INSERT INTO orders(id,total_amount,created_at,updated_at) VALUES (12,10,'2026-10-09 17:01:00','2026-10-09 17:01:00'),(13,10,'2026-10-09 17:02:00','2026-10-09 17:02:00')");
    sql.executeUpdate("INSERT INTO queue(id,queue_number,status,queue_date) VALUES (12,99,'WAITING','2026-10-10')");
    assertThrows(SQLException.class,()->sql.executeUpdate("INSERT INTO queue(id,queue_number,status,queue_date) VALUES (13,99,'WAITING','2026-10-10')"));
   }
  }
 }
}
