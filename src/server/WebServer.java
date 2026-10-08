package server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import user.UserService;

public class WebServer {

    public static void main(String[] args) throws IOException {

        // Railway/cloud platforms provide PORT through environment variable.
        // For local testing, use 8080.
        int port = 8080;

        String portEnv = System.getenv("PORT");

        if (portEnv != null && !portEnv.isBlank()) {
            port = Integer.parseInt(portEnv);
        }

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port),
                0);

        UserService userService = new UserService();

        server.createContext("/login", exchange -> {

            // CORS
            exchange.getResponseHeaders().set(
                    "Access-Control-Allow-Origin", "*");

            exchange.getResponseHeaders().set(
                    "Access-Control-Allow-Methods",
                    "POST, OPTIONS");

            exchange.getResponseHeaders().set(
                    "Access-Control-Allow-Headers",
                    "Content-Type");

            // Handle browser CORS preflight request
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            // Only POST is allowed
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {

                String requestData = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8);

                Map<String, String> formData = parseFormData(requestData);

                String email = formData.get("email");
                String password = formData.get("password");

                if (email == null || password == null) {
                    sendResponse(exchange, 400, "Email and Password are required");
                    return;
                }

                System.out.println("Login request received for: " + email);

                boolean result = userService.loginUser(email, password);

                if (result) {
                    sendResponse(exchange, 200, "Login Successful");
                } else {
                    sendResponse(exchange, 401, "Invalid Email or Password");
                }

            } catch (Exception e) {

                e.printStackTrace();

                sendResponse(
                        exchange,
                        500,
                        "Server Error");
            }
        });

        server.start();

        System.out.println("=================================");
        System.out.println("Web server started successfully!");
        System.out.println("Port: " + port);
        System.out.println("Login API: /login");
        System.out.println("=================================");
    }

    private static Map<String, String> parseFormData(String data) {

        Map<String, String> formData = new HashMap<>();

        if (data == null || data.isBlank()) {
            return formData;
        }

        String[] pairs = data.split("&");

        for (String pair : pairs) {

            String[] keyValue = pair.split("=", 2);

            if (keyValue.length == 2) {

                String key = URLDecoder.decode(
                        keyValue[0],
                        StandardCharsets.UTF_8);

                String value = URLDecoder.decode(
                        keyValue[1],
                        StandardCharsets.UTF_8);

                formData.put(key, value);
            }
        }

        return formData;
    }

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String response) throws IOException {

        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=UTF-8");

        exchange.sendResponseHeaders(
                statusCode,
                responseBytes.length);

        try (OutputStream output = exchange.getResponseBody()) {

            output.write(responseBytes);
        }
    }
}