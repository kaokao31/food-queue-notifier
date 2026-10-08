package com.kku.queuenotify.common;

import com.kku.queuenotify.exception.ApiException;
import java.util.Set;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;

public final class PageRequests {
  private PageRequests() {}

  public static Pageable of(int page, int size, String sort, Set<String> allowed) {
    var parts = sort == null ? new String[0] : sort.split(",", -1);
    if (page < 0
        || size < 1
        || size > 100
        || parts.length != 2
        || !allowed.contains(parts[0])
        || !Set.of("asc", "desc").contains(parts[1]))
      throw new ApiException(HttpStatus.BAD_REQUEST, "page, size หรือ sort ไม่ถูกต้อง");
    var s = Sort.by(parts[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, parts[0]);
    if (!parts[0].equals("id")) s = s.and(Sort.by("id"));
    return PageRequest.of(page, size, s);
  }
}
