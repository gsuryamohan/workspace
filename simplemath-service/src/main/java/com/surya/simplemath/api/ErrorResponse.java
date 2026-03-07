package com.surya.simplemath.api;

import java.util.List;

public record ErrorResponse(
        String error,
        String message,
        List<FieldViolation> violations
) {
    public record FieldViolation(String field, String message) {}
}

