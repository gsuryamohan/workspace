package com.surya.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Random;

public class SuryaRestApiClient2 {

    // SimpleMathService MULTIPLY endpoint (EC2 surya-service)
    private static final String MULTIPLY_ENDPOINT =
            "http://16.145.70.106:8080/api/v1/math/multiply";

    public static void main(String[] args) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        Random random = new Random();
        int iterations = 1000;

        for (int i = 1; i <= iterations; i++) {
            int a = random.nextInt(1_000) + 1; // 1..1000
            int b = random.nextInt(1_000) + 1; // 1..1000

            String requestBody = String.format("{\"a\":%d,\"b\":%d}", a, b);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MULTIPLY_ENDPOINT))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            try {
                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.printf(
                        "#%04d  a=%d, b=%d  HTTP %d  body=%s%n",
                        i, a, b, response.statusCode(), response.body()
                );
            } catch (Exception e) {
                System.err.printf(
                        "#%04d  a=%d, b=%d  ERROR: %s%n",
                        i, a, b, e.getMessage()
                );
            }
        }
    }
}
