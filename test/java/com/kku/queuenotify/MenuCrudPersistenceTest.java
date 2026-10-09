package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import com.kku.queuenotify.domain.entity.*;
import com.kku.queuenotify.domain.entity.Order;
import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.*;
import com.kku.queuenotify.service.MenuService;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE)
@Transactional
class MenuCrudPersistenceTest {
 static final PostgresTestDatabase DATABASE=start();
 static PostgresTestDatabase start(){try{return PostgresTestDatabase.start();}catch(Exception e){throw new IllegalStateException(e);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("spring.datasource.url",DATABASE::url);registry.add("spring.datasource.username",DATABASE::username);registry.add("spring.datasource.password",DATABASE::password);}
 @AfterAll static void close() throws Exception {DATABASE.close();}
 @Autowired MenuService service;@Autowired MenuItemRepository menus;@Autowired OrderItemRepository lines;@Autowired OrderRepository orders;
 MenuItemRequest request(String name,boolean available){return new MenuItemRequest(name,"FOOD",new BigDecimal("25.00"),10,available);}
 @BeforeEach void isolateCatalogFixtures(){menus.deleteAllInBatch();}
 @Test void createsUpdatesFiltersAndDeletesUnusedMenu(){
  var menu=service.create(request(" Rice ",true));assertEquals("Rice",menu.name());
  service.update(menu.id(),request("Changed",false));assertEquals(0,service.list(PageRequest.of(0,20),false).totalElements());assertEquals(1,service.list(PageRequest.of(0,20),true).totalElements());
  assertEquals(HttpStatus.NOT_FOUND,assertThrows(ApiException.class,()->service.get(menu.id(),false)).getStatus());
  service.delete(menu.id());menus.flush();assertFalse(menus.existsById(menu.id()));
 }
 @Test void protectsMenuHistoryAndAllowsClosingSales(){
  var menu=service.create(request("Rice",true));var entity=menus.findById(menu.id()).orElseThrow();
  var order=new Order();order.setTotalAmount(new BigDecimal("25.00"));orders.saveAndFlush(order);
  lines.saveAndFlush(OrderItem.builder().order(order).menuItem(entity).menuItemName("Rice").quantity(1).unitPrice(new BigDecimal("25.00")).subtotal(new BigDecimal("25.00")).build());
  assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,()->service.delete(menu.id())).getStatus());assertTrue(menus.existsById(menu.id()));
  service.update(menu.id(),request("Rice",false));assertFalse(service.get(menu.id(),true).isAvailable());assertEquals("Rice",lines.findAll().get(0).getMenuItemName());
 }
}
