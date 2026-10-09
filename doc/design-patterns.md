# Design patterns

ปัจจุบันมี Service layer, Repository และ DTO Mapper แยกหน้าที่ รวมทั้ง dependency injection ผ่าน interface สำหรับ QueueTokenGenerator/OrderAccessService เพื่อให้ module tests แทน dependency ได้

State สำหรับสถานะคิวเป็นงาน C ที่รอรวม: enum QueueStatus ที่มีอยู่ยังไม่ใช่ implementation ของ State pattern

Observer สำหรับเหตุการณ์สถานะ และ Strategy สำหรับช่องทางส่งแจ้งเตือนเป็นงาน T ที่รอรวม: entity/repository ของ Subscription และ Log เป็นโครงข้อมูล ยังไม่ใช่การส่งแจ้งเตือน

หลังรวม implementation ให้เพิ่มชื่อ class และหลักฐานการใช้งานจริงใน diagram และ tests
