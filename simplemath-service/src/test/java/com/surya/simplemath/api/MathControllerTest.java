package com.surya.simplemath.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MathControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void add_returnsResult() throws Exception {
        mockMvc.perform(post("/api/v1/math/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":5,\"b\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("Add"))
                .andExpect(jsonPath("$.result").value(8.0));
    }

    @Test
    void subtract_returnsResult() throws Exception {
        mockMvc.perform(post("/api/v1/math/subtract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":5,\"b\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("Subtract"))
                .andExpect(jsonPath("$.result").value(2.0));
    }

    @Test
    void multiply_returnsResult() throws Exception {
        mockMvc.perform(post("/api/v1/math/multiply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":5,\"b\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("Multiply"))
                .andExpect(jsonPath("$.result").value(15.0));
    }

    @Test
    void divide_returnsResult() throws Exception {
        mockMvc.perform(post("/api/v1/math/divide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":10,\"b\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("Divide"))
                .andExpect(jsonPath("$.result").value(5.0));
    }

    @Test
    void validation_negativeValue_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/math/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":-1,\"b\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.violations[0].field").value("a"));
    }

    @Test
    void validation_missingValue_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/math/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}

