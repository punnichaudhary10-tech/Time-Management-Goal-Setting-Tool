const loginForm = document.querySelector("form");

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
            body:
                "email=" + encodeURIComponent(email) +
                "&password=" + encodeURIComponent(password)
        });

        const data = await response.text();

        console.log("Login status:", response.status);
        console.log("Login response:", data);

        if (response.ok && data.trim() === "Login Successful") {
            localStorage.setItem("userEmail", email);
            window.location.href = "dashboard.html";
            return;
        }

        alert(data || "Invalid Email or Password");

    } catch (error) {
        console.error("Login error:", error);
        alert("Unable to connect to the server.");
    }
});