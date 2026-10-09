package server;

import admin.AdminService;
import admin.GoalParameter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import goal.Goal;
import goal.GoalService;
import progress.Progress;
import progress.ProgressService;
import timetracking.TimeEntry;
import timetracking.TimeTrackingService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
                GoalService goalService = new GoalService();
                ProgressService progressService = new ProgressService();
                TimeTrackingService timeTrackingService = new TimeTrackingService();
                AdminService adminService = new AdminService();

                /*
                 * ================================
                 * SINGLE API ENTRY POINT
                 * ================================
                 */
                server.createContext("/api/", exchange -> handleApi(
                                exchange,
                                userService,
                                goalService,
                                progressService,
                                timeTrackingService,
                                adminService));

                /*
                 * ================================
                 * SINGLE FRONTEND ENTRY POINT
                 * ================================
                 */
                server.createContext("/", WebServer::serveFrontend);

                server.start();

                System.out.println("=================================");
                System.out.println("Web server started successfully!");
                System.out.println("Port: " + port);
                System.out.println("Frontend: /");
                System.out.println("API: /api/");
                System.out.println("=================================");
        }

        /*
         * ================================
         * API ROUTER
         * ================================
         */
        private static void handleApi(
                        HttpExchange exchange,
                        UserService userService,
                        GoalService goalService,
                        ProgressService progressService,
                        TimeTrackingService timeTrackingService,
                        AdminService adminService) throws IOException {

                addCorsHeaders(exchange);

                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(204, -1);
                        exchange.close();
                        return;
                }

                String path = exchange.getRequestURI().getPath();
                System.out.println("API request: " + exchange.getRequestMethod() + " " + path);

                if (path.equals("/api/login")) {
                        handleLogin(exchange, userService);
                        return;
                }

                if (path.equals("/api/register")) {
                        handleRegister(exchange, userService);
                        return;
                }

                if (path.equals("/api/goals") || path.startsWith("/api/goals/")) {
                        handleGoals(exchange, goalService, userService);
                        return;
                }

                if (path.equals("/api/progress") || path.startsWith("/api/progress/")) {
                        handleProgress(exchange, progressService);
                        return;
                }

                if (path.equals("/api/time-tracking") ||
                                path.startsWith("/api/time-tracking/")) {
                        handleTimeTracking(exchange, timeTrackingService);
                        return;
                }

                if (path.equals("/api/admin") || path.startsWith("/api/admin/")) {
                        handleAdmin(exchange, adminService);
                        return;
                }

                sendResponse(exchange, 404, "API route not found");
        }

        /*
         * ================================
         * LOGIN
         * POST /api/login
         * ================================
         */
        private static void handleLogin(
                        HttpExchange exchange,
                        UserService userService) throws IOException {

                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        sendResponse(exchange, 405, "Method Not Allowed");
                        return;
                }

                Map<String, String> formData = parseFormData(readBody(exchange));

                String email = formData.get("email");
                String password = formData.get("password");

                if (isBlank(email) || isBlank(password)) {
                        sendResponse(exchange, 400, "Email and Password are required");
                        return;
                }

                boolean result = userService.loginUser(email.trim(), password);

                if (result) {
                        sendResponse(exchange, 200, "Login Successful");
                } else {
                        sendResponse(exchange, 401, "Invalid Email or Password");
                }
        }

        /*
         * ================================
         * REGISTER
         * POST /api/register
         * ================================
         */
        private static void handleRegister(
                        HttpExchange exchange,
                        UserService userService) throws IOException {

                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        sendResponse(exchange, 405, "Method Not Allowed");
                        return;
                }

                Map<String, String> formData = parseFormData(readBody(exchange));

                String name = formData.get("name");
                String email = formData.get("email");
                String password = formData.get("password");
                String confirmPassword = formData.get("confirmPassword");

                if (isBlank(name) || isBlank(email) ||
                                isBlank(password) || isBlank(confirmPassword)) {
                        sendResponse(exchange, 400, "All fields are required");
                        return;
                }

                if (!password.equals(confirmPassword)) {
                        sendResponse(exchange, 400, "Passwords do not match");
                        return;
                }

                if (userService.emailExists(email.trim())) {
                        sendResponse(exchange, 409, "Email already exists");
                        return;
                }

                User user = new User(
                                0,
                                name.trim(),
                                email.trim(),
                                password);

                boolean result = userService.registerUser(user);

                if (result) {
                        sendResponse(exchange, 201, "Registration Successful");
                } else {
                        sendResponse(exchange, 500, "Registration Failed");
                }
        }

        /*
         * ================================
         * GOALS
         * POST /api/goals
         * GET /api/goals/{goalId}
         * GET /api/goals/user/{userId}
         * PUT /api/goals/{goalId}
         * DELETE /api/goals/{goalId}
         * ================================
         */
        private static void handleGoals(
                        HttpExchange exchange,
                        GoalService goalService,
                        UserService userService) throws IOException {

                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();

                if (path.equals("/api/goals")) {

                        if ("POST".equalsIgnoreCase(method)) {
                                createGoal(exchange, goalService, userService);
                                return;
                        }

                        sendResponse(exchange, 405, "Method Not Allowed");
                        return;
                }

                String remainder = path.substring("/api/goals/".length());

                if (remainder.startsWith("user/")) {

                        if (!"GET".equalsIgnoreCase(method)) {
                                sendResponse(exchange, 405, "Method Not Allowed");
                                return;
                        }

                        Integer userId = parseInt(remainder.substring("user/".length()));

                        if (userId == null) {
                                sendResponse(exchange, 400, "Invalid userId");
                                return;
                        }

                        List<Goal> goals = goalService.getGoalsByUser(userId);
                        sendJson(exchange, 200, goalsToJson(goals));
                        return;
                }

                Integer goalId = parseInt(remainder);

                if (goalId == null) {
                        sendResponse(exchange, 400, "Invalid goalId");
                        return;
                }

                if ("GET".equalsIgnoreCase(method)) {

                        Goal goal = goalService.getGoalById(goalId);

                        if (goal == null) {
                                sendResponse(exchange, 404, "Goal not found");
                        } else {
                                sendJson(exchange, 200, goalToJson(goal));
                        }
                        return;
                }

                if ("PUT".equalsIgnoreCase(method)) {
                        updateGoal(exchange, goalService, goalId);
                        return;
                }

                if ("DELETE".equalsIgnoreCase(method)) {
                        goalService.deleteGoal(goalId);
                        sendResponse(exchange, 200, "Goal delete request completed");
                        return;
                }

                sendResponse(exchange, 405, "Method Not Allowed");
        }

        private static void createGoal(
                        HttpExchange exchange,
                        GoalService goalService,
                        UserService userService) throws IOException {

                Map<String, String> data = parseFormData(readBody(exchange));

                String email = data.get("email");
                String goalName = data.get("goalName");
                String description = data.get("description");
                Double target = parseDouble(data.get("target"));
                LocalDate deadline = parseDate(data.get("deadline"));
                String status = data.get("status");

                if (isBlank(email) || isBlank(goalName) ||
                                target == null || deadline == null) {
                        sendResponse(exchange, 400,
                                        "Required fields: email, goalName, target, deadline");
                        return;
                }

                int userId = userService.getUserIdByEmail(email.trim());
                if (userId <= 0) {
                        sendResponse(exchange, 401, "User not found. Please log in again.");
                        return;
                }

                if (isBlank(status)) {
                        status = "NOT_STARTED";
                }

                Goal goal = new Goal(
                                0,
                                userId,
                                goalName.trim(),
                                description == null ? "" : description.trim(),
                                target,
                                deadline,
                                status.trim());

                goalService.createGoal(goal);
                sendResponse(exchange, 201, "Goal creation request completed");
        }

        private static void updateGoal(
                        HttpExchange exchange,
                        GoalService goalService,
                        int goalId) throws IOException {

                Goal existing = goalService.getGoalById(goalId);

                if (existing == null) {
                        sendResponse(exchange, 404, "Goal not found");
                        return;
                }

                Map<String, String> data = parseFormData(readBody(exchange));

                String goalName = data.get("goalName");
                String description = data.get("description");
                Double target = parseDouble(data.get("target"));
                LocalDate deadline = parseDate(data.get("deadline"));
                String status = data.get("status");

                if (isBlank(goalName) || target == null ||
                                deadline == null || isBlank(status)) {
                        sendResponse(exchange, 400,
                                        "Required fields: goalName, target, deadline, status");
                        return;
                }

                Goal updated = new Goal(
                                goalId,
                                existing.getUserId(),
                                goalName.trim(),
                                description == null ? "" : description.trim(),
                                target,
                                deadline,
                                status.trim());

                goalService.updateGoal(updated);
                sendResponse(exchange, 200, "Goal update request completed");
        }

        /*
         * ================================
         * PROGRESS
         * POST /api/progress
         * GET /api/progress/{goalId}
         * ================================
         */
        private static void handleProgress(
                        HttpExchange exchange,
                        ProgressService progressService) throws IOException {

                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();

                if (path.equals("/api/progress")) {

                        if (!"POST".equalsIgnoreCase(method)) {
                                sendResponse(exchange, 405, "Method Not Allowed");
                                return;
                        }

                        Map<String, String> data = parseFormData(readBody(exchange));

                        Integer goalId = parseInt(data.get("goalId"));
                        Double percentage = parseDouble(data.get("percentage"));

                        if (goalId == null || percentage == null) {
                                sendResponse(exchange, 400,
                                                "Required fields: goalId, percentage");
                                return;
                        }

                        if (percentage < 0 || percentage > 100) {
                                sendResponse(exchange, 400,
                                                "Progress must be between 0 and 100");
                                return;
                        }

                        progressService.updateProgress(goalId, percentage);
                        sendResponse(exchange, 200, "Progress update request completed");
                        return;
                }

                if (!path.startsWith("/api/progress/")) {
                        sendResponse(exchange, 404, "Progress route not found");
                        return;
                }

                if (!"GET".equalsIgnoreCase(method)) {
                        sendResponse(exchange, 405, "Method Not Allowed");
                        return;
                }

                Integer goalId = parseInt(
                                path.substring("/api/progress/".length()));

                if (goalId == null) {
                        sendResponse(exchange, 400, "Invalid goalId");
                        return;
                }

                Progress progress = progressService.getProgressByGoal(goalId);

                if (progress == null) {
                        sendResponse(exchange, 404, "Progress not found");
                } else {
                        sendJson(exchange, 200, progressToJson(progress));
                }
        }

        /*
         * ================================
         * TIME TRACKING
         * POST /api/time-tracking
         * GET /api/time-tracking/goal/{goalId}
         * GET /api/time-tracking/user/{userId}
         * GET /api/time-tracking/goal/{goalId}/total
         * DELETE /api/time-tracking/{entryId}
         * ================================
         */
        private static void handleTimeTracking(
                        HttpExchange exchange,
                        TimeTrackingService timeTrackingService) throws IOException {

                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();

                if (path.equals("/api/time-tracking")) {

                        if (!"POST".equalsIgnoreCase(method)) {
                                sendResponse(exchange, 405, "Method Not Allowed");
                                return;
                        }

                        Map<String, String> data = parseFormData(readBody(exchange));

                        Integer userId = parseInt(data.get("userId"));
                        Integer goalId = parseInt(data.get("goalId"));
                        LocalDateTime startTime = parseDateTime(data.get("startTime"));
                        LocalDateTime endTime = parseDateTime(data.get("endTime"));

                        if (userId == null || goalId == null ||
                                        startTime == null || endTime == null) {
                                sendResponse(exchange, 400,
                                                "Required fields: userId, goalId, startTime, endTime");
                                return;
                        }

                        if (endTime.isBefore(startTime)) {
                                sendResponse(exchange, 400,
                                                "End time cannot be before start time");
                                return;
                        }

                        TimeEntry entry = new TimeEntry(
                                        0,
                                        userId,
                                        goalId,
                                        startTime,
                                        endTime,
                                        0);

                        timeTrackingService.addTimeEntry(entry);
                        sendResponse(exchange, 201, "Time entry request completed");
                        return;
                }

                String prefix = "/api/time-tracking/";
                String remainder = path.startsWith(prefix)
                                ? path.substring(prefix.length())
                                : "";

                if (remainder.startsWith("goal/") && remainder.endsWith("/total")) {

                        if (!"GET".equalsIgnoreCase(method)) {
                                sendResponse(exchange, 405, "Method Not Allowed");
                                return;
                        }

                        String goalPart = remainder.substring("goal/".length(),
                                        remainder.length() - "/total".length());

                        Integer goalId = parseInt(goalPart);

                        if (goalId == null) {
                                sendResponse(exchange, 400, "Invalid goalId");
                                return;
                        }

                        int totalMinutes = timeTrackingService.getTotalMinutesForGoal(goalId);

                        sendJson(exchange, 200,
                                        "{\"goalId\":" + goalId +
                                                        ",\"totalMinutes\":" + totalMinutes + "}");
                        return;
                }

                if (remainder.startsWith("goal/")) {

                        if (!"GET".equalsIgnoreCase(method)) {
                                sendResponse(exchange, 405, "Method Not Allowed");
                                return;
                        }

                        Integer goalId = parseInt(remainder.substring("goal/".length()));

                        if (goalId == null) {
                                sendResponse(exchange, 400, "Invalid goalId");
                                return;
                        }

                        List<TimeEntry> entries = timeTrackingService.getEntriesByGoal(goalId);
                        sendJson(exchange, 200, timeEntriesToJson(entries));
                        return;
                }

                if (remainder.startsWith("user/")) {

                        if (!"GET".equalsIgnoreCase(method)) {
                                sendResponse(exchange, 405, "Method Not Allowed");
                                return;
                        }

                        Integer userId = parseInt(remainder.substring("user/".length()));

                        if (userId == null) {
                                sendResponse(exchange, 400, "Invalid userId");
                                return;
                        }

                        List<TimeEntry> entries = timeTrackingService.getEntriesByUser(userId);
                        sendJson(exchange, 200, timeEntriesToJson(entries));
                        return;
                }

                Integer entryId = parseInt(remainder);

                if (entryId != null && "DELETE".equalsIgnoreCase(method)) {
                        timeTrackingService.deleteTimeEntry(entryId);
                        sendResponse(exchange, 200, "Time entry delete request completed");
                        return;
                }

                sendResponse(exchange, 404, "Time tracking route not found");
        }

        /*
         * ================================
         * ADMIN
         * ================================
         */
        private static void handleAdmin(
                        HttpExchange exchange,
                        AdminService adminService) throws IOException {

                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();

                String prefix = "/api/admin";
                String remainder = path.length() > prefix.length()
                                ? path.substring(prefix.length())
                                : "";

                if (remainder.equals("/goal-parameters") ||
                                remainder.equals("/goal-parameters/")) {

                        if ("GET".equalsIgnoreCase(method)) {
                                List<GoalParameter> parameters = adminService.getAllGoalParameters();
                                sendJson(exchange, 200, goalParametersToJson(parameters));
                                return;
                        }

                        if ("POST".equalsIgnoreCase(method)) {

                                Map<String, String> data = parseFormData(readBody(exchange));

                                String goalType = data.get("goalType");
                                String trackingMetric = data.get("trackingMetric");
                                String targetUnit = data.get("targetUnit");
                                Integer createdBy = parseInt(data.get("createdBy"));

                                if (isBlank(goalType) || isBlank(trackingMetric) ||
                                                isBlank(targetUnit)) {
                                        sendResponse(exchange, 400,
                                                        "Required fields: goalType, trackingMetric, targetUnit");
                                        return;
                                }

                                GoalParameter parameter = new GoalParameter(
                                                0,
                                                goalType.trim(),
                                                trackingMetric.trim(),
                                                targetUnit.trim(),
                                                createdBy,
                                                LocalDateTime.now());

                                adminService.createGoalParameter(parameter);
                                sendResponse(exchange, 201,
                                                "Goal parameter creation request completed");
                                return;
                        }

                        sendResponse(exchange, 405, "Method Not Allowed");
                        return;
                }

                if (remainder.startsWith("/goal-parameters/")) {

                        Integer parameterId = parseInt(
                                        remainder.substring("/goal-parameters/".length()));

                        if (parameterId == null) {
                                sendResponse(exchange, 400, "Invalid parameterId");
                                return;
                        }

                        if ("PUT".equalsIgnoreCase(method)) {

                                Map<String, String> data = parseFormData(readBody(exchange));

                                String goalType = data.get("goalType");
                                String trackingMetric = data.get("trackingMetric");
                                String targetUnit = data.get("targetUnit");

                                if (isBlank(goalType) || isBlank(trackingMetric) ||
                                                isBlank(targetUnit)) {
                                        sendResponse(exchange, 400,
                                                        "Required fields: goalType, trackingMetric, targetUnit");
                                        return;
                                }

                                adminService.updateGoalParameter(
                                                parameterId,
                                                goalType.trim(),
                                                trackingMetric.trim(),
                                                targetUnit.trim());

                                sendResponse(exchange, 200,
                                                "Goal parameter update request completed");
                                return;
                        }

                        if ("DELETE".equalsIgnoreCase(method)) {
                                adminService.deleteGoalParameter(parameterId);
                                sendResponse(exchange, 200,
                                                "Goal parameter delete request completed");
                                return;
                        }

                        sendResponse(exchange, 405, "Method Not Allowed");
                        return;
                }

                if (remainder.startsWith("/users/")) {

                        Integer userId = parseInt(
                                        remainder.substring("/users/".length()));

                        if (userId == null) {
                                sendResponse(exchange, 400, "Invalid userId");
                                return;
                        }

                        if (!"DELETE".equalsIgnoreCase(method)) {
                                sendResponse(exchange, 405, "Method Not Allowed");
                                return;
                        }

                        adminService.deleteUser(userId);
                        sendResponse(exchange, 200, "User delete request completed");
                        return;
                }

                if (remainder.equals("/usage-logs") ||
                                remainder.equals("/usage-logs/")) {

                        if ("POST".equalsIgnoreCase(method)) {

                                Map<String, String> data = parseFormData(readBody(exchange));

                                Integer userId = parseInt(data.get("userId"));
                                String activity = data.get("activity");

                                if (userId == null || isBlank(activity)) {
                                        sendResponse(exchange, 400,
                                                        "Required fields: userId, activity");
                                        return;
                                }

                                adminService.addUsageLog(userId, activity.trim());
                                sendResponse(exchange, 201, "Usage log request completed");
                                return;
                        }

                        if ("GET".equalsIgnoreCase(method)) {
                                sendResponse(exchange, 501,
                                                "Usage-log listing is currently console-based in AdminService");
                                return;
                        }

                        sendResponse(exchange, 405, "Method Not Allowed");
                        return;
                }

                if (remainder.equals("/users") || remainder.equals("/users/")) {

                        if ("GET".equalsIgnoreCase(method)) {
                                sendResponse(exchange, 501,
                                                "User listing is currently console-based in AdminService");
                                return;
                        }

                        sendResponse(exchange, 405, "Method Not Allowed");
                        return;
                }

                sendResponse(exchange, 404, "Admin route not found");
        }

        /*
         * ================================
         * FRONTEND SERVER
         * ================================
         */
        private static void serveFrontend(
                        HttpExchange exchange) throws IOException {

                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        sendResponse(exchange, 405, "Method Not Allowed");
                        return;
                }

                String requestPath = exchange.getRequestURI().getPath();
                String relativePath;

                if (requestPath == null || requestPath.equals("/") ||
                                requestPath.isBlank()) {
                        relativePath = "html/index.html";
                } else {
                        relativePath = requestPath.startsWith("/")
                                        ? requestPath.substring(1)
                                        : requestPath;

                        if (relativePath.startsWith("html/")) {
                                // Already in frontend/html.
                        } else if (relativePath.endsWith(".html")) {
                                relativePath = "html/" + relativePath;
                        }
                }

                Path requestedFile = FRONTEND_PATH
                                .resolve(relativePath)
                                .normalize();

                if (!requestedFile.startsWith(FRONTEND_PATH)) {
                        sendResponse(exchange, 403, "Forbidden");
                        return;
                }

                serveFile(exchange, requestedFile);
        }

        /*
         * ================================
         * FILE SERVER
         * ================================
         */
        private static void serveFile(
                        HttpExchange exchange,
                        Path file) throws IOException {

                if (!file.startsWith(FRONTEND_PATH)) {
                        sendResponse(exchange, 403, "Forbidden");
                        return;
                }

                if (!Files.exists(file) || !Files.isRegularFile(file)) {
                        sendResponse(exchange, 404, "File Not Found");
                        return;
                }

                byte[] bytes = Files.readAllBytes(file);

                exchange.getResponseHeaders().set(
                                "Content-Type",
                                getContentType(file));

                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream output = exchange.getResponseBody()) {
                        output.write(bytes);
                }
        }

        /*
         * ================================
         * PORT
         * ================================
         */
        private static int getPort() {

                String port = System.getenv("PORT");

                if (port == null || port.isBlank()) {
                        return 8080;
                }

                try {
                        return Integer.parseInt(port);
                } catch (NumberFormatException e) {
                        return 8080;
                }
        }

        /*
         * ================================
         * REQUEST HELPERS
         * ================================
         */
        private static String readBody(
                        HttpExchange exchange) throws IOException {

                return new String(
                                exchange.getRequestBody().readAllBytes(),
                                StandardCharsets.UTF_8);
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

        private static boolean isBlank(String value) {
                return value == null || value.isBlank();
        }

        private static Integer parseInt(String value) {
                if (value == null || value.isBlank()) {
                        return null;
                }

                try {
                        return Integer.parseInt(value.trim());
                } catch (NumberFormatException e) {
                        return null;
                }
        }

        private static Double parseDouble(String value) {
                if (value == null || value.isBlank()) {
                        return null;
                }

                try {
                        return Double.parseDouble(value.trim());
                } catch (NumberFormatException e) {
                        return null;
                }
        }

        private static LocalDate parseDate(String value) {
                if (value == null || value.isBlank()) {
                        return null;
                }

                try {
                        return LocalDate.parse(value.trim());
                } catch (Exception e) {
                        return null;
                }
        }

        private static LocalDateTime parseDateTime(String value) {
                if (value == null || value.isBlank()) {
                        return null;
                }

                try {
                        return LocalDateTime.parse(value.trim());
                } catch (Exception e) {
                        return null;
                }
        }

        /*
         * ================================
         * CORS
         * ================================
         */
        private static void addCorsHeaders(HttpExchange exchange) {

                exchange.getResponseHeaders().set(
                                "Access-Control-Allow-Origin", "*");

                exchange.getResponseHeaders().set(
                                "Access-Control-Allow-Methods",
                                "GET, POST, PUT, DELETE, OPTIONS");

                exchange.getResponseHeaders().set(
                                "Access-Control-Allow-Headers",
                                "Content-Type");
        }

        /*
         * ================================
         * TEXT / JSON RESPONSES
         * ================================
         */
        private static void sendResponse(
                        HttpExchange exchange,
                        int statusCode,
                        String response) throws IOException {

                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set(
                                "Content-Type",
                                "text/plain; charset=UTF-8");

                exchange.sendResponseHeaders(statusCode, bytes.length);

                try (OutputStream output = exchange.getResponseBody()) {
                        output.write(bytes);
                }
        }

        private static void sendJson(
                        HttpExchange exchange,
                        int statusCode,
                        String json) throws IOException {

                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set(
                                "Content-Type",
                                "application/json; charset=UTF-8");

                exchange.sendResponseHeaders(statusCode, bytes.length);

                try (OutputStream output = exchange.getResponseBody()) {
                        output.write(bytes);
                }
        }

        /*
         * ================================
         * JSON SERIALIZATION
         * ================================
         */
        private static String goalToJson(Goal goal) {
                return "{" +
                                "\"goalId\":" + goal.getGoalId() + "," +
                                "\"userId\":" + goal.getUserId() + "," +
                                "\"goalName\":" + jsonString(goal.getGoalName()) + "," +
                                "\"description\":" + jsonString(goal.getDescription()) + "," +
                                "\"target\":" + goal.getTarget() + "," +
                                "\"deadline\":" + jsonString(goal.getDeadline().toString()) + "," +
                                "\"status\":" + jsonString(goal.getStatus()) +
                                "}";
        }

        private static String goalsToJson(List<Goal> goals) {

                StringBuilder json = new StringBuilder("[");

                for (int i = 0; i < goals.size(); i++) {
                        if (i > 0) {
                                json.append(',');
                        }
                        json.append(goalToJson(goals.get(i)));
                }

                return json.append(']').toString();
        }

        private static String progressToJson(Progress progress) {
                return "{" +
                                "\"progressId\":" + progress.getProgressId() + "," +
                                "\"goalId\":" + progress.getGoalId() + "," +
                                "\"progressPercentage\":" + progress.getProgressPercentage() + "," +
                                "\"completionStatus\":" + jsonString(progress.getCompletionStatus()) + "," +
                                "\"updatedAt\":" + jsonString(progress.getUpdatedAt().toString()) +
                                "}";
        }

        private static String timeEntryToJson(TimeEntry entry) {
                return "{" +
                                "\"entryId\":" + entry.getEntryId() + "," +
                                "\"userId\":" + entry.getUserId() + "," +
                                "\"goalId\":" + entry.getGoalId() + "," +
                                "\"startTime\":" + jsonString(entry.getStartTime().toString()) + "," +
                                "\"endTime\":" + jsonString(entry.getEndTime().toString()) + "," +
                                "\"durationMinutes\":" + entry.getDurationMinutes() +
                                "}";
        }

        private static String timeEntriesToJson(List<TimeEntry> entries) {

                StringBuilder json = new StringBuilder("[");

                for (int i = 0; i < entries.size(); i++) {
                        if (i > 0) {
                                json.append(',');
                        }
                        json.append(timeEntryToJson(entries.get(i)));
                }

                return json.append(']').toString();
        }

        private static String goalParameterToJson(GoalParameter parameter) {

                String createdBy = parameter.getCreatedBy() == null
                                ? "null"
                                : String.valueOf(parameter.getCreatedBy());

                return "{" +
                                "\"parameterId\":" + parameter.getParameterId() + "," +
                                "\"goalType\":" + jsonString(parameter.getGoalType()) + "," +
                                "\"trackingMetric\":" + jsonString(parameter.getTrackingMetric()) + "," +
                                "\"targetUnit\":" + jsonString(parameter.getTargetUnit()) + "," +
                                "\"createdBy\":" + createdBy + "," +
                                "\"createdAt\":" + jsonString(parameter.getCreatedAt().toString()) +
                                "}";
        }

        private static String goalParametersToJson(List<GoalParameter> parameters) {

                StringBuilder json = new StringBuilder("[");

                for (int i = 0; i < parameters.size(); i++) {
                        if (i > 0) {
                                json.append(',');
                        }
                        json.append(goalParameterToJson(parameters.get(i)));
                }

                return json.append(']').toString();
        }

        private static String jsonString(String value) {

                if (value == null) {
                        return "null";
                }

                return "\"" + value
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\r", "\\r")
                                .replace("\n", "\\n")
                                .replace("\t", "\\t")
                                + "\"";
        }

        /*
         * ================================
         * CONTENT TYPES
         * ================================
         */
        private static String getContentType(Path file) {

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

                if (name.endsWith(".json")) {
                        return "application/json; charset=UTF-8";
                }

                if (name.endsWith(".png")) {
                        return "image/png";
                }

                if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
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
