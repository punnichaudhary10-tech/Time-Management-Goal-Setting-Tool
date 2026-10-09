document.addEventListener("DOMContentLoaded", async function () {

    const email = localStorage.getItem("userEmail");

    if (!email) {
        alert("Please log in first.");
        window.location.href = "login.html";
        return;
    }

    let userId = null;
    let goals = [];
    let timeEntries = [];

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
                throw new Error("Unable to load goals");
            }

            goals = await response.json();

            console.log("Goals:", goals);

            populateGoalSelects();

        } catch (error) {

            console.error("Goal loading error:", error);
        }
    }


    // ==========================================
    // POPULATE GOAL SELECT BOXES
    // ==========================================

    function populateGoalSelects() {

        const selects = document.querySelectorAll(
            ".timer-content select, .manual-card select"
        );

        selects.forEach(function (select) {

            select.innerHTML = "";

            if (goals.length === 0) {

                const option =
                    document.createElement("option");

                option.textContent = "No goals available";
                option.value = "";

                select.appendChild(option);

                return;
            }


            goals.forEach(function (goal) {

                const option =
                    document.createElement("option");

                option.value = goal.goalId;
                option.textContent = goal.goalName;

                select.appendChild(option);
            });
        });
    }


    // ==========================================
    // LOAD TIME ENTRIES
    // ==========================================

    async function loadTimeEntries() {

        try {

            const response = await fetch(
                "/api/time-tracking/user/" + userId
            );

            if (!response.ok) {
                throw new Error("Unable to load time entries");
            }

            timeEntries = await response.json();

            console.log(
                "Time entries:",
                timeEntries
            );

            displayTimeEntries();
            updateStatistics();
            updateWeeklySummary();

        } catch (error) {

            console.error(
                "Time entry loading error:",
                error
            );
        }
    }


    // ==========================================
    // DISPLAY TIME LOGS
    // ==========================================

    function displayTimeEntries() {

        const tableBody =
            document.querySelector(".logs-section tbody");

        if (!tableBody) {
            return;
        }

        tableBody.innerHTML = "";


        if (timeEntries.length === 0) {

            tableBody.innerHTML = `
                <tr>
                    <td colspan="6"
                        style="text-align:center; padding:30px;">
                        No time entries found.
                    </td>
                </tr>
            `;

            return;
        }


        const recentEntries =
            [...timeEntries]
                .sort(function (a, b) {
                    return new Date(b.startTime) -
                        new Date(a.startTime);
                })
                .slice(0, 10);


        recentEntries.forEach(function (entry) {

            const goal = goals.find(function (item) {
                return Number(item.goalId) ===
                    Number(entry.goalId);
            });


            const goalName =
                goal
                    ? goal.goalName
                    : "Unknown Goal";


            const row =
                document.createElement("tr");


            row.innerHTML = `
                <td>
                    <strong>
                        ${formatDate(entry.startTime)}
                    </strong>
                </td>

                <td>
                    ${escapeHtml(goalName)}
                </td>

                <td>
                    ${formatTime(entry.startTime)}
                </td>

                <td>
                    ${formatTime(entry.endTime)}
                </td>

                <td>
                    <strong>
                        ${formatDuration(entry.durationMinutes)}
                    </strong>
                </td>

                <td>
                    <span class="status completed">
                        Completed
                    </span>
                </td>
            `;

            tableBody.appendChild(row);
        });
    }


    // ==========================================
    // SAVE MANUAL TIME ENTRY
    // ==========================================

    const manualForm =
        document.querySelector(".manual-card form");


    if (manualForm) {

        manualForm.addEventListener(
            "submit",
            async function (event) {

                event.preventDefault();


                const select =
                    manualForm.querySelector("select");

                const startInput =
                    manualForm.querySelector(
                        'input[type="time"]'
                    );

                const endInput =
                    manualForm.querySelectorAll(
                        'input[type="time"]'
                    )[1];


                const goalId =
                    select.value;

                const startTime =
                    startInput.value;

                const endTime =
                    endInput.value;


                if (!goalId ||
                    !startTime ||
                    !endTime) {

                    alert(
                        "Please select a goal and enter both times."
                    );

                    return;
                }


                if (endTime <= startTime) {

                    alert(
                        "End time must be later than start time."
                    );

                    return;
                }


                const today =
                    new Date()
                        .toISOString()
                        .split("T")[0];


                const startDateTime =
                    today + "T" + startTime;


                const endDateTime =
                    today + "T" + endTime;


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
                                    new URLSearchParams({

                                        userId: userId,

                                        goalId: goalId,

                                        startTime:
                                            startDateTime,

                                        endTime:
                                            endDateTime

                                    })
                            }
                        );


                    const message =
                        await response.text();


                    if (response.ok) {

                        alert(
                            "Time entry saved successfully!"
                        );

                        manualForm.reset();

                        await loadTimeEntries();

                    } else {

                        alert(
                            message ||
                            "Failed to save time entry."
                        );
                    }


                } catch (error) {

                    console.error(
                        "Save time error:",
                        error
                    );

                    alert(
                        "Unable to connect to the server."
                    );
                }
            }
        );
    }


    // ==========================================
    // FOCUS TIMER
    // ==========================================

    let timerInterval = null;
    let timerSeconds = 0;
    let timerStartTime = null;
    let timerGoalId = null;


    const timerSelect =
        document.querySelector(".timer-content select");

    const timerDisplay =
        document.querySelector(".timer-display");

    const startButton =
        document.querySelector(".start-btn");

    const stopButton =
        document.querySelector(".stop-btn");


    if (startButton) {

        startButton.addEventListener(
            "click",
            function () {

                if (timerInterval) {
                    return;
                }


                timerGoalId =
                    timerSelect.value;


                if (!timerGoalId) {

                    alert(
                        "Please select a goal first."
                    );

                    return;
                }


                timerStartTime =
                    new Date();


                timerSeconds = 0;


                timerInterval =
                    setInterval(function () {

                        timerSeconds++;

                        updateTimerDisplay();

                    }, 1000);


                startButton.disabled = true;

                startButton.innerHTML =
                    '<i class="fa-solid fa-pause"></i> Running...';
            }
        );
    }


    if (stopButton) {

        stopButton.addEventListener(
            "click",
            async function () {

                if (!timerInterval) {
                    return;
                }


                clearInterval(timerInterval);

                timerInterval = null;


                const timerEndTime =
                    new Date();


                if (timerStartTime &&
                    timerGoalId) {

                    await saveTimerEntry(
                        timerStartTime,
                        timerEndTime,
                        timerGoalId
                    );
                }


                startButton.disabled = false;

                startButton.innerHTML =
                    '<i class="fa-solid fa-play"></i> Start Timer';

                timerStartTime = null;
                timerGoalId = null;
                timerSeconds = 0;

                updateTimerDisplay();
            }
        );
    }


    // ==========================================
    // SAVE TIMER ENTRY
    // ==========================================

    async function saveTimerEntry(
        startTime,
        endTime,
        goalId
    ) {

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
                            new URLSearchParams({

                                userId: userId,

                                goalId: goalId,

                                startTime:
                                    formatDateTime(startTime),

                                endTime:
                                    formatDateTime(endTime)

                            })
                    }
                );


            if (response.ok) {

                alert(
                    "Timer session saved!"
                );

                await loadTimeEntries();

            } else {

                const message =
                    await response.text();

                alert(
                    message ||
                    "Failed to save timer session."
                );
            }


        } catch (error) {

            console.error(
                "Timer save error:",
                error
            );

            alert(
                "Unable to save timer session."
            );
        }
    }


    // ==========================================
    // TIMER DISPLAY
    // ==========================================

    function updateTimerDisplay() {

        if (!timerDisplay) {
            return;
        }


        const hours =
            Math.floor(timerSeconds / 3600);

        const minutes =
            Math.floor(
                (timerSeconds % 3600) / 60
            );

        const seconds =
            timerSeconds % 60;


        timerDisplay.textContent =
            String(hours).padStart(2, "0") +
            ":" +
            String(minutes).padStart(2, "0") +
            ":" +
            String(seconds).padStart(2, "0");
    }


    // ==========================================
    // STATISTICS
    // ==========================================

    function updateStatistics() {

        const now =
            new Date();


        let todayMinutes = 0;
        let weekMinutes = 0;
        let totalMinutes = 0;


        const startOfWeek =
            new Date(now);

        const day =
            startOfWeek.getDay();

        const difference =
            day === 0 ? 6 : day - 1;

        startOfWeek.setDate(
            startOfWeek.getDate() -
            difference
        );

        startOfWeek.setHours(
            0, 0, 0, 0
        );


        timeEntries.forEach(function (entry) {

            const minutes =
                Number(entry.durationMinutes) || 0;

            totalMinutes += minutes;


            const entryDate =
                new Date(entry.startTime);


            if (
                entryDate.toDateString() ===
                now.toDateString()
            ) {
                todayMinutes += minutes;
            }


            if (entryDate >= startOfWeek) {
                weekMinutes += minutes;
            }
        });


        const statCards =
            document.querySelectorAll(
                ".stat-card h3"
            );


        if (statCards.length >= 4) {

            statCards[0].textContent =
                formatDuration(todayMinutes);

            statCards[1].textContent =
                formatDuration(weekMinutes);

            statCards[2].textContent =
                formatDuration(totalMinutes);

            statCards[3].textContent =
                totalMinutes > 0
                    ? "100%"
                    : "0%";
        }
    }


    // ==========================================
    // WEEKLY SUMMARY
    // ==========================================

    function updateWeeklySummary() {

        const days =
            document.querySelectorAll(".week-grid .day");

        if (days.length !== 7) {
            return;
        }


        const totals =
            [0, 0, 0, 0, 0, 0, 0];


        timeEntries.forEach(function (entry) {

            const date =
                new Date(entry.startTime);

            const day =
                date.getDay();

            const index =
                day === 0 ? 6 : day - 1;

            totals[index] +=
                Number(entry.durationMinutes) || 0;
        });


        const max =
            Math.max(...totals, 1);


        days.forEach(function (day, index) {

            const bar =
                day.querySelector(".day-bar div");

            const hours =
                totals[index] / 60;


            const percentage =
                (totals[index] / max) * 100;


            if (bar) {
                bar.style.height =
                    percentage + "%";
            }


            const value =
                day.querySelector("strong");

            if (value) {

                value.textContent =
                    Math.round(hours * 10) /
                    10 + "h";
            }
        });
    }


    // ==========================================
    // HELPERS
    // ==========================================

    function formatDuration(minutes) {

        minutes =
            Number(minutes) || 0;

        const hours =
            Math.floor(minutes / 60);

        const mins =
            minutes % 60;


        return (
            String(hours).padStart(2, "0") +
            "h " +
            String(mins).padStart(2, "0") +
            "m"
        );
    }


    function formatDate(dateString) {

        if (!dateString) {
            return "-";
        }


        return new Date(
            dateString
        ).toLocaleDateString(
            "en-GB",
            {
                day: "2-digit",
                month: "short",
                year: "numeric"
            }
        );
    }


    function formatTime(dateString) {

        if (!dateString) {
            return "-";
        }


        return new Date(
            dateString
        ).toLocaleTimeString(
            "en-US",
            {
                hour: "2-digit",
                minute: "2-digit"
            }
        );
    }


    function formatDateTime(date) {

        const year =
            date.getFullYear();

        const month =
            String(
                date.getMonth() + 1
            ).padStart(2, "0");

        const day =
            String(
                date.getDate()
            ).padStart(2, "0");

        const hours =
            String(
                date.getHours()
            ).padStart(2, "0");

        const minutes =
            String(
                date.getMinutes()
            ).padStart(2, "0");

        const seconds =
            String(
                date.getSeconds()
            ).padStart(2, "0");


        return (
            year +
            "-" +
            month +
            "-" +
            day +
            "T" +
            hours +
            ":" +
            minutes +
            ":" +
            seconds
        );
    }


    function escapeHtml(value) {

        if (
            value === null ||
            value === undefined
        ) {
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
    // INITIALIZE
    // ==========================================

    await getUserId();

    if (userId) {

        await loadGoals();

        await loadTimeEntries();
    }

});