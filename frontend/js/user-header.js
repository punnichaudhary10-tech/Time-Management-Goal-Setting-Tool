document.addEventListener("DOMContentLoaded", async () => {
    const email = localStorage.getItem("userEmail");

    if (!email) {
        window.location.href = "login.html";
        return;
    }

    try {
        const response = await fetch(
            "/api/user/profile?email=" + encodeURIComponent(email)
        );

        if (!response.ok) {
            throw new Error("Unable to load user profile");
        }

        const profile = await response.json();

        // Headers using .profile-mini
        document.querySelectorAll(".profile-mini").forEach((container) => {
            const avatar = container.querySelector(".avatar");
            const name = container.querySelector("strong");
            const role = container.querySelector("small");

            if (avatar) {
                avatar.textContent =
                    (profile.name || "U").charAt(0).toUpperCase();
            }

            if (name) {
                name.textContent = profile.name || "User";
            }

            if (role) {
                role.textContent = profile.role || "USER";
            }
        });

        // Headers using .user-profile
        document.querySelectorAll(".user-profile").forEach((container) => {
            const avatar = container.querySelector(".avatar");
            const name = container.querySelector(".user-info strong");
            const role = container.querySelector(".user-info span");

            if (avatar) {
                avatar.textContent =
                    (profile.name || "U").charAt(0).toUpperCase();
            }

            if (name) {
                name.textContent = profile.name || "User";
            }

            if (role) {
                role.textContent = profile.role || "USER";
            }
        });

    } catch (error) {
        console.error("Header profile loading error:", error);
    }
});