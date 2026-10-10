package com.kku.queuenotify.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.QrService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Validates the public menu URL and generates its QR without external requests. */
@Service
public class QrServiceImpl implements QrService {
  @Override
  public byte[] generateMenuQr(String url) throws Exception {
    if(url==null || url.isBlank() || url.length()>1000)throw invalid();
    URI uri;
    try {
      uri = URI.create(url);
    } catch (IllegalArgumentException ex) {
      throw invalid();
    }
    if (url.length() > 1000
        || !"https".equalsIgnoreCase(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getRawQuery() != null
        || uri.getRawFragment() != null
        || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))) throw invalid();
    var matrix =
        new QRCodeWriter()
            .encode(
                url,
                BarcodeFormat.QR_CODE,
                480,
                480,
                Map.of(EncodeHintType.CHARACTER_SET, "UTF-8", EncodeHintType.MARGIN, 4));
    var image = new BufferedImage(480, 480, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < 480; y++)
      for (int x = 0; x < 480; x++) image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xffffff);
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);
    return bytes.toByteArray();
  }

  private ApiException invalid() {
    return new ApiException(
        HttpStatus.BAD_REQUEST,
        "ใช้ URL HTTPS ของหน้าเมนู เช่น https://ชื่อเว็บ.onrender.com/"
            + " โดยไม่ใส่รหัสหรือพารามิเตอร์");
  }
}
