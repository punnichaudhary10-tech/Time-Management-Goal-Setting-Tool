package server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import user.UserService;

public class WebServer {

    private static final Path FRONTEND_PATH = Paths
            .get(System.getProperty("user.dir"), "frontend")
            .toAbsolutePath()
            .normalize();

    public static void main(String[] args) throws IOException {

        // Railway provides PORT.
        // Local development uses 8080.
        int port = 8080;

        String portEnv = System.getenv("PORT");

        if (portEnv != null && !portEnv.isBlank()) {
            port = Integer.parseInt(portEnv);
        }

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port),
                0);

        UserService userService = new UserService();

        // =====================================================
        // LOGIN API
        // =====================================================

        server.createContext("/login", exchange -> {

            exchange.getResponseHeaders().set(
                    "Access-Control-Allow-Origin",
                    "*");

            exchange.getResponseHeaders().set(
                    "Access-Control-Allow-Methods",
                    "POST, OPTIONS");

            exchange.getResponseHeaders().set(
                    "Access-Control-Allow-Headers",
                    "Content-Type");

            // Handle browser preflight request
            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            // Only POST is allowed for login
            if (!"POST".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                sendResponse(
                        exchange,
                        405,
                        "Method Not Allowed");

                return;
            }

            try {

                String requestData = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8);

                Map<String, String> formData = parseFormData(requestData);

                String email = formData.get("email");
                String password = formData.get("password");

                // Validate input
                if (email == null ||
                        password == null ||
                        email.isBlank() ||
                        password.isBlank()) {

                    sendResponse(
                            exchange,
                            400,
                            "Email and Password are required");

                    return;
                }

                System.out.println(
                        "Login request received for: " + email);

                boolean result = userService.loginUser(
                        email,
                        password);

                if (result) {

                    sendResponse(
                            exchange,
                            200,
                            "Login Successful");

                } else {

                    sendResponse(
                            exchange,
                            401,
                            "Invalid Email or Password");
                }

            } catch (Exception e) {

                e.printStackTrace();

                sendResponse(
                        exchange,
                        500,
                        "Server Error");
            }
        });

        // =====================================================
        // FRONTEND
        // =====================================================

        server.createContext(
                "/",
                WebServer::serveFrontend);

        // =====================================================
        // START SERVER
        // =====================================================

        server.start();

        System.out.println("=================================");
        System.out.println("Web server started successfully!");
        System.out.println("Port: " + port);
        System.out.println("Frontend: /");
        System.out.println("Login API: /login");
        System.out.println("=================================");
    }

    // =========================================================
    // SERVE FRONTEND FILES
    // =========================================================

    private static void serveFrontend(
            HttpExchange exchange) throws IOException {

        // Only GET requests are allowed
        if (!"GET".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed");

            return;
        }

        String requestPath = exchange.getRequestURI().getPath();

        /*
         * IMPORTANT FIX
         *
         * "/" directly maps to:
         *
         * frontend/html/index.html
         *
         * We do NOT first create "/html/index.html"
         * and then prepend "html/" again.
         */

        String relativePath;

        if (requestPath == null ||
                requestPath.equals("/")) {

            relativePath = "html/index.html";

        } else {

            // Remove leading slash
            relativePath = requestPath.startsWith("/")
                    ? requestPath.substring(1)
                    : requestPath;

            /*
             * HTML files:
             *
             * /login.html
             * ↓
             * frontend/html/login.html
             *
             * /dashboard.html
             * ↓
             * frontend/html/dashboard.html
             */

            if (relativePath.endsWith(".html") &&
                    !relativePath.startsWith("html/")) {

                relativePath = "html/" + relativePath;
            }
        }

        /*
         * CSS files:
         *
         * /css/style.css
         * ↓
         * frontend/css/style.css
         *
         * No modification required.
         */

        /*
         * JS files:
         *
         * /js/login.js
         * ↓
         * frontend/js/login.js
         *
         * No modification required.
         */

        Path requestedFile = FRONTEND_PATH
                .resolve(relativePath)
                .normalize();

        // =====================================================
        // SECURITY
        // Prevent path traversal such as:
        // /../some-file
        // =====================================================

        if (!requestedFile.startsWith(
                FRONTEND_PATH)) {

            sendResponse(
                    exchange,
                    403,
                    "Forbidden");

            return;
        }

        // =====================================================
        // FILE EXISTENCE CHECK
        // =====================================================

        if (!Files.exists(requestedFile) ||
                !Files.isRegularFile(requestedFile)) {

            sendResponse(
                    exchange,
                    404,
                    "File Not Found");

            return;
        }

        // =====================================================
        // READ FILE
        // =====================================================

        byte[] fileBytes = Files.readAllBytes(requestedFile);

        // =====================================================
        // CONTENT TYPE
        // =====================================================

        exchange.getResponseHeaders().set(
                "Content-Type",
                getContentType(requestedFile));

        // =====================================================
        // SEND FILE
        // =====================================================

        exchange.sendResponseHeaders(
                200,
                fileBytes.length);

        try (OutputStream output = exchange.getResponseBody()) {

            output.write(fileBytes);
        }
    }

    // =========================================================
    // CONTENT TYPE
    // =========================================================

    private static String getContentType(
            Path file) {

        String fileName = file.getFileName()
                .toString()
                .toLowerCase();

        if (fileName.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }

        if (fileName.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }

        if (fileName.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }

        if (fileName.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        }

        if (fileName.endsWith(".png")) {
            return "image/png";
        }

        if (fileName.endsWith(".jpg") ||
                fileName.endsWith(".jpeg")) {

            return "image/jpeg";
        }

        if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        }

        if (fileName.endsWith(".ico")) {
            return "image/x-icon";
        }

        return "application/octet-stream";
    }

    // =========================================================
    // FORM DATA PARSER
    // =========================================================

    private static Map<String, String> parseFormData(
            String data) {

        Map<String, String> formData = new HashMap<>();

        if (data == null ||
                data.isBlank()) {

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

                formData.put(
                        key,
                        value);
            }
        }

        return formData;
    }

    // =========================================================
    // TEXT RESPONSE
    // =========================================================

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String response) throws IOException {

        byte[] responseBytes = response.getBytes(
                StandardCharsets.UTF_8);

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