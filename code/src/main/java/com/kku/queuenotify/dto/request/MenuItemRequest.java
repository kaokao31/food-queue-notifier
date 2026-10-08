package com.kku.queuenotify.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record MenuItemRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 50) String category,
    @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
    @Min(0) @Max(240) Integer prepTimeMinutes,
    @NotNull Boolean isAvailable) {}
