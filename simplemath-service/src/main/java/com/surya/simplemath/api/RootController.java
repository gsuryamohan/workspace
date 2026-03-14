package com.surya.simplemath.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "SimpleMath API",
                "status", "UP",
                "endpoints", Map.of(
                        "add", "POST /api/v1/math/add",
                        "subtract", "POST /api/v1/math/subtract",
                        "multiply", "POST /api/v1/math/multiply",
                        "divide", "POST /api/v1/math/divide"
                )
        );
    }
}
