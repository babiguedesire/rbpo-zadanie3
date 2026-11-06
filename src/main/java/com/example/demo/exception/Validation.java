package com.example.demo.exception;

import java.math.BigDecimal;

public final class Validation {
    private Validation() {}

    public static void required(Object value, String field) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            throw new BadRequestException("Field '" + field + "' is required");
        }
    }

    public static void nonNegative(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) {
            throw new BadRequestException("Field '" + field + "' must not be negative");
        }
    }

    public static void nonNegative(Integer value, String field) {
        if (value != null && value < 0) {
            throw new BadRequestException("Field '" + field + "' must not be negative");
        }
    }
}
