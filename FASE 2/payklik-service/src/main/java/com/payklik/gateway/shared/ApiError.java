package com.payklik.gateway.shared;

import java.time.Instant;

public record ApiError(String code, String message, Instant timestamp, String path) {
}
