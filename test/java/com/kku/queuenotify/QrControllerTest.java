package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.kku.queuenotify.controller.QrController;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
class QrControllerTest {
 @Test void deniesPageAndPngWithoutStaffProvider() throws Exception {
  var beans=new StaticListableBeanFactory();var mvc=MockMvcBuilders.standaloneSetup(new QrController(url->{fail("must not generate before staff access");return null;},beans.getBeanProvider(OrderAccessService.class))).build();
  mvc.perform(get("/staff/qr")).andExpect(status().isServiceUnavailable());mvc.perform(get("/staff/qr.png").param("url","https://example.com/")).andExpect(status().isServiceUnavailable());
  beans.addBean("access",access(false));mvc.perform(get("/staff/qr.png").param("url","https://example.com/")).andExpect(status().isForbidden());
 }
 @Test void staffReceivesPngWithoutCache() throws Exception {
  var beans=new StaticListableBeanFactory();beans.addBean("access",access(true));var mvc=MockMvcBuilders.standaloneSetup(new QrController(url->new byte[]{1,2},beans.getBeanProvider(OrderAccessService.class))).setSingleView(new org.springframework.web.servlet.view.InternalResourceView("/templates/qr.html")).build();
  mvc.perform(get("/staff/qr")).andExpect(status().isOk()).andExpect(view().name("qr"));mvc.perform(get("/staff/qr.png").param("url","https://example.com/")).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(new byte[]{1,2})).andExpect(header().string("Cache-Control","no-store"));
 }
 // Staff fixtures are used only to verify controller delegation.
 OrderAccessService access(boolean staff){return new OrderAccessService(){public boolean isStaff(){return staff;}public Queue locked(Long id,String token){throw new UnsupportedOperationException();}};}
}
