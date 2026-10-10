package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import com.kku.queuenotify.dto.request.MenuItemRequest;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.*;
import com.kku.queuenotify.service.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE)
class MenuImagePersistenceTest {
 static final PostgresTestDatabase DATABASE=start();
 static PostgresTestDatabase start(){try{return PostgresTestDatabase.start();}catch(Exception e){throw new IllegalStateException(e);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",DATABASE::url);r.add("spring.datasource.username",DATABASE::username);r.add("spring.datasource.password",DATABASE::password);}
 @AfterAll static void close() throws Exception {DATABASE.close();}
 @Autowired MenuImageService service;@Autowired MenuService menus;@Autowired MenuItemRepository items;@Autowired MenuImageRepository images;@Autowired PlatformTransactionManager manager;
 MenuItemRequest request(String name){return new MenuItemRequest(name,"FOOD",BigDecimal.TEN,10,true);}
 MockMultipartFile file(String format) throws Exception {return new MockMultipartFile("file","image."+format,"image/"+format,MenuImageFilesTest.image(format,2,2));}
 @BeforeEach void cleanFixtureImages(){new TransactionTemplate(manager).executeWithoutResult(s->{images.deleteAllInBatch();items.deleteAllInBatch();});}
 @Test void commitsImageReplacesItAndDeletesItWithUnusedMenu() throws Exception {
  var png=file("png");var first=service.saveMenu(null,request("Rice"),png);assertNotNull(first.imageUrl());assertEquals(1,images.count());assertArrayEquals(png.getBytes(),service.get(first.id()).data());
  var jpeg=file("jpeg");var second=service.saveMenu(first.id(),request("Changed"),jpeg);assertNotEquals(first.imageUrl(),second.imageUrl());assertEquals(1,images.count());assertEquals("image/jpeg",service.get(first.id()).contentType());assertArrayEquals(jpeg.getBytes(),service.get(first.id()).data());
  assertEquals(second.imageUrl(),menus.get(first.id(),true).imageUrl());menus.delete(first.id());assertEquals(0,images.count());assertFalse(items.existsById(first.id()));
 }
 @Test void invalidImageDoesNotCreateOrChangeMenu() throws Exception {
  var bad=new MockMultipartFile("file","bad.png","image/png",new byte[]{1,2});assertThrows(ApiException.class,()->service.saveMenu(null,request("Bad"),bad));assertEquals(0,items.count());assertEquals(0,images.count());
  var first=service.saveMenu(null,request("Keep"),file("png"));assertThrows(ApiException.class,()->service.saveMenu(first.id(),request("Changed"),bad));assertEquals("Keep",menus.get(first.id(),true).name());assertEquals(first.imageUrl(),menus.get(first.id(),true).imageUrl());
 }
 @Test void transactionRollbackRemovesBothMenuAndImage() throws Exception {
  var png=file("png");var id=new AtomicReference<Long>();
  assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->{id.set(service.saveMenu(null,request("Rollback"),png).id());images.flush();throw new IllegalStateException("rollback fixture");}));
  assertNotNull(id.get());assertFalse(items.existsById(id.get()));assertFalse(images.existsById(id.get()));
 }
}
