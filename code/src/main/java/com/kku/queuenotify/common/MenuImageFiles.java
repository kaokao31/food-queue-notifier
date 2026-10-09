package com.kku.queuenotify.common;

import com.kku.queuenotify.dto.response.MenuImageContent;
import com.kku.queuenotify.exception.ApiException;
import java.io.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

public final class MenuImageFiles {
  public static final long MAX_BYTES = 2L * 1024 * 1024;

  private MenuImageFiles() {}

  public static MenuImageContent validate(MultipartFile file) {
    if (file == null || file.isEmpty()) throw bad("กรุณาเลือกไฟล์ภาพ JPG หรือ PNG");
    if (file.getSize() > MAX_BYTES)
      throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "รูปภาพต้องมีขนาดไม่เกิน 2 MB");
    try {
      byte[] data = file.getBytes();
      String type;
      if (data.length >= 3
          && (data[0] & 255) == 255
          && (data[1] & 255) == 216
          && (data[2] & 255) == 255) type = "image/jpeg";
      else if (data.length >= 8
          && data[0] == (byte) 137
          && data[1] == 80
          && data[2] == 78
          && data[3] == 71
          && data[4] == 13
          && data[5] == 10
          && data[6] == 26
          && data[7] == 10) type = "image/png";
      else throw bad("รองรับเฉพาะไฟล์ภาพ JPG และ PNG");
      try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
        var readers = ImageIO.getImageReaders(input);
        if (!readers.hasNext()) throw bad("ไฟล์ภาพไม่ถูกต้อง");
        var reader = readers.next();
        try {
          reader.setInput(input, true, true);
          int width = reader.getWidth(0), height = reader.getHeight(0);
          if (width < 1
              || height < 1
              || width > 6000
              || height > 6000
              || (long) width * height > 12000000)
            throw bad("ภาพต้องไม่เกิน 6000 พิกเซลต่อด้าน และ 12 ล้านพิกเซล");
          if (reader.read(0) == null) throw bad("ไฟล์ภาพไม่ถูกต้อง");
        } finally {
          reader.dispose();
        }
      }
      return new MenuImageContent(
          data, type, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)));
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw bad("ไฟล์ภาพเสียหายหรืออ่านไม่ได้");
    }
  }

  private static ApiException bad(String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, message);
  }
}
