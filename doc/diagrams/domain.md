# Domain model

อ้างอิง `develop` baseline `64b3ce7` วันที่ 10 ตุลาคม 2026 แสดง object relationships ปัจจุบัน โดย legacy SQL columns แยกอธิบายด้านล่าง

```mermaid
classDiagram
  Order "1" *-- "0..*" OrderItem : item snapshots
  Order "1" *-- "0..1" Queue : shared ID
  OrderItem "0..*" --> "1" MenuItem : references catalog
  Order "0..*" --> "0..1" PushSubscription : optional attachment
  MenuItem "1" -- "0..1" MenuImage : shared ID
  Queue "1" -- "0..*" NotificationLog : delivery history
  class DailyQueueCounter {
    LocalDate queueDate
    Integer lastNumber
  }
  class Queue {
    Long id
    Integer queueNumber
    LocalDate queueDate
    QueueStatus status
    String tokenHash
  }
  class MenuItem {
    Long id
    String name
    String imageKey
  }
  class OrderItem {
    Integer quantity
    BigDecimal unitPrice
    BigDecimal subtotal
    String menuItemName
  }
```

MenuImage map ไป menu_item_image ส่วน MenuItem.imageKey แทน built-in asset หรือ key ของรูปอัปโหลด OrderItem เก็บชื่อและราคาขณะสั่งโดยไม่เปลี่ยนตาม catalog ภายหลัง Order/Queue ใช้ shared primary key ผ่าน MapsId

DailyQueueCounter มี primary key เป็นวันที่ Bangkok ใช้จัดเลขคิวรายวันโดยไม่ผูก FK กับ Queue โดยตรง Customer/NotificationPreference และ orders.customer_id/status ยังอยู่ใน SQL เพื่อเก็บข้อมูล legacy แต่ Order entity ปัจจุบันไม่ใช้ customer association/status ดังกล่าว สถานะที่ใช้งานอยู่ใน Queue

State, Observer และ Strategy เป็นพฤติกรรมใน services ไม่ใช่ความสัมพันธ์ entity ใน diagram นี้ ดู [Class and pattern diagrams](../class-diagram-patterns.md), [State](state.md) และ [ER](er.md)
