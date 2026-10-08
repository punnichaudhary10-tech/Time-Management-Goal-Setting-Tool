package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import user.User;
import user.UserService;

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

public class WebServer {

        private static final Path FRONTEND_PATH = Paths.get("frontend")
                        .toAbsolutePath()
                        .normalize();

        public static void main(String[] args) throws IOException {

                int port = getPort();

                HttpServer server = HttpServer.create(
                                new InetSocketAddress("0.0.0.0", port),
                                0);

                UserService userService = new UserService();

                // ================================
                // API ROUTES
                // ================================

                server.createContext(
                                "/api/login",
                                exchange -> handleLogin(exchange, userService));

                server.createContext(
                                "/api/register",
                                exchange -> handleRegister(exchange, userService));

                // ================================
                // HTML ROUTES
                // ================================

                server.createContext(
                                "/register.html",
                                exchange -> serveHtmlPage(exchange, "register.html"));

                server.createContext(
                                "/login.html",
                                exchange -> serveHtmlPage(exchange, "login.html"));

                server.createContext(
                                "/dashboard.html",
                                exchange -> serveHtmlPage(exchange, "dashboard.html"));

                server.createContext(
                                "/index.html",
                                exchange -> serveHtmlPage(exchange, "index.html"));

                // ================================
                // ALL OTHER FRONTEND FILES
                // ================================

                server.createContext(
                                "/",
                                WebServer::serveFrontend);

                server.start();

                System.out.println("=================================");
                System.out.println("Web server started successfully!");
                System.out.println("Port: " + port);
                System.out.println("Frontend: /");
                System.out.println("Login API: /api/login");
                System.out.println("Register API: /api/register");
                System.out.println("=================================");
        }

        // ================================
        // PORT
        // ================================

        private static int getPort() {

                String port = System.getenv("PORT");

                if (port == null || port.isBlank()) {
                        return 8080;
                }

                return Integer.parseInt(port);
        }

        // ================================
        // LOGIN API
        // ================================

