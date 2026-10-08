const loginForm = document.querySelector("form");

loginForm.addEventListener("submit", function (event) {

    event.preventDefault();

    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    fetch("http://localhost:8080/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body:
            `email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`
    })
        .then(response => response.text())
        .then(data => {

            console.log("Java response:", data);

            if (data === "Login Successful") {
                window.location.href = "dashboard.html";
            } else {
                alert(data);
            }
        })
        .catch(error => {

            console.error("Login error:", error);
            alert("Unable to connect to the server.");
        });
});