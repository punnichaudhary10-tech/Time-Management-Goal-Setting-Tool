
const loginForm = document.querySelector("form");

if (loginForm) {
    loginForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const email = document.getElementById("email").value.trim();
        const password = document.getElementById("password").value;

        try {
            const response = await fetch("/api/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: new URLSearchParams({
                    email: email,
                    password: password
                })
            });

            const data = await response.text();

            console.log("Java response:", data);

            if (response.ok && data.trim() === "Login Successful") {
                localStorage.setItem("loggedInEmail", email);
                window.location.href = "dashboard.html";
            } else {
                alert(data || "Login failed");
            }

        } catch (error) {
            console.error("Login error:", error);
            alert("Unable to connect to the server.");
        }
    });
}