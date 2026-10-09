package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import com.kku.queuenotify.common.MenuImageFiles;
import com.kku.queuenotify.exception.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
class MenuImageFilesTest {
 static byte[] image(String format,int width,int height) throws Exception {
  var out=new ByteArrayOutputStream();assertTrue(ImageIO.write(new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB),format,out));return out.toByteArray();
 }
 @Test void detectsRealPngAndJpegDespiteFilenameAndDeclaredMime() throws Exception {
  for(String format:new String[]{"png","jpeg"}){
   var bytes=image(format,2,2);var result=MenuImageFiles.validate(new MockMultipartFile("file","wrong.txt","text/plain",bytes));
   assertEquals("image/"+format,result.contentType());assertArrayEquals(bytes,result.data());
   assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),result.key());
  }
 }
 @Test void rejectsEmptyForgedCorruptAndOversizedFiles(){
  assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ApiException.class,()->MenuImageFiles.validate(null)).getStatus());
  for(byte[] bytes:new byte[][]{new byte[0],"not an image".getBytes(),new byte[]{(byte)255,(byte)216,(byte)255}})
   assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ApiException.class,()->MenuImageFiles.validate(new MockMultipartFile("file","fake.png","image/png",bytes))).getStatus());
  assertEquals(HttpStatus.PAYLOAD_TOO_LARGE,assertThrows(ApiException.class,()->MenuImageFiles.validate(new MockMultipartFile("file",new byte[(int)MenuImageFiles.MAX_BYTES+1]))).getStatus());
 }
 @Test void rejectsTooWideImagesBeforeDecodingPixels() throws Exception {
  var bytes=image("png",6001,1);assertEquals(HttpStatus.BAD_REQUEST,assertThrows(ApiException.class,()->MenuImageFiles.validate(new MockMultipartFile("file",bytes))).getStatus());
 }
}
