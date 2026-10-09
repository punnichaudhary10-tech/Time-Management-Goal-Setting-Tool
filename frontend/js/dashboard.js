document.addEventListener("DOMContentLoaded", async function () {

    const userEmail = localStorage.getItem("userEmail");

    if (!userEmail) {
        alert("Please login first.");
        window.location.href = "login.html";
        return;
    }

    let userId = null;
    let goals = [];
    let timeEntries = [];
    let progressData = [];

    // =====================================================
    // INITIAL LOAD
    // =====================================================

    try {

        // Get user ID
        const userResponse = await fetch(
            `/api/user/id?email=${encodeURIComponent(userEmail)}`
        );

        if (!userResponse.ok) {
            throw new Error("Unable to get user ID");
        }

        userId = Number(await userResponse.text());

        console.log("Logged-in user ID:", userId);

        // Welcome message
        const welcomeMessage =
            document.getElementById("welcomeMessage");

        if (welcomeMessage) {
            welcomeMessage.textContent =
                `Welcome back, ${userEmail}!`;
        }

        // Today's date
        const todayDate =
            document.getElementById("todayDate");

        if (todayDate) {
            todayDate.textContent =
                "Today: " +
                new Date().toLocaleDateString("en-IN", {
                    day: "numeric",
                    month: "long",
                    year: "numeric"
                });
        }

        await loadDashboard();

        setupButtons();

    } catch (error) {

        console.error("Dashboard error:", error);

        alert("Unable to load dashboard data.");
    }


    // =====================================================
    // LOAD ALL DASHBOARD DATA
    // =====================================================

    async function loadDashboard() {

        // -----------------------------
        // GET GOALS
        // -----------------------------

        const goalsResponse = await fetch(
            `/api/goals/user/${userId}`
        );

        if (!goalsResponse.ok) {
            throw new Error("Unable to load goals");
        }

        goals = await goalsResponse.json();

        console.log("Goals:", goals);


        // -----------------------------
        // GET TIME ENTRIES
        // -----------------------------

        const timeResponse = await fetch(
            `/api/time-tracking/user/${userId}`
        );

        if (timeResponse.ok) {
            timeEntries = await timeResponse.json();
        } else {
            timeEntries = [];
        }

        console.log("Time entries:", timeEntries);


        // -----------------------------
        // GET PROGRESS
        // -----------------------------

        progressData = [];

        for (const goal of goals) {

            try {

                const response = await fetch(
                    `/api/progress/${goal.goalId}`
                );

                if (response.ok) {

                    const progress =
                        await response.json();

                    progressData.push(progress);

                } else {

                    progressData.push({
                        goalId: goal.goalId,
                        progressPercentage: 0,
                        completionStatus: goal.status
                    });
                }

            } catch (error) {

                progressData.push({
                    goalId: goal.goalId,
                    progressPercentage: 0,
                    completionStatus: goal.status
                });
            }
        }


        updateSummary();
        updateGoalsTable();
        updateTimeTable();
        updateProgress();
        updateDeadlines();
        updateActivity();
        updateTimeGoalDropdown();
    }


    // =====================================================
    // SUMMARY CARDS
    // =====================================================

    function updateSummary() {

        const totalGoals = goals.length;

        const completedGoals =
            goals.filter(
                goal => goal.status === "COMPLETED"
            ).length;


        const totalMinutes =
            timeEntries.reduce(
                (total, entry) =>
                    total +
                    Number(entry.durationMinutes || 0),
                0
            );


        const totalHours =
            totalMinutes / 60;


        let overallProgress = 0;

        if (progressData.length > 0) {

            overallProgress =
                progressData.reduce(
                    (total, progress) =>
                        total +
                        Number(
                            progress.progressPercentage || 0
                        ),
                    0
                ) / progressData.length;
        }


        document.getElementById("totalGoals").textContent =
            totalGoals;

        document.getElementById("completedGoals").textContent =
            completedGoals;

        document.getElementById("totalTime").textContent =
            `${totalHours.toFixed(1)} hrs`;

        document.getElementById("overallProgress").textContent =
            `${Math.round(overallProgress)}%`;
    }


    // =====================================================
    // GOALS TABLE
    // =====================================================

    function updateGoalsTable() {

        const tableBody =
            document.getElementById("goalsTableBody");

        if (!tableBody) {
            return;
        }

        tableBody.innerHTML = "";


        if (goals.length === 0) {

            tableBody.innerHTML = `
                <tr>
                    <td colspan="5">
                        No goals created yet.
                    </td>
                </tr>
            `;

            return;
        }


        goals.forEach(goal => {

            const progress =
                getProgressForGoal(goal.goalId);

            const percentage =
                progress
                    ? Number(
                        progress.progressPercentage || 0
                    )
                    : 0;


            const row =
                document.createElement("tr");


            row.innerHTML = `
                <td>
                    ${escapeHtml(goal.goalName)}
                </td>

                <td>
                    ${escapeHtml(String(goal.target))}
                </td>

                <td>
                    ${formatDate(goal.deadline)}
                </td>

                <td>
                    ${Math.round(percentage)}%
                </td>

                <td>
                    ${formatStatus(goal.status)}
                </td>
            `;


            tableBody.appendChild(row);
        });
    }


    // =====================================================
    // TIME TABLE
    // =====================================================

    function updateTimeTable() {

        const tableBody =
            document.getElementById("timeTableBody");

        if (!tableBody) {
            return;
        }

        tableBody.innerHTML = "";


        if (timeEntries.length === 0) {

            tableBody.innerHTML = `
                <tr>
                    <td colspan="5">
                        No time entries yet.
                    </td>
                </tr>
            `;

            return;
        }


        timeEntries
            .slice()
            .reverse()
            .slice(0, 10)
            .forEach(entry => {

                const goal =
                    goals.find(
                        item =>
                            item.goalId === entry.goalId
                    );


                const row =
                    document.createElement("tr");


                row.innerHTML = `
                    <td>
                        ${goal
                        ? escapeHtml(goal.goalName)
                        : "Unknown Goal"
                    }
                    </td>

                    <td>
                        ${formatDateTime(entry.startTime)}
                    </td>

                    <td>
                        ${formatTime(entry.startTime)}
                    </td>

                    <td>
                        ${formatTime(entry.endTime)}
                    </td>

                    <td>
                        ${formatDuration(
                        entry.durationMinutes
                    )}
                    </td>
                `;


                tableBody.appendChild(row);
            });
    }


    // =====================================================
    // PROGRESS
    // =====================================================

    function updateProgress() {

        const container =
            document.getElementById("progressContainer");

        if (!container) {
            return;
        }

        container.innerHTML = "";


        if (goals.length === 0) {

            container.innerHTML =
                "<p>No goals available.</p>";

            return;
        }


        goals.forEach(goal => {

            const progress =
                getProgressForGoal(goal.goalId);


            const percentage =
                progress
                    ? Number(
                        progress.progressPercentage || 0
                    )
                    : 0;


            const item =
                document.createElement("div");

            item.className = "progress-item";


            item.innerHTML = `
                <div>
                    <span>
                        ${escapeHtml(goal.goalName)}
                    </span>

                    <span>
                        ${Math.round(percentage)}%
                    </span>
                </div>

                <progress
                    value="${percentage}"
                    max="100">
                </progress>
            `;


            container.appendChild(item);
        });
    }


    // =====================================================
    // UPCOMING DEADLINES
    // =====================================================

    function updateDeadlines() {

        const container =
            document.getElementById(
                "deadlinesContainer"
            );

        if (!container) {
            return;
        }

        container.innerHTML = "";


        const today =
            new Date();

        today.setHours(0, 0, 0, 0);


        const upcoming =
            goals
                .filter(goal => {

                    if (!goal.deadline) {
                        return false;
                    }

                    const deadline =
                        new Date(
                            goal.deadline + "T00:00:00"
                        );

                    return (
                        deadline >= today &&
                        goal.status !== "COMPLETED"
                    );
                })
                .sort(
                    (a, b) =>
                        new Date(a.deadline) -
                        new Date(b.deadline)
                )
                .slice(0, 5);


        if (upcoming.length === 0) {

            container.innerHTML =
                "<p>No upcoming deadlines.</p>";

            return;
        }


        upcoming.forEach(goal => {

            const item =
                document.createElement("div");

            item.className = "deadline-item";


            item.innerHTML = `
                <strong>
                    ${escapeHtml(goal.goalName)}
                </strong>

                <span>
                    Due: ${formatDate(goal.deadline)}
                </span>
            `;


            container.appendChild(item);
        });
    }


    // =====================================================
    // RECENT ACTIVITY
    // =====================================================

    function updateActivity() {

        const list =
            document.getElementById("activityList");

        if (!list) {
            return;
        }

        list.innerHTML = "";


        const activities = [];


        // Goal activities
        goals.forEach(goal => {

            activities.push({
                text:
                    `Goal "${goal.goalName}" exists`,
                date:
                    goal.createdAt || goal.deadline
            });
        });


        // Time activities
        timeEntries.forEach(entry => {

            const goal =
                goals.find(
                    item =>
                        item.goalId === entry.goalId
                );


            activities.push({
                text:
                    `Logged ${formatDuration(
                        entry.durationMinutes
                    )} for "${goal
                        ? goal.goalName
                        : "Unknown Goal"
                    }"`,
                date:
                    entry.startTime
            });
        });


        // Progress activities
        progressData.forEach(progress => {

            const goal =
                goals.find(
                    item =>
                        item.goalId === progress.goalId
                );


            if (goal) {

                activities.push({
                    text:
                        `Updated progress for "${goal.goalName}"`,
                    date:
                        progress.updatedAt
                });
            }
        });


        if (activities.length === 0) {

            list.innerHTML =
                "<li>No recent activity.</li>";

            return;
        }


        activities
            .slice(-8)
            .reverse()
            .forEach(activity => {

                const li =
                    document.createElement("li");

                li.textContent =
                    activity.text;

                list.appendChild(li);
            });
    }


    // =====================================================
    // GOAL DROPDOWN FOR TIME FORM
    // =====================================================

    function updateTimeGoalDropdown() {

        const select =
            document.getElementById("timeGoal");

        if (!select) {
            return;
        }

        select.innerHTML =
            `<option value="">Select Goal</option>`;


        goals.forEach(goal => {

            const option =
                document.createElement("option");

            option.value =
                goal.goalId;

            option.textContent =
                goal.goalName;

            select.appendChild(option);
        });
    }


    // =====================================================
    // BUTTONS
    // =====================================================

    function setupButtons() {

        // Add Goal
        const addGoalBtn =
            document.getElementById("addGoalBtn");

        const addGoalBtn2 =
            document.getElementById("addGoalBtn2");

        if (addGoalBtn) {
            addGoalBtn.addEventListener(
                "click",
                showGoalForm
            );
        }

        if (addGoalBtn2) {
            addGoalBtn2.addEventListener(
                "click",
                showGoalForm
            );
        }


        // Log Time
        const logTimeBtn =
            document.getElementById("logTimeBtn");

        const logTimeBtn2 =
            document.getElementById("logTimeBtn2");

        if (logTimeBtn) {
            logTimeBtn.addEventListener(
                "click",
                showTimeForm
            );
        }

        if (logTimeBtn2) {
            logTimeBtn2.addEventListener(
                "click",
                showTimeForm
            );
        }


        // View Progress
        const viewProgressBtn =
            document.getElementById(
                "viewProgressBtn"
            );

        if (viewProgressBtn) {

            viewProgressBtn.addEventListener(
                "click",
                function () {

                    document
                        .querySelector(
                            ".progress-section"
                        )
                        .scrollIntoView({
                            behavior: "smooth"
                        });
                }
            );
        }


        // Cancel goal
        const cancelGoalBtn =
            document.getElementById(
                "cancelGoalBtn"
            );

        if (cancelGoalBtn) {

            cancelGoalBtn.addEventListener(
                "click",
                hideGoalForm
            );
        }


        // Cancel time
        const cancelTimeBtn =
            document.getElementById(
                "cancelTimeBtn"
            );

        if (cancelTimeBtn) {

            cancelTimeBtn.addEventListener(
                "click",
                hideTimeForm
            );
        }


        // Goal form
        const goalForm =
            document.getElementById(
                "goalForm"
            );

        if (goalForm) {

            goalForm.addEventListener(
                "submit",
                createGoal
            );
        }


        // Time form
        const timeForm =
            document.getElementById(
                "timeForm"
            );

        if (timeForm) {

            timeForm.addEventListener(
                "submit",
                logTime
            );
        }


        // Progress update
        const updateProgressBtn =
            document.getElementById(
                "updateProgressBtn"
            );

        if (updateProgressBtn) {

            updateProgressBtn.addEventListener(
                "click",
                updateGoalProgress
            );
        }


        // Logout
        const logoutBtn =
            document.getElementById(
                "logoutBtn"
            );

        if (logoutBtn) {

            logoutBtn.addEventListener(
                "click",
                function (event) {

                    event.preventDefault();

                    localStorage.removeItem(
                        "userEmail"
                    );

                    window.location.href =
                        "login.html";
                }
            );
        }
    }


    // =====================================================
    // SHOW GOAL FORM
    // =====================================================

    function showGoalForm() {

        const section =
            document.getElementById(
                "goalFormSection"
            );

        if (section) {

            section.hidden = false;

            section.scrollIntoView({
                behavior: "smooth"
            });
        }
    }


    function hideGoalForm() {

        const section =
            document.getElementById(
                "goalFormSection"
            );

        if (section) {
            section.hidden = true;
        }
    }


    // =====================================================
    // CREATE GOAL
    // =====================================================

    async function createGoal(event) {

        event.preventDefault();


        const goalName =
            document.getElementById(
                "goalName"
            ).value.trim();


        const description =
            document.getElementById(
                "goalDescription"
            ).value.trim();


        const target =
            document.getElementById(
                "goalTarget"
            ).value;


        const deadline =
            document.getElementById(
                "goalDeadline"
            ).value;


        if (!goalName || !target || !deadline) {

            alert(
                "Please fill all required goal fields."
            );

            return;
        }


        try {

            const response =
                await fetch(
                    "/api/goals",
                    {
                        method: "POST",

                        headers: {
                            "Content-Type":
                                "application/x-www-form-urlencoded"
                        },

                        body:
                            "userId=" +
                            encodeURIComponent(userId) +

                            "&goalName=" +
                            encodeURIComponent(goalName) +

                            "&description=" +
                            encodeURIComponent(description) +

                            "&target=" +
                            encodeURIComponent(target) +

                            "&deadline=" +
                            encodeURIComponent(deadline)
                    }
                );


            const result =
                await response.text();


            console.log(
                "Create goal:",
                response.status,
                result
            );


            if (!response.ok) {

                alert(
                    result ||
                    "Unable to create goal."
                );

                return;
            }


            alert("Goal created successfully!");


            document
                .getElementById("goalForm")
                .reset();


            hideGoalForm();

            await loadDashboard();

        } catch (error) {

            console.error(
                "Create goal error:",
                error
            );

            alert(
                "Unable to connect to the server."
            );
        }
    }


    // =====================================================
    // SHOW TIME FORM
    // =====================================================

    function showTimeForm() {

        const section =
            document.getElementById(
                "timeFormSection"
            );

        if (section) {

            section.hidden = false;

            section.scrollIntoView({
                behavior: "smooth"
            });
        }
    }


    function hideTimeForm() {

        const section =
            document.getElementById(
                "timeFormSection"
            );

        if (section) {
            section.hidden = true;
        }
    }


    // =====================================================
    // LOG TIME
    // =====================================================

    async function logTime(event) {

        event.preventDefault();


        const goalId =
            document.getElementById(
                "timeGoal"
            ).value;


        const startTime =
            document.getElementById(
                "startTime"
            ).value;


        const endTime =
            document.getElementById(
                "endTime"
            ).value;


        if (!goalId || !startTime || !endTime) {

            alert(
                "Please fill all time fields."
            );

            return;
        }


        if (
            new Date(endTime) <=
            new Date(startTime)
        ) {

            alert(
                "End time must be after start time."
            );

            return;
        }


        try {

            const response =
                await fetch(
                    "/api/time-tracking",
                    {
                        method: "POST",

                        headers: {
                            "Content-Type":
                                "application/x-www-form-urlencoded"
                        },

                        body:
                            "userId=" +
                            encodeURIComponent(userId) +

                            "&goalId=" +
                            encodeURIComponent(goalId) +

                            "&startTime=" +
                            encodeURIComponent(startTime) +

                            "&endTime=" +
                            encodeURIComponent(endTime)
                    }
                );


            const result =
                await response.text();


            console.log(
                "Log time:",
                response.status,
                result
            );


            if (!response.ok) {

                alert(
                    result ||
                    "Unable to save time entry."
                );

                return;
            }


            alert(
                "Time logged successfully!"
            );


            document
                .getElementById("timeForm")
                .reset();


            hideTimeForm();

            await loadDashboard();

        } catch (error) {

            console.error(
                "Log time error:",
                error
            );

            alert(
                "Unable to connect to the server."
            );
        }
    }


    // =====================================================
    // UPDATE PROGRESS
    // =====================================================

    async function updateGoalProgress() {

        if (goals.length === 0) {

            alert(
                "Create a goal first."
            );

            return;
        }


        let message =
            "Select a goal by entering its number:\n\n";


        goals.forEach(
            (goal, index) => {

                const progress =
                    getProgressForGoal(
                        goal.goalId
                    );

                const percentage =
                    progress
                        ? Number(
                            progress.progressPercentage ||
                            0
                        )
                        : 0;


                message +=
                    `${index + 1}. ` +
                    `${goal.goalName} ` +
                    `(${Math.round(percentage)}%)\n`;
            }
        );


        const choice =
            prompt(message);


        if (choice === null) {
            return;
        }


        const index =
            Number(choice) - 1;


        if (
            !Number.isInteger(index) ||
            index < 0 ||
            index >= goals.length
        ) {

            alert("Invalid goal number.");

            return;
        }


        const goal =
            goals[index];


        const percentage =
            prompt(
                `Enter progress for "${goal.goalName}" (0-100):`
            );


        if (percentage === null) {
            return;
        }


        const value =
            Number(percentage);


        if (
            Number.isNaN(value) ||
            value < 0 ||
            value > 100
        ) {

            alert(
                "Progress must be between 0 and 100."
            );

            return;
        }


        try {

            const response =
                await fetch(
                    "/api/progress",
                    {
                        method: "POST",

                        headers: {
                            "Content-Type":
                                "application/x-www-form-urlencoded"
                        },

                        body:
                            "goalId=" +
                            encodeURIComponent(
                                goal.goalId
                            ) +

                            "&percentage=" +
                            encodeURIComponent(value)
                    }
                );


            const result =
                await response.text();


            console.log(
                "Progress update:",
                response.status,
                result
            );


            if (!response.ok) {

                alert(
                    result ||
                    "Unable to update progress."
                );

                return;
            }


            alert(
                "Progress updated successfully!"
            );


            await loadDashboard();

        } catch (error) {

            console.error(
                "Progress update error:",
                error
            );

            alert(
                "Unable to connect to the server."
            );
        }
    }


    // =====================================================
    // GET PROGRESS FOR GOAL
    // =====================================================

    function getProgressForGoal(goalId) {

        return progressData.find(
            progress =>
                Number(progress.goalId) ===
                Number(goalId)
        );
    }


    // =====================================================
    // DATE FORMAT
    // =====================================================

    function formatDate(dateString) {

        if (!dateString) {
            return "-";
        }


        const date =
            new Date(
                dateString + "T00:00:00"
            );


        return date.toLocaleDateString(
            "en-IN",
            {
                day: "numeric",
                month: "short",
                year: "numeric"
            }
        );
    }


    // =====================================================
    // DATETIME FORMAT
    // =====================================================

    function formatDateTime(dateTimeString) {

        if (!dateTimeString) {
            return "-";
        }


        const date =
            new Date(dateTimeString);


        return date.toLocaleDateString(
            "en-IN",
            {
                day: "numeric",
                month: "short",
                year: "numeric"
            }
        );
    }


    // =====================================================
    // TIME FORMAT
    // =====================================================

    function formatTime(dateTimeString) {

        if (!dateTimeString) {
            return "-";
        }


        const date =
            new Date(dateTimeString);


        return date.toLocaleTimeString(
            "en-IN",
            {
                hour: "numeric",
                minute: "2-digit"
            }
        );
    }


    // =====================================================
    // DURATION FORMAT
    // =====================================================

    function formatDuration(minutes) {

        minutes =
            Number(minutes || 0);


        const hours =
            Math.floor(minutes / 60);


        const remainingMinutes =
            minutes % 60;


        if (hours === 0) {
            return `${remainingMinutes}m`;
        }


        if (remainingMinutes === 0) {
            return `${hours}h`;
        }


        return (
            `${hours}h ` +
            `${remainingMinutes}m`
        );
    }


    // =====================================================
    // STATUS FORMAT
    // =====================================================

    function formatStatus(status) {

        if (!status) {
            return "-";
        }


        return status
            .replaceAll("_", " ")
            .replace(
                /\b\w/g,
                letter =>
                    letter.toUpperCase()
            );
    }


    // =====================================================
    // HTML ESCAPE
    // =====================================================

    function escapeHtml(value) {

        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll(
                '"',
                "&quot;"
            )
            .replaceAll(
                "'",
                "&#039;"
            );
    }

});