package main;

import java.util.Scanner;
import user.User;
import user.UserService;

public class Main {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter User ID: ");
        int userId = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter Username: ");
        String username = sc.nextLine();

        System.out.print("Enter Email: ");
        String email = sc.nextLine();

        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        User u1 = new User(userId, username, email, password);

        UserService service = new UserService();

        service.registerUser(u1);

        System.out.println("\n----- Login -----");

        System.out.print("Enter Login Email: ");
        String loginEmail = sc.nextLine();

        System.out.print("Enter Login Password: ");
        String loginPassword = sc.nextLine();

        service.loginUser(loginEmail, loginPassword);

        sc.close();
    }
}
