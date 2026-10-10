package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
class StaffFormTemplateTest {
 String render(String name,boolean csrf,boolean failed){
  var resolver=new ClassLoaderTemplateResolver();resolver.setPrefix("templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");resolver.setTemplateMode(TemplateMode.HTML);resolver.setCacheable(false);
  var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);
  var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setRequestURI("/staff/login");if(failed)request.setParameter("error","true");
  var exchange=JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse());
  var context=new WebContext(exchange);if(csrf)context.setVariable("_csrf",new TestToken());
  return engine.process(name,context);
 }
 @Test void loginBindsCsrfAndShowsErrorsWithoutExposingCredentials(){
  String ready=render("login",true,true);assertTrue(ready.contains("action=\"/staff/login\""));assertTrue(ready.contains("method=\"post\""));
  assertTrue(ready.contains("name=\"_csrf\""));assertTrue(ready.contains("value=\"test-token\""));assertTrue(ready.contains("ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง"));assertTrue(ready.contains("type=\"password\""));assertTrue(ready.contains("autocomplete=\"current-password\""));
  assertFalse(ready.contains("disabled="));assertFalse(ready.contains("ยังไม่พร้อมใช้งาน"));
  String pending=render("login",false,false);assertTrue(pending.contains("disabled="));assertTrue(pending.contains("ยังไม่พร้อมใช้งาน"));assertFalse(pending.contains("test-token"));assertFalse(pending.contains("ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง"));
 }
 @Test void logoutIsPostAndNeedsServerCsrfOnEveryStaffPage(){
  for(String page:new String[]{"staff","staff-menu","qr"}){
   String ready=render(page,true,false);String form=ready.substring(ready.indexOf("<form action=\"/staff/logout\""));form=form.substring(0,form.indexOf("</form>"));
   assertTrue(form.contains("method=\"post\""));assertTrue(form.contains("name=\"_csrf\""));assertTrue(form.contains("value=\"test-token\""));assertFalse(form.contains("disabled="));
   String pending=render(page,false,false);String disabled=pending.substring(pending.indexOf("<form action=\"/staff/logout\""));disabled=disabled.substring(0,disabled.indexOf("</form>"));assertTrue(disabled.contains("disabled="));assertFalse(disabled.contains("test-token"));
  }
 }
 // This token is a template fixture only; production tokens come from Spring Security.
 public static class TestToken {public String getParameterName(){return "_csrf";}public String getToken(){return "test-token";}}
}
