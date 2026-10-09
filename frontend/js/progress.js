const userEmail = localStorage.getItem("userEmail");

if (!userEmail) {
    alert("Please log in first.");
    window.location.href = "login.html";
}

let userId = null;
let goals = [];
let timeEntries = [];

document.addEventListener("DOMContentLoaded", loadProgress);


/* =========================
   LOAD ALL PROGRESS DATA
========================= */

async function loadProgress() {

    try {

        // =========================
        // GET USER ID
        // =========================

        const userResponse = await fetch(
            "/api/user/id?email=" +
            encodeURIComponent(userEmail)
        );

        if (!userResponse.ok) {
            throw new Error("Unable to find user.");
        }

        userId = parseInt(await userResponse.text());

        if (!userId || userId < 1) {
            throw new Error("Invalid user ID.");
        }


        // =========================
        // GET USER GOALS
        // =========================

        const goalsResponse = await fetch(
            "/api/goals/user/" + userId
        );

        if (!goalsResponse.ok) {
            throw new Error("Unable to load goals.");
        }

        goals = await goalsResponse.json();


        // =========================
        // GET PROGRESS FOR GOALS
        // =========================

        const progressResults = await Promise.all(

            goals.map(async (goal) => {

                try {

                    const response = await fetch(
                        "/api/progress/" + goal.goalId
                    );

                    if (response.ok) {

                        const progress =
                            await response.json();

                        return {
                            ...goal,

                            progressPercentage:
                                Number(
                                    progress.progressPercentage || 0
                                ),

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
                        goal.status ||
                        "NOT_STARTED"
                };
            })
        );


        goals = progressResults;


        // =========================
        // GET TIME ENTRIES
        // =========================

        await loadTimeEntries();


        // =========================
        // UPDATE PAGE
        // =========================

        updateSummary();

        renderGoals();

        updateWeeklyProgress();

        updateAchievements();


    } catch (error) {

        console.error(
            "Progress loading error:",
            error
        );

        alert(
            "Unable to load progress. Please make sure the server is running."
        );
    }
}


/* =========================
   LOAD TIME ENTRIES
========================= */

async function loadTimeEntries() {

    try {

        const response = await fetch(
            "/api/time-tracking/user/" + userId
        );

        if (!response.ok) {

            console.error(
                "Unable to load time entries."
            );

            timeEntries = [];

            return;
        }

        timeEntries = await response.json();

    } catch (error) {

        console.error(
            "Time entry loading error:",
            error
        );

        timeEntries = [];
    }
}


/* =========================
   UPDATE SUMMARY
========================= */

function updateSummary() {

    const total = goals.length;


    const completed = goals.filter(
        goal =>
            Number(goal.progressPercentage || 0) >= 100 ||
            goal.completionStatus === "COMPLETED"
    ).length;


    const inProgress = goals.filter(
        goal =>
            Number(goal.progressPercentage || 0) > 0 &&
            Number(goal.progressPercentage || 0) < 100 &&
            goal.completionStatus !== "COMPLETED"
    ).length;


    const notStarted =
        Math.max(
            0,
            total - completed - inProgress
        );


    const average =
        total === 0
            ? 0
            : Math.round(
                goals.reduce(
                    (sum, goal) =>
                        sum +
                        Number(
                            goal.progressPercentage || 0
                        ),
                    0
                ) / total
            );


    // =========================
    // SUMMARY CARDS
    // =========================

    setText(
        "totalGoals",
        total
    );

    setText(
        "completedGoals",
        completed
    );

    setText(
        "inProgressGoals",
        inProgress
    );

    setText(
        "averageProgress",
        average + "%"
    );


    // =========================
    // CIRCLE
    // =========================

    setText(
        "completionPercentage",
        average + "%"
    );


    // =========================
    // COMPLETION DETAILS
    // =========================

    setText(
        "completedCount",
        completed +
        (completed === 1 ? " Goal" : " Goals")
    );

    setText(
        "inProgressCount",
        inProgress +
        (inProgress === 1 ? " Goal" : " Goals")
    );

    setText(
        "notStartedCount",
        notStarted +
        (notStarted === 1 ? " Goal" : " Goals")
    );


    // =========================
    // CIRCLE PROGRESS
    // =========================

    const circle =
        document.querySelector(".circle-progress");

    if (circle) {

        const degrees =
            Math.max(
                0,
                Math.min(
                    100,
                    average
                )
            ) * 3.6;

        circle.style.background =
            `conic-gradient(
                #4f46e5 ${degrees}deg,
                #e5e7eb ${degrees}deg
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


    // Remove previous dynamic goals

    goalList
        .querySelectorAll(".goal-progress-item")
        .forEach(item => item.remove());


    // =========================
    // NO GOALS
    // =========================

    if (goals.length === 0) {

        if (noGoalsMessage) {

            noGoalsMessage.textContent =
                "No goals found. Create a goal to start tracking your progress.";

            noGoalsMessage.style.display =
                "block";
        }

        return;
    }


    if (noGoalsMessage) {
        noGoalsMessage.style.display =
            "none";
    }


    // =========================
    // RENDER EACH GOAL
    // =========================

    goals.forEach(
        (goal, index) => {

            const percentage =
                Math.max(
                    0,
                    Math.min(
                        100,
                        Number(
                            goal.progressPercentage || 0
                        )
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
                            ${escapeHtml(
                goal.goalName
            )}
                        </h3>

                        <span>
                            ${escapeHtml(
                goal.description ||
                "Personal Goal"
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
        }
    );
}


/* =========================
   WEEKLY PROGRESS
========================= */

function updateWeeklyProgress() {

    const chart =
        document.getElementById("weeklyChart");

    if (!chart) {
        return;
    }


    const rows =
        chart.querySelectorAll(".chart-row");


    if (!rows.length) {
        return;
    }


    // Monday of current week

    const weekStart =
        getStartOfWeek(new Date());


    // Seven days

    const dailyMinutes =
        [
            0,
            0,
            0,
            0,
            0,
            0,
            0
        ];


    // =========================
    // CALCULATE DAILY TIME
    // =========================

    timeEntries.forEach(
        entry => {

            const start =
                parseDateTime(
                    entry.startTime
                );

            const end =
                parseDateTime(
                    entry.endTime
                );


            if (
                !start ||
                !end ||
                end <= start
            ) {
                return;
            }


            // Only current week

            if (
                start < weekStart
            ) {
                return;
            }


            const diff =
                Math.floor(
                    (
                        end.getTime() -
                        start.getTime()
                    ) / 60000
                );


            const dayIndex =
                getMondayIndex(start);


            if (
                dayIndex >= 0 &&
                dayIndex <= 6
            ) {

                dailyMinutes[dayIndex] +=
                    diff;
            }
        }
    );


    // =========================
    // FIND MAXIMUM DAY
    // =========================

    const maxMinutes =
        Math.max(
            ...dailyMinutes
        );


    // =========================
    // UPDATE BARS
    // =========================

    rows.forEach(
        (row, index) => {

            const minutes =
                dailyMinutes[index] || 0;


            let percentage = 0;


            if (maxMinutes > 0) {

                percentage =
                    Math.round(
                        (
                            minutes /
                            maxMinutes
                        ) * 100
                    );
            }


            const bar =
                row.querySelector(".bar");

            const value =
                row.querySelector("strong");


            if (bar) {

                bar.style.width =
                    percentage + "%";
            }


            if (value) {

                value.textContent =
                    percentage + "%";
            }
        }
    );
}


/* =========================
   ACHIEVEMENTS
========================= */

function updateAchievements() {

    const container =
        document.getElementById(
            "achievementList"
        );


    if (!container) {
        return;
    }


    container.innerHTML = "";


    const completedGoals =
        goals.filter(
            goal =>
                Number(
                    goal.progressPercentage || 0
                ) >= 100 ||
                goal.completionStatus ===
                "COMPLETED"
        ).length;


    const totalMinutes =
        getTotalTrackedMinutes();


    const streak =
        calculateStreak();


    let achievements = [];


    // =========================
    // 7 DAY STREAK
    // =========================

    if (streak >= 7) {

        achievements.push({

            icon: "fas fa-fire",

            title: "7 Day Streak",

            description:
                "Stayed productive for 7 consecutive days."

        });
    }


    // =========================
    // GOAL CRUSHER
    // =========================

    if (completedGoals >= 3) {

        achievements.push({

            icon: "fas fa-check-double",

            title: "Goal Crusher",

            description:
                "Completed 3 or more personal goals."

        });
    }


    // =========================
    // TIME MASTER
    // =========================

    if (totalMinutes >= 1200) {

        achievements.push({

            icon: "fas fa-hourglass-half",

            title: "Time Master",

            description:
                "Tracked more than 20 hours."

        });
    }


    // =========================
    // NO ACHIEVEMENTS
    // =========================

    if (achievements.length === 0) {

        container.innerHTML = `

            <div class="achievement">

                <div class="achievement-icon">
                    <i class="fas fa-trophy"></i>
                </div>

                <div>

                    <strong>
                        No achievements yet
                    </strong>

                    <p>
                        Keep tracking your time and completing goals to unlock achievements.
                    </p>

                </div>

            </div>

        `;

        return;
    }


    // =========================
    // RENDER ACHIEVEMENTS
    // =========================

    achievements.forEach(
        achievement => {

            const item =
                document.createElement("div");

            item.className =
                "achievement";


            item.innerHTML = `

                <div class="achievement-icon">

                    <i class="${achievement.icon}"></i>

                </div>

                <div>

                    <strong>
                        ${escapeHtml(
                achievement.title
            )}
                    </strong>

                    <p>
                        ${escapeHtml(
                achievement.description
            )}
                    </p>

                </div>

            `;


            container.appendChild(item);
        }
    );
}


/* =========================
   STREAK CALCULATION
========================= */

function calculateStreak() {

    const dates = new Set();


    timeEntries.forEach(
        entry => {

            const start =
                parseDateTime(
                    entry.startTime
                );


            if (!start) {
                return;
            }


            dates.add(
                getDateKey(start)
            );
        }
    );


    if (dates.size === 0) {
        return 0;
    }


    const sortedDates =
        Array.from(dates)
            .sort();


    let streak = 1;

    let bestStreak = 1;


    for (
        let i = 1;
        i < sortedDates.length;
        i++
    ) {

        const previous =
            new Date(
                sortedDates[i - 1] +
                "T00:00:00"
            );


        const current =
            new Date(
                sortedDates[i] +
                "T00:00:00"
            );


        const difference =
            Math.round(
                (
                    current.getTime() -
                    previous.getTime()
                ) /
                86400000
            );


        if (difference === 1) {

            streak++;

        } else {

            streak = 1;
        }


        bestStreak =
            Math.max(
                bestStreak,
                streak
            );
    }


    return bestStreak;
}


/* =========================
   TOTAL TRACKED TIME
========================= */

function getTotalTrackedMinutes() {

    return timeEntries.reduce(
        (total, entry) => {

            const start =
                parseDateTime(
                    entry.startTime
                );

            const end =
                parseDateTime(
                    entry.endTime
                );


            if (
                !start ||
                !end ||
                end <= start
            ) {
                return total;
            }


            return total +
                Math.floor(
                    (
                        end.getTime() -
                        start.getTime()
                    ) / 60000
                );
        },

        0
    );
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

    return colors[
        index % colors.length
    ];
}


function getGoalIcon(index) {

    const icons = [
        "fas fa-bullseye",
        "fas fa-book",
        "fas fa-code",
        "fas fa-chart-line",
        "fas fa-star"
    ];

    return icons[
        index % icons.length
    ];
}


/* =========================
   DATE HELPERS
========================= */

function parseDateTime(value) {

    if (!value) {
        return null;
    }


    if (value instanceof Date) {
        return value;
    }


    const text =
        String(value)
            .trim()
            .replace(" ", "T");


    const date =
        new Date(text);


    if (Number.isNaN(date.getTime())) {
        return null;
    }


    return date;
}


function getStartOfWeek(date) {

    const result =
        new Date(date);


    result.setHours(
        0,
        0,
        0,
        0
    );


    const day =
        result.getDay();


    const difference =
        day === 0
            ? -6
            : 1 - day;


    result.setDate(
        result.getDate() +
        difference
    );


    return result;
}


function getMondayIndex(date) {

    const day =
        date.getDay();


    return day === 0
        ? 6
        : day - 1;
}


function getDateKey(date) {

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


    return `${year}-${month}-${day}`;
}


/* =========================
   HTML SAFETY
========================= */

function escapeHtml(value) {

    return String(value)
        .replace(
            /&/g,
            "&amp;"
        )
        .replace(
            /</g,
            "&lt;"
        )
        .replace(
            />/g,
            "&gt;"
        )
        .replace(
            /"/g,
            "&quot;"
        )
        .replace(
            /'/g,
            "&#039;"
        );
}


/* =========================
   TEXT HELPER
========================= */

function setText(
    id,
    value
) {

    const element =
        document.getElementById(id);


    if (element) {
        element.textContent = value;
    }
}