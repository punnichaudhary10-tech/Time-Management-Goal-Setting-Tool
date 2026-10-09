const createGoalForm = document.getElementById("createGoalForm");

if (createGoalForm) {
    createGoalForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const email = localStorage.getItem("loggedInEmail");

        if (!email) {
            alert("Please log in before creating a goal.");
            window.location.href = "/login.html";
            return;
        }

        const goalName = document.getElementById("goalName").value.trim();
        const description = document.getElementById("description").value.trim();
        const target = document.getElementById("target").value;
        const deadline = document.getElementById("deadline").value;

        if (!goalName || !description || !target || !deadline) {
            alert("Please fill in all required fields.");
            return;
        }

        if (Number(target) <= 0) {
            alert("Target must be greater than zero.");
            return;
        }

        try {
            const response = await fetch("/api/goals", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: new URLSearchParams({
                    email: email,
                    goalName: goalName,
                    description: description,
                    target: target,
                    deadline: deadline,
                    status: "NOT_STARTED"
                })
            });

            const message = await response.text();

            if (response.ok) {
                alert("Goal created successfully!");
                createGoalForm.reset();
            } else {
                alert(message || "Failed to create goal.");
            }
        } catch (error) {
            console.error("Goal creation error:", error);
            alert("Unable to connect to the server.");
        }
    });
}