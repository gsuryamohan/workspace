package com.surya.simplemath.service;

import org.springframework.stereotype.Service;

@Service
public class SimpleMathService {
    public double add(int a, int b) {
        return (double) a + b;
    }

    public double subtract(int a, int b) {
        return (double) a - b;
    }

    public double multiply(int a, int b) {
        return (double) a * b;
    }

    public double divide(int a, int b) {
        return (double) a / b;
    }
}

