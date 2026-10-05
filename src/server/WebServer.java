package server;

import java.io.InputStream;
import java.net.URLDecoder;
import user.UserService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class WebServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        UserService userService = new UserService();
        server.createContext("/login", exchange -> {

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            InputStream input = exchange.getRequestBody();

            String requestData = new String(input.readAllBytes());

            String[] parts = requestData.split("&");

            String email = URLDecoder.decode(parts[0].split("=")[1], "UTF-8");
            String password = URLDecoder.decode(parts[1].split("=")[1], "UTF-8");

            System.out.println("Email received: " + email);
            System.out.println("Password received: " + password);

            boolean result = userService.loginUser(email, password);

            String response;

            if (result) {
                response = "Login Successful";
            } else {
                response = "Invalid Email or Password";
            }

            exchange.sendResponseHeaders(200, response.length());

            OutputStream output = exchange.getResponseBody();
            output.write(response.getBytes());
            output.close();
        });

        server.start();

        System.out.println("Web server started!");
        System.out.println("Open: http://localhost:8080/login");
    }
}
