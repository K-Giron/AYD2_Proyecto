package com.logiexpress.carrier.shared;

import java.time.Instant;

public record ApiError(String code, String message, Instant timestamp, String path) {
}
