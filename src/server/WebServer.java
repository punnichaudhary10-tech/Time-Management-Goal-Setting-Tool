package server;

import user.UserService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class WebServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        UserService userService = new UserService();
        server.createContext("/login-test", exchange -> {

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            boolean result = userService.loginUser(
                    "test@gmail.com",
                    "1234");
            String response;

            if (result) {
                response = "Login Successful";
            } else {
                response = "Login Failed";
            }

            exchange.sendResponseHeaders(200, response.length());

            OutputStream output = exchange.getResponseBody();
            output.write(response.getBytes());
            output.close();
        });

        server.start();

        System.out.println("Web server started!");
        System.out.println("Open: http://localhost:8080/login-test");
    }
}
