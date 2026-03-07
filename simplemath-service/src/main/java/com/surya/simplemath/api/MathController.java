package com.surya.simplemath.api;

import com.surya.simplemath.service.SimpleMathService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/math")
public class MathController {
    private final SimpleMathService simpleMathService;

    public MathController(SimpleMathService simpleMathService) {
        this.simpleMathService = simpleMathService;
    }

    @PostMapping("/add")
    public MathResponse add(@Valid @RequestBody MathRequest request) {
        return new MathResponse("Add", request.a(), request.b(), simpleMathService.add(request.a(), request.b()));
    }

    @PostMapping("/subtract")
    public MathResponse subtract(@Valid @RequestBody MathRequest request) {
        return new MathResponse("Subtract", request.a(), request.b(), simpleMathService.subtract(request.a(), request.b()));
    }

    @PostMapping("/multiply")
    public MathResponse multiply(@Valid @RequestBody MathRequest request) {
        return new MathResponse("Multiply", request.a(), request.b(), simpleMathService.multiply(request.a(), request.b()));
    }

    @PostMapping("/divide")
    public MathResponse divide(@Valid @RequestBody MathRequest request) {
        return new MathResponse("Divide", request.a(), request.b(), simpleMathService.divide(request.a(), request.b()));
    }
}

