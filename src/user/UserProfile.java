package user;

public class UserProfile {

    private int userId;
    private String name;
    private String email;
    private String role;
    private String createdAt;

    public UserProfile(
            int userId,
            String name,
            String email,
            String role,
            String createdAt) {

        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}