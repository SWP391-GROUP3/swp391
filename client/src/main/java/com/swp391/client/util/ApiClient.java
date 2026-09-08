package com.swp391.client.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Goi REST API cua server.
 *
 * <p>Client goi API tu phia Java (server-side), KHONG goi bang JavaScript
 * trong JSP. Nho vay khong dinh CORS, va token khong lo ra trinh duyet.
 *
 * <p>Khi lam xong dang nhap, lay JWT tu {@code HttpSession} roi truyen vao
 * tham so {@code token} cua cac ham duoi day.
 */
public final class ApiClient {

    /** Doi thanh dia chi server that khi deploy. */
    private static final String BASE_URL = "http://localhost:8080";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper JSON = new ObjectMapper();

    private ApiClient() {
    }

    /** GET khong can dang nhap. */
    public static JsonNode get(String duongDan) throws IOException, InterruptedException {
        return get(duongDan, null);
    }

    /** GET co kem JWT. Truyen {@code token = null} neu endpoint khong can auth. */
    public static JsonNode get(String duongDan, String token)
            throws IOException, InterruptedException {

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + duongDan))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET();

        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> res = HTTP.send(b.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (res.statusCode() >= 400) {
            throw new IOException("Server tra ve loi " + res.statusCode() + ": " + res.body());
        }
        return JSON.readTree(res.body());
    }

    /** POST body dang JSON. */
    public static JsonNode post(String duongDan, Object body, String token)
            throws IOException, InterruptedException {

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + duongDan))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        JSON.writeValueAsString(body), StandardCharsets.UTF_8));

        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> res = HTTP.send(b.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (res.statusCode() >= 400) {
            throw new IOException("Server tra ve loi " + res.statusCode() + ": " + res.body());
        }
        return JSON.readTree(res.body());
    }
}
