package com.kku.queuenotify;
import static org.junit.jupiter.api.Assertions.*;
import com.kku.queuenotify.service.impl.QrServiceImpl;
import com.kku.queuenotify.exception.ApiException;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
class QrServiceTest {
 @Test void generates480PixelPngThatDecodesToExactUrl() throws Exception {
  var url="https://example.com/";var image=ImageIO.read(new ByteArrayInputStream(new QrServiceImpl().generateMenuQr(url)));assertNotNull(image);assertEquals(480,image.getWidth());assertEquals(480,image.getHeight());
  var source=new RGBLuminanceSource(480,480,image.getRGB(0,0,480,480,null,0,480));assertEquals(url,new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(source))).getText());
 }
 @Test void rejectsNonHttpsCredentialsPathsParametersAndMissingUrl(){
  for(String url:new String[]{null,"","not a URL","http://example.com/","https://user:pass@example.com/","https://example.com/menu","https://example.com/?token=x","https://example.com/#x","https://example.com/"+"a".repeat(1001)})assertThrows(ApiException.class,()->new QrServiceImpl().generateMenuQr(url));
 }
}
