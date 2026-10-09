const registerForm = document.querySelector("form");

if (registerForm) {
    registerForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const name = document.getElementById("name").value.trim();
        const email = document.getElementById("email").value.trim();
        const password = document.getElementById("password").value;
        const confirmPassword =
            document.getElementById("confirmPassword").value;

        if (!name || !email || !password || !confirmPassword) {
            alert("All fields are required");
            return;
        }

        if (password !== confirmPassword) {
            alert("Passwords do not match");
            return;
        }

        try {
            const response = await fetch("/api/register", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: new URLSearchParams({
                    name: name,
                    email: email,
                    password: password,
                    confirmPassword: confirmPassword
                })
            });

            const message = await response.text();

            if (response.ok) {
                alert(message);
                window.location.href = "/login.html";
            } else {
                alert(message || "Registration failed");
            }
        } catch (error) {
            console.error("Registration error:", error);
            alert("Unable to connect to the server.");
        }
    });
}