document.addEventListener("DOMContentLoaded", async function () {

    const createGoalForm = document.getElementById("createGoalForm");

    const email = localStorage.getItem("userEmail");

    if (!email) {
        alert("Please log in first.");
        window.location.href = "login.html";
        return;
    }

    let userId = null;

    // ==========================================
    // GET USER ID
    // ==========================================

    async function getUserId() {

        try {

            const response = await fetch(
                "/api/user/id?email=" + encodeURIComponent(email)
            );

            if (!response.ok) {
                throw new Error("Unable to get user ID");
            }

            userId = await response.text();

            if (!userId || Number(userId) <= 0) {
                throw new Error("Invalid user ID");
            }

            console.log("User ID:", userId);

        } catch (error) {

            console.error("User ID error:", error);
            alert("Unable to load user information.");
        }
    }


    // ==========================================
    // LOAD GOALS
    // ==========================================

    async function loadGoals() {

        try {

            const response = await fetch(
                "/api/goals/user/" + userId
            );

            if (!response.ok) {
                throw new Error("Failed to load goals");
            }

            const goals = await response.json();

            console.log("Goals:", goals);

            displayGoals(goals);
            updateStats(goals);
            updateDeadlines(goals);

        } catch (error) {

            console.error("Load goals error:", error);
            alert("Unable to load goals.");
        }
    }


    // ==========================================
    // DISPLAY GOALS
    // ==========================================

    function displayGoals(goals) {

        const tableBody = document.querySelector(".goals-section tbody");

        if (!tableBody) {
            return;
        }

        tableBody.innerHTML = "";

        if (goals.length === 0) {

            tableBody.innerHTML = `
                <tr>
                    <td colspan="6" style="text-align:center; padding:30px;">
                        No goals found. Create your first goal!
                    </td>
                </tr>
            `;

            return;
        }

        goals.forEach(function (goal) {

            const progress = goal.progressPercentage || 0;

            let statusText = "Active";

            if (goal.status === "COMPLETED") {
                statusText = "Completed";
            } else if (goal.status === "IN_PROGRESS") {
                statusText = "Active";
            }

            const row = document.createElement("tr");

            row.innerHTML = `
                <td>
                    <div class="goal-title">
                        <strong>${escapeHtml(goal.goalName)}</strong>
                        <span>${escapeHtml(goal.description || "")}</span>
                    </div>
                </td>

                <td>
                    <span class="type-badge">
                        Goal
                    </span>
                </td>

                <td>
                    <div class="progress-info">
                        <span>${progress}%</span>
                    </div>

                    <div class="progress-bar">
                        <div style="width:${progress}%"></div>
                    </div>
                </td>

                <td>
                    <strong>${formatDate(goal.deadline)}</strong>
                </td>

                <td>
                    <span class="status ${goal.status === "COMPLETED"
                    ? "completed-status"
                    : "active-status"
                }">
                        ${statusText}
                    </span>
                </td>

                <td>
                    <div class="actions">

                        <button
                            type="button"
                            onclick="editGoal(${goal.goalId})">
                            <i class="fa-solid fa-pen"></i>
                        </button>

                        <button
                            type="button"
                            onclick="deleteGoal(${goal.goalId})">
                            <i class="fa-solid fa-trash"></i>
                        </button>

                    </div>
                </td>
            `;

            tableBody.appendChild(row);
        });
    }


    // ==========================================
    // UPDATE STATISTICS
    // ==========================================

    function updateStats(goals) {

        const totalGoals = goals.length;

        const completedGoals = goals.filter(function (goal) {
            return goal.status === "COMPLETED";
        }).length;

        const activeGoals = totalGoals - completedGoals;

        const today = new Date();

        const dueSoon = goals.filter(function (goal) {

            if (!goal.deadline) {
                return false;
            }

            const deadline = new Date(goal.deadline);

            const difference =
                (deadline - today) / (1000 * 60 * 60 * 24);

            return difference >= 0 && difference <= 7;
        }).length;


        const statCards = document.querySelectorAll(".stat-card h3");

        if (statCards.length >= 4) {

            statCards[0].textContent =
                String(totalGoals).padStart(2, "0");

            statCards[1].textContent =
                String(activeGoals).padStart(2, "0");

            statCards[2].textContent =
                String(completedGoals).padStart(2, "0");

            statCards[3].textContent =
                String(dueSoon).padStart(2, "0");
        }
    }


    // ==========================================
    // CREATE GOAL
    // ==========================================

    if (createGoalForm) {

        createGoalForm.addEventListener("submit", async function (event) {

            event.preventDefault();

            const goalName =
                document.getElementById("goalName").value.trim();

            const description =
                document.getElementById("description").value.trim();

            const target =
                document.getElementById("target").value;

            const deadline =
                document.getElementById("deadline").value;


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
                        "Content-Type":
                            "application/x-www-form-urlencoded"
                    },

                    body: new URLSearchParams({

                        userId: userId,
                        goalName: goalName,
                        description: description,
                        target: target,
                        deadline: deadline

                    })
                });


                const message = await response.text();

                console.log(
                    "Create goal:",
                    response.status,
                    message
                );


                if (response.ok) {

                    alert("Goal created successfully!");

                    createGoalForm.reset();

                    await loadGoals();

                } else {

                    alert(
                        message ||
                        "Failed to create goal."
                    );
                }


            } catch (error) {

                console.error(
                    "Goal creation error:",
                    error
                );

                alert(
                    "Unable to connect to the server."
                );
            }
        });
    }


    // ==========================================
    // DELETE GOAL
    // ==========================================

    window.deleteGoal = async function (goalId) {

        const confirmed = confirm(
            "Are you sure you want to delete this goal?"
        );

        if (!confirmed) {
            return;
        }


        try {

            const response = await fetch(
                "/api/goals/" + goalId,
                {
                    method: "DELETE"
                }
            );


            const message = await response.text();


            if (response.ok) {

                alert("Goal deleted successfully!");

                await loadGoals();

            } else {

                alert(
                    message ||
                    "Failed to delete goal."
                );
            }


        } catch (error) {

            console.error(
                "Delete goal error:",
                error
            );

            alert(
                "Unable to connect to the server."
            );
        }
    };


    // ==========================================
    // EDIT GOAL
    // ==========================================

    window.editGoal = async function (goalId) {

        const newName = prompt(
            "Enter the new goal name:"
        );

        if (!newName || !newName.trim()) {
            return;
        }


        const newDeadline = prompt(
            "Enter the new deadline (YYYY-MM-DD):"
        );

        if (!newDeadline) {
            return;
        }


        try {

            const response = await fetch(
                "/api/goals/" + goalId,
                {
                    method: "PUT",

                    headers: {
                        "Content-Type":
                            "application/x-www-form-urlencoded"
                    },

                    body: new URLSearchParams({

                        goalName: newName.trim(),
                        deadline: newDeadline

                    })
                }
            );


            const message = await response.text();


            if (response.ok) {

                alert("Goal updated successfully!");

                await loadGoals();

            } else {

                alert(
                    message ||
                    "Failed to update goal."
                );
            }


        } catch (error) {

            console.error(
                "Update goal error:",
                error
            );

            alert(
                "Unable to connect to the server."
            );
        }
    };


    // ==========================================
    // UPCOMING DEADLINES
    // ==========================================

    function updateDeadlines(goals) {

        const container =
            document.querySelector(".deadline-grid");

        if (!container) {
            return;
        }

        container.innerHTML = "";

        const upcomingGoals = goals
            .filter(function (goal) {
                return goal.status !== "COMPLETED";
            })
            .sort(function (a, b) {
                return new Date(a.deadline) -
                    new Date(b.deadline);
            })
            .slice(0, 3);


        if (upcomingGoals.length === 0) {

            container.innerHTML =
                "<p>No upcoming deadlines.</p>";

            return;
        }


        upcomingGoals.forEach(function (goal) {

            const deadline =
                new Date(goal.deadline);

            const today =
                new Date();

            const daysLeft =
                Math.ceil(
                    (deadline - today) /
                    (1000 * 60 * 60 * 24)
                );


            const div =
                document.createElement("div");

            div.className = "deadline";

            div.innerHTML = `
                <div class="deadline-date">
                    <strong>${deadline.getDate()}</strong>
                    <span>
                        ${deadline.toLocaleString(
                "en-US",
                { month: "short" }
            ).toUpperCase()}
                    </span>
                </div>

                <div>
                    <strong>
                        ${escapeHtml(goal.goalName)}
                    </strong>

                    <p>
                        ${daysLeft >= 0
                    ? daysLeft + " days remaining"
                    : "Deadline passed"
                }
                    </p>
                </div>

                <span class="priority medium">
                    Goal
                </span>
            `;

            container.appendChild(div);
        });
    }


    // ==========================================
    // HELPERS
    // ==========================================

    function formatDate(dateString) {

        if (!dateString) {
            return "-";
        }

        const date =
            new Date(dateString);

        return date.toLocaleDateString(
            "en-GB",
            {
                day: "2-digit",
                month: "short",
                year: "numeric"
            }
        );
    }


    function escapeHtml(value) {

        if (value === null || value === undefined) {
            return "";
        }

        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }


    // ==========================================
    // INITIAL LOAD
    // ==========================================

    await getUserId();

    if (userId) {
        await loadGoals();
    }

});