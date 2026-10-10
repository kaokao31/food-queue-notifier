package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.kku.queuenotify.controller.api.MenuController;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.dto.response.*;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
class MenuControllerTest {
 FakeMenus menus;StaticListableBeanFactory beans;MockMvc mvc;
 @BeforeEach void setup(){
  menus=new FakeMenus();beans=new StaticListableBeanFactory();
  mvc=MockMvcBuilders.standaloneSetup(new MenuController(menus,beans.getBeanProvider(OrderAccessService.class))).build();
 }
 void staff(boolean staff){beans.addBean("access",new OrderAccessService(){
  public boolean isStaff(){return staff;}
  public Queue locked(Long id,String token){throw new UnsupportedOperationException("Not used in menu tests");}
 });}
 @Test void publicReadWorksWithoutSecurityProviderAndCannotReadClosedMenus() throws Exception {
  mvc.perform(get("/api/v1/menu-items").param("includeUnavailable","true")).andExpect(status().isOk());assertFalse(menus.all);
  mvc.perform(get("/api/v1/menu-items/9")).andExpect(status().isNotFound());assertFalse(menus.all);
 }
 @Test void rejectsMutationWhenSecurityMissingOrCallerIsNotStaff() throws Exception {
  mvc.perform(delete("/api/v1/menu-items/9")).andExpect(status().isServiceUnavailable());
  staff(false);mvc.perform(delete("/api/v1/menu-items/9")).andExpect(status().isForbidden());assertEquals(0,menus.mutations);
 }
 @Test void validatesRequestAndReturnsCreatedLocationForStaff() throws Exception {
  staff(true);
  mvc.perform(post("/api/v1/menu-items").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());assertEquals(0,menus.mutations);
  mvc.perform(post("/api/v1/menu-items").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Rice\",\"price\":10,\"isAvailable\":true}")).andExpect(status().isCreated()).andExpect(header().string("Location","/api/v1/menu-items/4"));assertEquals(1,menus.mutations);
  mvc.perform(delete("/api/v1/menu-items/4")).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("history"));
 }
 // Test-only service and access doubles verify MVC routing and contract delegation.
 static class FakeMenus implements MenuService {
  boolean all;int mutations;
  public PageResponse<MenuItemResponse> list(Pageable page,boolean all){this.all=all;return new PageResponse<>(List.of(),0,20,0,0);}
  public MenuItemResponse get(Long id,boolean all){this.all=all;throw new ApiException(HttpStatus.NOT_FOUND,"missing");}
  public MenuItemResponse create(MenuItemRequest request){mutations++;return new MenuItemResponse(4L,request.name(),request.category(),request.price(),request.prepTimeMinutes(),request.isAvailable(),null);}
  public MenuItemResponse update(Long id,MenuItemRequest request){throw new UnsupportedOperationException();}
  public void delete(Long id){mutations++;throw new ApiException(HttpStatus.CONFLICT,"history");}
 }
}
