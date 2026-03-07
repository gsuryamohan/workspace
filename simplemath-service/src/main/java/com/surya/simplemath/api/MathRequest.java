package com.surya.simplemath.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MathRequest(
        @NotNull(message = "a is required")
        @Positive(message = "a must be a positive integer")
        Integer a,

        @NotNull(message = "b is required")
        @Positive(message = "b must be a positive integer")
        Integer b
) {
}

