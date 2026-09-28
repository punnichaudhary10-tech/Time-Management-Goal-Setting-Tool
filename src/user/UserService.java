package user;

public class UserService {

    public void registerUser(User user) {
        System.out.println("User Registered Successfully");
        System.out.println(user);
    }

    public boolean loginUser(String email, String password) {
        if (email.equals("punni@gmail.com") && password.equals("1234")) {
            System.out.println("Login Successful");
            return true;
        } else {
            System.out.println("Invalid Email or Password");
            return false;
        }
    }
}
