package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kku.queuenotify.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
class MenuImageIntegrationTest extends IntegrationTestSupport {
 @Autowired ObjectMapper json;@Autowired MenuItemRepository menus;@Autowired MenuImageRepository images;
 MockMultipartFile data(String name){return new MockMultipartFile("menu","","application/json",("{\"name\":\""+name+"\",\"category\":\"FOOD\",\"price\":10,\"prepTimeMinutes\":10,\"isAvailable\":true}").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 @Test void multipartMvcStoresActualPngAndInvalidReplacementPreservesBothRecords() throws Exception {
  access.staff=true;byte[] png=MenuImageFilesTest.image("png",3,2);var file=new MockMultipartFile("file","rice.png","image/png",png);
  var response=mvc.perform(multipart("/api/v1/menu-items/with-image").file(data("Rice")).file(file)).andExpect(status().isCreated()).andReturn();long id=json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
  var first=mvc.perform(get("/api/v1/menu-items/"+id+"/image")).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG)).andExpect(content().bytes(png)).andReturn();String etag=first.getResponse().getHeader("ETag");assertNotNull(etag);
  mvc.perform(multipart("/api/v1/menu-items/"+id+"/with-image").file(data("Bad replacement")).file(new MockMultipartFile("file","bad.png","image/png",new byte[]{1,2})).with(r->{r.setMethod("PUT");return r;})).andExpect(status().isBadRequest());assertEquals("Rice",menus.findById(id).orElseThrow().getName());assertEquals(1,images.count());
  mvc.perform(get("/api/v1/menu-items/"+id+"/image")).andExpect(header().string("ETag",etag)).andExpect(content().bytes(png));
  access.staff=false;mvc.perform(get("/api/v1/menu-items/"+id+"/image")).andExpect(status().isOk());access.staff=true;mvc.perform(delete("/api/v1/menu-items/"+id)).andExpect(status().isNoContent());assertEquals(0,images.count());
 }
}
