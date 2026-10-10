package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.kku.queuenotify.controller.api.MenuImageController;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.dto.response.*;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
class MenuImageControllerTest {
 StaticListableBeanFactory beans;MockMvc mvc;int mutations,reads;boolean closed;
 @BeforeEach void setup(){
  beans=new StaticListableBeanFactory();mutations=0;reads=0;closed=false;
  // Test-only service doubles isolate routing, validation and access delegation.
  MenuImageService images=new MenuImageService(){
   public MenuItemResponse saveMenu(Long id,MenuItemRequest r,MultipartFile f){mutations++;return response(id==null?4L:id);}
   public MenuImageContent get(Long id){reads++;return new MenuImageContent(new byte[]{1,2},"image/png","a".repeat(64));}
  };
  MenuService menus=new MenuService(){
   public MenuItemResponse get(Long id,boolean all){if(closed&&!all)throw new ApiException(HttpStatus.NOT_FOUND,"missing");return response(id);}
   public PageResponse<MenuItemResponse> list(Pageable p,boolean all){throw new UnsupportedOperationException();}
   public MenuItemResponse create(MenuItemRequest r){throw new UnsupportedOperationException();}
   public MenuItemResponse update(Long id,MenuItemRequest r){throw new UnsupportedOperationException();}
   public void delete(Long id){throw new UnsupportedOperationException();}
  };
  mvc=MockMvcBuilders.standaloneSetup(new MenuImageController(images,menus,beans.getBeanProvider(OrderAccessService.class))).build();
 }
 MenuItemResponse response(Long id){return new MenuItemResponse(id,"Rice","FOOD",BigDecimal.TEN,10,true,"/api/v1/menu-items/"+id+"/image");}
 void staff(boolean ready){beans.addBean("access",new OrderAccessService(){public boolean isStaff(){return ready;}public Queue locked(Long id,String token){throw new UnsupportedOperationException();}});}
 org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder upload(String url,String json){return multipart(url).file(new MockMultipartFile("menu","","application/json",json.getBytes(java.nio.charset.StandardCharsets.UTF_8))).file(new MockMultipartFile("file","x.png","image/png",new byte[]{1}));}
 String valid(){return "{\"name\":\"Rice\",\"price\":10,\"isAvailable\":true}";}
 @Test void rejectsWritesUntilRealStaffAccessIsProvided() throws Exception {
  mvc.perform(upload("/api/v1/menu-items/with-image",valid())).andExpect(status().isServiceUnavailable());staff(false);
  mvc.perform(upload("/api/v1/menu-items/4/with-image",valid()).with(r->{r.setMethod("PUT");return r;})).andExpect(status().isForbidden());assertEquals(0,mutations);
 }
 @Test void validatesPartsAndRoutesCreateAndUpdateForStaff() throws Exception {
  staff(true);mvc.perform(upload("/api/v1/menu-items/with-image","{}")).andExpect(status().isBadRequest());assertEquals(0,mutations);
  mvc.perform(upload("/api/v1/menu-items/with-image",valid())).andExpect(status().isCreated()).andExpect(header().string("Location","/api/v1/menu-items/4"));
  mvc.perform(upload("/api/v1/menu-items/8/with-image",valid()).with(r->{r.setMethod("PUT");return r;})).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(8));assertEquals(2,mutations);
 }
 @Test void servesBytesWithRevalidationAndHidesClosedMenuImages() throws Exception {
  mvc.perform(get("/api/v1/menu-items/4/image")).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(new byte[]{1,2})).andExpect(header().string("Cache-Control","no-cache")).andExpect(header().string("ETag","\""+"a".repeat(64)+"\""));
  closed=true;mvc.perform(get("/api/v1/menu-items/4/image")).andExpect(status().isNotFound());assertEquals(1,reads);
  staff(true);mvc.perform(get("/api/v1/menu-items/4/image")).andExpect(status().isOk());assertEquals(2,reads);
 }
}
