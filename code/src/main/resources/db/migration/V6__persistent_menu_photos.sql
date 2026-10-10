-- Persist the photo previously selected by the UI; preserve uploaded images.
UPDATE menu_item AS menu
SET image_key = 'asset:' || CASE trim(menu.name)
  WHEN 'ข้าวกะเพราไก่' THEN 'basil-chicken-v2.webp'
  WHEN 'ข้าวผัดไข่' THEN 'egg-fried-rice-v2.avif'
  WHEN 'ข้าวไก่ทอด' THEN 'crispy-chicken-rice-v2.jpg'
  WHEN 'ชาไทยเย็น' THEN 'thai-tea-v2.jpg'
  WHEN 'น้ำมะนาว' THEN 'limeade-v2.jpg'
  WHEN 'ผัดไทย' THEN 'pad-thai-v2.webp'
  ELSE CASE menu.category
    WHEN 'เครื่องดื่ม' THEN 'limeade-v2.jpg'
    WHEN 'เส้น' THEN 'pad-thai-v2.webp'
    ELSE 'egg-fried-rice-v2.avif'
  END
END
WHERE menu.image_key IS NULL
  AND NOT EXISTS (SELECT 1 FROM menu_item_image WHERE menu_item_id = menu.id);
