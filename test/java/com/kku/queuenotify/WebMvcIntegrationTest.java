package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.kku.queuenotify.controller.*;
import com.kku.queuenotify.service.*;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceView;
/** MVC mapping plus actual Thymeleaf rendering; no production security provider. */
class WebMvcIntegrationTest {
 @Test void customerAndStaffRoutesResolveToTheirTemplates() throws Exception {
  var mvc=MockMvcBuilders.standaloneSetup(new WebController()).setSingleView(new InternalResourceView("/templates/test.html")).build();
  for(var entry:Map.of("/","menu","/queue/42","queue","/staff/login","login","/staff","staff","/staff/menu","staff-menu").entrySet())mvc.perform(get(entry.getKey())).andExpect(status().isOk()).andExpect(view().name(entry.getValue()));
 }
 @Test void renderedPagesHaveExistingLocalAssetsAndOrderControls() throws Exception {
  var fixture=new StaffFormTemplateTest();
  for(String page:new String[]{"menu","queue","login","staff","staff-menu","qr"}){
   var html=fixture.render(page,false,false);assertTrue(html.contains("lang=\"th\""),page);
   var assets=Pattern.compile("(?:src|href)=\"(/assets/[^\"]+)\"").matcher(html);int count=0;
   while(assets.find()){try(var stream=getClass().getResourceAsStream("/static"+assets.group(1))){assertNotNull(stream,assets.group(1));count++;}}
   assertTrue(count>0,page);
  }
  var menu=fixture.render("menu",false,false);for(String id:new String[]{"queue-history-dialog","queue-history-list"})assertTrue(menu.contains("id=\""+id+"\""));
  assertTrue(fixture.render("queue",false,false).contains("/assets/queue-ui.js"));
 }
 @Test void staffFormsRemainDisabledWithoutCsrfAndQrFailsWithoutAccess() throws Exception {
  var fixture=new StaffFormTemplateTest();fixture.logoutIsPostAndNeedsServerCsrfOnEveryStaffPage();fixture.loginBindsCsrfAndShowsErrorsWithoutExposingCredentials();
  var beans=new StaticListableBeanFactory();var mvc=MockMvcBuilders.standaloneSetup(new QrController(url->{fail("must not generate QR without access");return null;},beans.getBeanProvider(OrderAccessService.class))).build();
  mvc.perform(get("/staff/qr")).andExpect(status().isServiceUnavailable());
 }
}
