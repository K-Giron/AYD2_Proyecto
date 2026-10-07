package com.megastore.storeservice.shared;

import java.time.Instant;

public record ApiError(String code, String message, Instant timestamp, String path) {
}
