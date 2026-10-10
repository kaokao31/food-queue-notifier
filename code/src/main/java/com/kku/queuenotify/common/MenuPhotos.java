package com.kku.queuenotify.common;

import java.util.Map;

/** Assign bundled photos once, independently of later name/category edits. */
public final class MenuPhotos {
  private static final String PREFIX = "asset:";
  private static final Map<String,String> PHOTOS = Map.of(
      "ข้าวกะเพราไก่", "basil-chicken-v2.webp", "ข้าวผัดไข่", "egg-fried-rice-v2.avif",
      "ข้าวไก่ทอด", "crispy-chicken-rice-v2.jpg", "ชาไทยเย็น", "thai-tea-v2.jpg",
      "น้ำมะนาว", "limeade-v2.jpg", "ผัดไทย", "pad-thai-v2.webp");
  private MenuPhotos() {}
  public static String initialKey(String name,String category) {
    String fallback="เครื่องดื่ม".equals(category)?"limeade-v2.jpg"
        :"เส้น".equals(category)?"pad-thai-v2.webp":"egg-fried-rice-v2.avif";
    return PREFIX+PHOTOS.getOrDefault(name==null?"":name.trim(),fallback);
  }
  public static String imageUrl(Long id,String key) {
    if(key==null)return null;
    if(key.startsWith(PREFIX)) {
      String file=key.substring(PREFIX.length());
      return PHOTOS.containsValue(file)?"/assets/food/"+file:null;
    }
    return "/api/v1/menu-items/"+id+"/image?v="+key;
  }
}
