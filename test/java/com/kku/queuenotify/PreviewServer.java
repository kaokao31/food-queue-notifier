package com.kku.queuenotify;

import java.util.Map;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

/** Local UI preview only; H2 is not used as migration or PostgreSQL evidence. */
public class PreviewServer {
  public static void main(String[] args) {
    var app =
        new SpringApplicationBuilder(QueueNotifyApplication.class)
            .properties(
                Map.of(
                    "server.port",
                    "18080",
                    "spring.datasource.url",
                    "jdbc:h2:mem:preview;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                    "spring.datasource.username",
                    "sa",
                    "spring.datasource.password",
                    "",
                    "spring.flyway.enabled",
                    "false",
                    "spring.jpa.hibernate.ddl-auto",
                    "create-drop",
                    "staff.password",
                    "preview-local-only-password",
                    "notification.mode",
                    "console"))
            .run(
                "--server.port=18080",
                "--spring.datasource.url=jdbc:h2:mem:preview;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "--spring.datasource.username=sa",
                "--spring.datasource.password=",
                "--spring.flyway.enabled=false",
                "--spring.jpa.hibernate.ddl-auto=create-drop");
    var jdbc = app.getBean(JdbcTemplate.class);
    jdbc.execute("CREATE SEQUENCE queue_number_seq");
    jdbc.update(
        "insert into menu_item(name,category,price,prep_time_minutes,is_available)"
            + " values('ข้าวกะเพราไก่','อาหารจานเดียว',55,10,true),('ข้าวผัดไข่','อาหารจานเดียว',45,8,true),('ข้าวไก่ทอด','อาหารจานเดียว',60,12,true),('ผัดไทย','เส้น',65,12,true),('ชาไทยเย็น','เครื่องดื่ม',35,3,true),('น้ำมะนาว','เครื่องดื่ม',30,3,true)");
  }
}
