package com.bloodline.madibets.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Owner: Kieran (B-series). Small helpers shared by the API handlers. */
final class Http {

    private Http() {}

    /** Permissive CORS so the SPA also works when opened from file:// or another
     *  port during development. Same-origin serving makes these headers inert. */
    static void cors(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type");
    }

    /** Serializes body to JSON and completes the exchange. */
    static void send(HttpExchange ex, int status, Object body) throws IOException {
        cors(ex);
        byte[] bytes = JsonUtil.write(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static Map<String, Object> readJsonBody(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body.isBlank()) {
            throw new IllegalArgumentException("Request body is required.");
        }
        return JsonUtil.parseObject(body);
    }

    static Map<String, String> queryParams(HttpExchange ex) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = ex.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            params.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                       URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return params;
    }
}
