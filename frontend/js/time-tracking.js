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

            goals = [];

            populateGoalSelects();
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

            console.log("Time entries:", timeEntries);


            displayTimeEntries();

            updateStatistics();

            updateProductivityCards();

            updateWeeklySummary();

        } catch (error) {

            console.error(
                "Time entry loading error:",
                error
            );

            timeEntries = [];

            displayTimeEntries();

            updateStatistics();

            updateProductivityCards();

            updateWeeklySummary();
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


            const goal =
                goals.find(function (item) {

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


                const timeInputs =
                    manualForm.querySelectorAll(
                        'input[type="time"]'
                    );


                const startInput =
                    timeInputs[0];


                const endInput =
                    timeInputs[1];


                const goalId =
                    select.value;


                const startTime =
                    startInput.value;


                const endTime =
                    endInput.value;


                if (
                    !goalId ||
                    !startTime ||
                    !endTime
                ) {

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
                    getLocalDateString();


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
        document.querySelector(
            ".timer-content select"
        );


    const timerDisplay =
        document.querySelector(
            ".timer-display"
        );


    const startButton =
        document.querySelector(
            ".start-btn"
        );


    const stopButton =
        document.querySelector(
            ".stop-btn"
        );


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
                    setInterval(
                        function () {

                            timerSeconds++;

                            updateTimerDisplay();

                        },
                        1000
                    );


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


                if (
                    timerStartTime &&
                    timerGoalId
                ) {

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
            Math.floor(
                timerSeconds / 3600
            );


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
        let goalMinutes = 0;


        const startOfToday =
            new Date(now);


        startOfToday.setHours(
            0,
            0,
            0,
            0
        );


        const startOfWeek =
            getStartOfWeek();


        timeEntries.forEach(function (entry) {

            const minutes =
                Number(entry.durationMinutes) || 0;


            const entryDate =
                new Date(entry.startTime);


            if (
                !Number.isNaN(entryDate.getTime())
            ) {

                if (entryDate >= startOfToday) {
                    todayMinutes += minutes;
                }


                if (entryDate >= startOfWeek) {
                    weekMinutes += minutes;
                }
            }


            if (entry.goalId) {
                goalMinutes += minutes;
            }

        });


        const totalMinutes =
            timeEntries.reduce(
                function (total, entry) {

                    return total +
                        (Number(entry.durationMinutes) || 0);

                },
                0
            );


        /*
         * Every time entry in this system belongs
         * to a goal. Therefore goal focus represents
         * the percentage of tracked time attached
         * to a valid goal.
         */

        const productivity =
            totalMinutes > 0
                ? Math.round(
                    (goalMinutes / totalMinutes) * 100
                )
                : 0;


        setText(
            "todayTime",
            formatDuration(todayMinutes)
        );


        setText(
            "weekTime",
            formatDuration(weekMinutes)
        );


        setText(
            "goalTime",
            formatDuration(goalMinutes)
        );


        setText(
            "productivityPercentage",
            productivity + "%"
        );
    }


    // ==========================================
    // PRODUCTIVITY CARDS
    // ==========================================

    function updateProductivityCards() {

        const startOfWeek =
            getStartOfWeek();


        const activeDates =
            new Set();


        let weekMinutes = 0;
        let goalMinutes = 0;


        timeEntries.forEach(function (entry) {

            const date =
                new Date(entry.startTime);


            const minutes =
                Number(entry.durationMinutes) || 0;


            if (
                Number.isNaN(date.getTime())
            ) {
                return;
            }


            if (date >= startOfWeek) {

                weekMinutes += minutes;

                activeDates.add(
                    getDateKey(date)
                );

            }


            if (entry.goalId) {
                goalMinutes += minutes;
            }

        });


        const focusStreak =
            calculateFocusStreak();


        const dailyAverage =
            activeDates.size > 0
                ? Math.round(
                    weekMinutes / activeDates.size
                )
                : 0;


        const totalMinutes =
            timeEntries.reduce(
                function (total, entry) {

                    return total +
                        (Number(entry.durationMinutes) || 0);

                },
                0
            );


        const goalFocus =
            totalMinutes > 0
                ? Math.round(
                    (goalMinutes / totalMinutes) * 100
                )
                : 0;


        setText(
            "focusStreak",
            focusStreak + " Days"
        );


        setText(
            "dailyAverage",
            formatDuration(dailyAverage)
        );


        setText(
            "goalFocus",
            goalFocus + "%"
        );
    }


    // ==========================================
    // CALCULATE FOCUS STREAK
    // ==========================================

    function calculateFocusStreak() {

        const activeDates =
            new Set();


        timeEntries.forEach(function (entry) {

            const date =
                new Date(entry.startTime);


            if (!Number.isNaN(date.getTime())) {

                activeDates.add(
                    getDateKey(date)
                );
            }

        });


        if (activeDates.size === 0) {
            return 0;
        }


        const today =
            new Date();


        today.setHours(
            0,
            0,
            0,
            0
        );


        let cursor =
            new Date(today);


        /*
         * If the user has not tracked time today,
         * continue the streak from yesterday.
         */

        if (
            !activeDates.has(
                getDateKey(cursor)
            )
        ) {

            cursor.setDate(
                cursor.getDate() - 1
            );
        }


        let streak = 0;


        while (
            activeDates.has(
                getDateKey(cursor)
            )
        ) {

            streak++;


            cursor.setDate(
                cursor.getDate() - 1
            );
        }


        return streak;
    }


    // ==========================================
    // WEEKLY SUMMARY
    // ==========================================

    function updateWeeklySummary() {

        const days =
            document.querySelectorAll(
                ".week-grid .day"
            );


        if (days.length !== 7) {
            return;
        }


        const totals =
            [0, 0, 0, 0, 0, 0, 0];


        const startOfWeek =
            getStartOfWeek();


        timeEntries.forEach(function (entry) {

            const date =
                new Date(entry.startTime);


            if (
                Number.isNaN(date.getTime())
            ) {
                return;
            }


            if (date < startOfWeek) {
                return;
            }


            const day =
                date.getDay();


            const index =
                day === 0
                    ? 6
                    : day - 1;


            totals[index] +=
                Number(entry.durationMinutes) || 0;
        });


        const max =
            Math.max(...totals);


        days.forEach(function (day, index) {

            const bar =
                day.querySelector(
                    ".day-bar div"
                );


            const minutes =
                totals[index];


            const hours =
                minutes / 60;


            const percentage =
                max > 0
                    ? (minutes / max) * 100
                    : 0;


            if (bar) {

                bar.style.height =
                    percentage + "%";
            }


            const value =
                day.querySelector("strong");


            if (value) {

                value.textContent =
                    formatHours(hours);
            }

        });
    }


    // ==========================================
    // START OF CURRENT WEEK
    // ==========================================

    function getStartOfWeek() {

        const date =
            new Date();


        date.setHours(
            0,
            0,
            0,
            0
        );


        const day =
            date.getDay();


        const difference =
            day === 0
                ? 6
                : day - 1;


        date.setDate(
            date.getDate() - difference
        );


        return date;
    }


    // ==========================================
    // FORMAT DURATION
    // ==========================================

    function formatDuration(minutes) {

        minutes =
            Number(minutes) || 0;


        const hours =
            Math.floor(
                minutes / 60
            );


        const mins =
            Math.round(
                minutes % 60
            );


        return (
            String(hours).padStart(2, "0") +
            "h " +
            String(mins).padStart(2, "0") +
            "m"
        );
    }


    // ==========================================
    // FORMAT HOURS
    // ==========================================

    function formatHours(hours) {

        if (hours === 0) {
            return "0h";
        }


        if (
            Number.isInteger(hours)
        ) {

            return hours + "h";
        }


        return (
            Math.round(hours * 10) / 10
        ) + "h";
    }


    // ==========================================
    // FORMAT DATE
    // ==========================================

    function formatDate(dateString) {

        if (!dateString) {
            return "-";
        }


        const date =
            new Date(dateString);


        if (Number.isNaN(date.getTime())) {
            return "-";
        }


        return date.toLocaleDateString(
            "en-GB",
            {
                day: "2-digit",
                month: "short",
                year: "numeric"
            }
        );
    }


    // ==========================================
    // FORMAT TIME
    // ==========================================

    function formatTime(dateString) {

        if (!dateString) {
            return "-";
        }


        const date =
            new Date(dateString);


        if (Number.isNaN(date.getTime())) {
            return "-";
        }


        return date.toLocaleTimeString(
            "en-US",
            {
                hour: "2-digit",
                minute: "2-digit"
            }
        );
    }


    // ==========================================
    // FORMAT DATETIME
    // ==========================================

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


    // ==========================================
    // LOCAL DATE
    // ==========================================

    function getLocalDateString() {

        const date =
            new Date();


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


        return (
            year +
            "-" +
            month +
            "-" +
            day
        );
    }


    // ==========================================
    // DATE KEY
    // ==========================================

    function getDateKey(date) {

        return (
            date.getFullYear() +
            "-" +
            String(
                date.getMonth() + 1
            ).padStart(2, "0") +
            "-" +
            String(
                date.getDate()
            ).padStart(2, "0")
        );
    }


    // ==========================================
    // SET TEXT
    // ==========================================

    function setText(id, value) {

        const element =
            document.getElementById(id);


        if (element) {

            element.textContent =
                value;
        }
    }


    // ==========================================
    // ESCAPE HTML
    // ==========================================

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