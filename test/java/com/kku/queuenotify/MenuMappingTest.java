package com.kku.queuenotify;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.domain.entity.MenuItem;
import com.kku.queuenotify.mapper.MenuItemMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MenuMappingTest {
  @Test void storedPhotoSurvivesRenameAndUploadOverridesIt() {
    var menu=MenuItem.builder().id(8L).name("ข้าวกะเพราไก่")
        .imageKey("asset:basil-chicken-v2.webp").build();
    var mapper=new MenuItemMapper();menu.setName("test");
    assertEquals("/assets/food/basil-chicken-v2.webp",mapper.response(menu).imageUrl());
    menu.setImageKey("a".repeat(64));
    assertEquals("/api/v1/menu-items/8/image?v="+"a".repeat(64),mapper.response(menu).imageUrl());
    menu.setImageKey("asset:../../secret");assertNull(mapper.response(menu).imageUrl());
  }
  @Test void mapsCurrentMenuFieldsWithoutLoadingImageBytes() {
    var menu=MenuItem.builder().id(8L).name("Rice").category("FOOD")
        .price(new BigDecimal("12.50")).prepTimeMinutes(10).available(false).build();
    var response=new MenuItemMapper().response(menu);
    assertEquals(menu.getId(),response.id());assertEquals(menu.getName(),response.name());
    assertEquals(menu.getCategory(),response.category());assertEquals(menu.getPrice(),response.price());
    assertEquals(menu.getPrepTimeMinutes(),response.prepTimeMinutes());assertFalse(response.isAvailable());
    assertNull(response.imageUrl());
    menu.setImageKey("a".repeat(64));
    assertEquals("/api/v1/menu-items/8/image?v="+"a".repeat(64),new MenuItemMapper().response(menu).imageUrl());
    var json=new ObjectMapper().valueToTree(response);
    assertEquals(7,json.size());assertFalse(json.has("data"));assertFalse(json.has("imageKey"));
  }
}
