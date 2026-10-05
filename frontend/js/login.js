const loginForm = document.querySelector("form");

loginForm.addEventListener("submit", function (event) {
    event.preventDefault();

    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    console.log("Email:", email);
    console.log("Password:", password);

    fetch("http://localhost:8080/login", {
        method: "POST",
        headers: {
            "content-Type": "application/x-www-form-urlencoded"
        },
        body: `email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`
    })
        .then(response => response.text())
        .then(data => {
            console.log("Java response:", data);

            if (data === "Login Successful") {
                window.location.href = "dashboard.html";
            } else {
                alert("Invalid Email or Password");
            }
        })
        .catch(error => {
            console.error("Error: ", error);
        });
});