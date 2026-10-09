const userEmail = localStorage.getItem("userEmail");

if (!userEmail) {
    alert("Please log in first.");
    window.location.href = "login.html";
}

let userId = null;
let goals = [];

document.addEventListener("DOMContentLoaded", loadProgress);


async function loadProgress() {

    try {

        // Get logged-in user's ID
        const userResponse = await fetch(
            "/api/user/id?email=" + encodeURIComponent(userEmail)
        );

        if (!userResponse.ok) {
            throw new Error("Unable to find user.");
        }

        userId = parseInt(await userResponse.text());

        if (!userId || userId < 1) {
            throw new Error("Invalid user ID.");
        }


        // Get user's goals
        const goalsResponse = await fetch(
            "/api/goals/user/" + userId
        );

        if (!goalsResponse.ok) {
            throw new Error("Unable to load goals.");
        }

        goals = await goalsResponse.json();


        // Get progress for every goal
        const progressResults = await Promise.all(
            goals.map(async (goal) => {

                try {

                    const response = await fetch(
                        "/api/progress/" + goal.goalId
                    );

                    if (response.ok) {

                        const progress = await response.json();

                        return {
                            ...goal,
                            progressPercentage:
                                Number(progress.progressPercentage || 0),
                            completionStatus:
                                progress.completionStatus ||
                                goal.status ||
                                "NOT_STARTED"
                        };
                    }

                } catch (error) {
                    console.error(
                        "Progress loading error:",
                        error
                    );
                }

                return {
                    ...goal,
                    progressPercentage: 0,
                    completionStatus:
                        goal.status || "NOT_STARTED"
                };
            })
        );


        goals = progressResults;

        updateSummary();
        renderGoals();

    } catch (error) {

        console.error("Progress loading error:", error);

        alert(
            "Unable to load progress. Please make sure the server is running."
        );
    }
}


/* =========================
   UPDATE SUMMARY
========================= */

function updateSummary() {

    const total = goals.length;

    const completed = goals.filter(
        goal =>
            goal.progressPercentage >= 100 ||
            goal.completionStatus === "COMPLETED"
    ).length;

    const inProgress = goals.filter(
        goal =>
            goal.progressPercentage > 0 &&
            goal.progressPercentage < 100
    ).length;

    const notStarted = total - completed - inProgress;


    const average =
        total === 0
            ? 0
            : Math.round(
                goals.reduce(
                    (sum, goal) =>
                        sum + Number(goal.progressPercentage || 0),
                    0
                ) / total
            );


    // Summary cards
    document.getElementById("totalGoals").textContent = total;
    document.getElementById("completedGoals").textContent = completed;
    document.getElementById("inProgressGoals").textContent = inProgress;
    document.getElementById("averageProgress").textContent =
        average + "%";


    // Circle
    document.getElementById("completionPercentage").textContent =
        average + "%";


    // Completion details
    document.getElementById("completedCount").textContent =
        completed + (completed === 1 ? " Goal" : " Goals");

    document.getElementById("inProgressCount").textContent =
        inProgress + (inProgress === 1 ? " Goal" : " Goals");

    document.getElementById("notStartedCount").textContent =
        notStarted + (notStarted === 1 ? " Goal" : " Goals");


    // Update circle progress
    const circle = document.querySelector(".circle-progress");

    if (circle) {

        circle.style.background =
            `conic-gradient(
                #4f46e5 ${average * 3.6}deg,
                #e5e7eb ${average * 3.6}deg
            )`;
    }
}


/* =========================
   RENDER GOALS
========================= */

function renderGoals() {

    const goalList =
        document.getElementById("goalList");

    const noGoalsMessage =
        document.getElementById("noGoalsMessage");


    if (!goalList) {
        return;
    }


    // Remove old dynamically created goals
    goalList
        .querySelectorAll(".goal-progress-item")
        .forEach(item => item.remove());


    if (goals.length === 0) {

        if (noGoalsMessage) {
            noGoalsMessage.style.display = "block";
        }

        return;
    }


    if (noGoalsMessage) {
        noGoalsMessage.style.display = "none";
    }


    goals.forEach((goal, index) => {

        const percentage =
            Math.max(
                0,
                Math.min(
                    100,
                    Number(goal.progressPercentage || 0)
                )
            );


        const status =
            getStatusText(
                percentage,
                goal.completionStatus
            );


        const statusClass =
            getStatusClass(
                percentage,
                goal.completionStatus
            );


        const colorClass =
            getColorClass(index);


        const icon =
            getGoalIcon(index);


        const item =
            document.createElement("div");

        item.className =
            "goal-progress-item";


        item.innerHTML = `

            <div class="goal-info">

                <div class="goal-icon ${colorClass}">
                    <i class="${icon}"></i>
                </div>

                <div>
                    <h3>
                        ${escapeHtml(goal.goalName)}
                    </h3>

                    <span>
                        ${escapeHtml(
            goal.description || "Personal Goal"
        )}
                    </span>
                </div>

            </div>


            <div class="goal-progress-bar">

                <div class="progress-label">

                    <span>Progress</span>

                    <strong>
                        ${percentage}%
                    </strong>

                </div>

                <div class="progress-track">

                    <div
                        class="progress-fill ${colorClass}-fill"
                        style="width: ${percentage}%;">
                    </div>

                </div>

            </div>


            <span class="status-badge ${statusClass}">
                ${status}
            </span>

        `;


        goalList.appendChild(item);
    });
}


/* =========================
   STATUS
========================= */

function getStatusText(
    percentage,
    completionStatus
) {

    if (
        percentage >= 100 ||
        completionStatus === "COMPLETED"
    ) {
        return "Completed";
    }

    if (percentage > 0) {

        if (percentage >= 75) {
            return "Almost Complete";
        }

        return "In Progress";
    }

    return "Not Started";
}


function getStatusClass(
    percentage,
    completionStatus
) {

    if (
        percentage >= 100 ||
        completionStatus === "COMPLETED"
    ) {
        return "completed";
    }

    if (percentage > 0) {

        if (percentage >= 75) {
            return "almost";
        }

        return "active";
    }

    return "started";
}


/* =========================
   COLORS / ICONS
========================= */

function getColorClass(index) {

    const colors = [
        "blue",
        "green",
        "orange",
        "purple",
        "teal"
    ];

    return colors[index % colors.length];
}


function getGoalIcon(index) {

    const icons = [
        "fas fa-bullseye",
        "fas fa-book",
        "fas fa-code",
        "fas fa-chart-line",
        "fas fa-star"
    ];

    return icons[index % icons.length];
}


/* =========================
   HTML SAFETY
========================= */

function escapeHtml(value) {

    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}