        private static void handleLogin(
                        HttpExchange exchange,
                        UserService userService) throws IOException {

                addCorsHeaders(exchange);

                if ("OPTIONS".equalsIgnoreCase(
                                exchange.getRequestMethod())) {

                        exchange.sendResponseHeaders(204, -1);
                        exchange.close();
                        return;
                }

                if (!"POST".equalsIgnoreCase(
                                exchange.getRequestMethod())) {

                        sendResponse(
                                        exchange,
                                        405,
                                        "Method Not Allowed");

                        return;
                }

                String requestData = readBody(exchange);

                Map<String, String> formData = parseFormData(requestData);

                String email = formData.get("email");
                String password = formData.get("password");

                if (isBlank(email) || isBlank(password)) {

                        sendResponse(
                                        exchange,
                                        400,
                                        "Email and Password are required");

                        return;
                }

                boolean result = userService.loginUser(
                                email.trim(),
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
        }

        // ================================
        // REGISTER API
        // ================================

        private static void handleRegister(
                        HttpExchange exchange,
                        UserService userService) throws IOException {

                addCorsHeaders(exchange);

                if ("OPTIONS".equalsIgnoreCase(
                                exchange.getRequestMethod())) {

                        exchange.sendResponseHeaders(204, -1);
                        exchange.close();
                        return;
                }

                if (!"POST".equalsIgnoreCase(
                                exchange.getRequestMethod())) {

                        sendResponse(
                                        exchange,
                                        405,
                                        "Method Not Allowed");

                        return;
                }

                String requestData = readBody(exchange);

                Map<String, String> formData = parseFormData(requestData);

                String name = formData.get("name");
                String email = formData.get("email");
                String password = formData.get("password");
                String confirmPassword = formData.get("confirmPassword");

                // ================================
                // VALIDATE FIELDS
                // ================================

                if (isBlank(name)
                                || isBlank(email)
                                || isBlank(password)
                                || isBlank(confirmPassword)) {

                        sendResponse(
                                        exchange,
                                        400,
                                        "All fields are required");

                        return;
                }

                // ================================
                // CHECK PASSWORD
                // ================================

                if (!password.equals(confirmPassword)) {

                        sendResponse(
                                        exchange,
                                        400,
                                        "Passwords do not match");

                        return;
                }

                // ================================
                // CREATE USER
                // ================================

                User user = new User(
                                0,
                                name.trim(),
                                email.trim(),
                                password);

                // ================================
                // SAVE USER
                // ================================

                boolean result = userService.registerUser(user);

                if (result) {

                        sendResponse(
                                        exchange,
                                        201,
                                        "Registration Successful");

                } else {

                        sendResponse(
                                        exchange,
                                        500,
                                        "Registration Failed");
                }
        }

        // ================================
        // HTML PAGE HANDLER
        // ================================

        private static void serveHtmlPage(
                        HttpExchange exchange,
                        String fileName) throws IOException {

                if (!"GET".equalsIgnoreCase(
                                exchange.getRequestMethod())) {

                        sendResponse(
                                        exchange,
                                        405,
                                        "Method Not Allowed");

                        return;
                }

                Path file = FRONTEND_PATH
                                .resolve("html")
                                .resolve(fileName)
                                .normalize();

                serveFile(exchange, file);
        }

        // ================================
        // FRONTEND STATIC FILES
        // ================================

        private static void serveFrontend(
                        HttpExchange exchange) throws IOException {

                if (!"GET".equalsIgnoreCase(
                                exchange.getRequestMethod())) {

                        sendResponse(
                                        exchange,
                                        405,
                                        "Method Not Allowed");

                        return;
                }

                String requestPath = exchange.getRequestURI().getPath();

                String relativePath;

                if (requestPath == null
                                || requestPath.equals("/")
                                || requestPath.isBlank()) {

                        relativePath = "html/index.html";

                } else {

                        relativePath = requestPath.startsWith("/")
                                        ? requestPath.substring(1)
                                        : requestPath;

                        // Handle HTML files
                        if (relativePath.endsWith(".html")
                                        && !relativePath.startsWith("html/")) {

                                relativePath = "html/" + relativePath;
                        }
                }

                Path requestedFile = FRONTEND_PATH
                                .resolve(relativePath)
                                .normalize();

                // Security check
                if (!requestedFile.startsWith(
                                FRONTEND_PATH)) {

                        sendResponse(
                                        exchange,
                                        403,
                                        "Forbidden");

                        return;
                }

                serveFile(exchange, requestedFile);
        }

        // ================================
        // FILE SERVER
        // ================================

        private static void serveFile(
                        HttpExchange exchange,
                        Path file) throws IOException {

                if (!file.startsWith(
                                FRONTEND_PATH)) {

                        sendResponse(
                                        exchange,
                                        403,
                                        "Forbidden");

                        return;
                }

                if (!Files.exists(file)
                                || !Files.isRegularFile(file)) {

                        sendResponse(
                                        exchange,
                                        404,
                                        "File Not Found");

                        return;
                }

                byte[] bytes = Files.readAllBytes(file);

                exchange.getResponseHeaders().set(
                                "Content-Type",
                                getContentType(file));

                exchange.sendResponseHeaders(
                                200,
                                bytes.length);

                try (OutputStream output = exchange.getResponseBody()) {

                        output.write(bytes);
                }
        }

        // ================================
        // READ REQUEST BODY
        // ================================

        private static String readBody(
                        HttpExchange exchange) throws IOException {

                return new String(
                                exchange.getRequestBody().readAllBytes(),
                                StandardCharsets.UTF_8);
        }

        // ================================
        // FORM DATA PARSER
        // ================================

        private static Map<String, String> parseFormData(
                        String data) {

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

        // ================================
        // VALIDATION
        // ================================

        private static boolean isBlank(
                        String value) {

                return value == null
                                || value.isBlank();
        }

        // ================================
        // CORS
        // ================================

        private static void addCorsHeaders(
                        HttpExchange exchange) {

                exchange.getResponseHeaders().set(
                                "Access-Control-Allow-Origin",
                                "*");

                exchange.getResponseHeaders().set(
                                "Access-Control-Allow-Methods",
                                "POST, OPTIONS");

                exchange.getResponseHeaders().set(
                                "Access-Control-Allow-Headers",
                                "Content-Type");
        }

        // ================================
        // TEXT RESPONSE
        // ================================

        private static void sendResponse(
                        HttpExchange exchange,
                        int statusCode,
                        String response) throws IOException {

                byte[] bytes = response.getBytes(
                                StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set(
                                "Content-Type",
                                "text/plain; charset=UTF-8");

                exchange.sendResponseHeaders(
                                statusCode,
                                bytes.length);

                try (OutputStream output = exchange.getResponseBody()) {

                        output.write(bytes);
                }
        }

        // ================================
        // CONTENT TYPES
        // ================================

        private static String getContentType(
                        Path file) {

                String name = file.getFileName()
                                .toString()
                                .toLowerCase();

                if (name.endsWith(".html")) {
                        return "text/html; charset=UTF-8";
                }

                if (name.endsWith(".css")) {
                        return "text/css; charset=UTF-8";
                }

                if (name.endsWith(".js")) {
                        return "application/javascript; charset=UTF-8";
                }

                if (name.endsWith(".png")) {
                        return "image/png";
                }

                if (name.endsWith(".jpg")
                                || name.endsWith(".jpeg")) {

                        return "image/jpeg";
                }

                if (name.endsWith(".svg")) {
                        return "image/svg+xml";
                }

                if (name.endsWith(".ico")) {
                        return "image/x-icon";
                }

                return "application/octet-stream";
        }
}