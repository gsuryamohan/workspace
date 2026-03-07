package com.surya.simplemath.api;

public record MathResponse(
        String operation,
        int a,
        int b,
        double result
) {
}

