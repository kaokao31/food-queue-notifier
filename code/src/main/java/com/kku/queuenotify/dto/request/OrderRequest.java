package com.kku.queuenotify.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record OrderRequest(@NotNull @Size(min = 1, max = 50) List<@NotNull @Valid Item> items) {
  public record Item(
      @NotNull @Positive Long menuItemId, @NotNull @Min(1) @Max(99) Integer quantity) {}
}
