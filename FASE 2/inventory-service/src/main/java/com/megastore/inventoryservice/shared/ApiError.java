package com.megastore.inventoryservice.shared;

import java.time.Instant;

public record ApiError(String code, String message, Instant timestamp, String path) {
}
