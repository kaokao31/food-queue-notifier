-- Seed only when the menu is empty. No personal information is inserted.
INSERT INTO menu_item(name,category,price,prep_time_minutes,is_available)
SELECT n,c,p,t,true FROM (VALUES
 ('ข้าวกะเพราไก่','อาหารจานเดียว',55.00,10),
 ('ข้าวผัดไข่','อาหารจานเดียว',45.00,8),
 ('ข้าวไก่ทอด','อาหารจานเดียว',60.00,12),
 ('ผัดไทย','เส้น',65.00,12),
 ('ชาไทยเย็น','เครื่องดื่ม',35.00,3),
 ('น้ำมะนาว','เครื่องดื่ม',30.00,3)
) AS seed(n,c,p,t) WHERE NOT EXISTS(SELECT 1 FROM menu_item);
