const userEmail = localStorage.getItem("userEmail");

if (!userEmail) {

    alert("Please log in first.");

    window.location.href = "login.html";
}


let userId = null;
let userProfile = null;
let goals = [];
let timeEntries = [];


document.addEventListener(
    "DOMContentLoaded",
    loadProfile
);


async function loadProfile() {

    try {

        updateDate();


        // =========================================
        // GET ACTUAL USER PROFILE FROM DATABASE
        // =========================================

        const profileResponse =
            await fetch(
                "/api/user/profile?email=" +
                encodeURIComponent(userEmail)
            );


        if (!profileResponse.ok) {

            throw new Error(
                "Unable to load user profile."
            );
        }


        userProfile =
            await profileResponse.json();


        userId =
            userProfile.userId;


        // =========================================
        // DISPLAY ACTUAL USER INFORMATION
        // =========================================

        displayUserProfile();


        // =========================================
        // GET GOALS
        // =========================================

        const goalsResponse =
            await fetch(
                "/api/goals/user/" +
                userId
            );


        if (goalsResponse.ok) {

            goals =
                await goalsResponse.json();

        } else {

            goals = [];
        }


        // =========================================
        // GET TIME ENTRIES
        // =========================================

        const timeResponse =
            await fetch(
                "/api/time-tracking/user/" +
                userId
            );


        if (timeResponse.ok) {

            timeEntries =
                await timeResponse.json();

        } else {

            timeEntries = [];
        }


        // =========================================
        // UPDATE STATISTICS
        // =========================================

        updateStatistics();

        await calculateAverageProgress();

    } catch (error) {

        console.error(
            "Profile loading error:",
            error
        );


        alert(
            "Unable to load your profile."
        );
    }
}


/* =========================================
   DISPLAY ACTUAL USER DATA
========================================= */

function displayUserProfile() {

    const name =
        userProfile.name || "User";

    const email =
        userProfile.email || "";

    const role =
        userProfile.role || "USER";


    // Name
    document.getElementById(
        "profileName"
    ).textContent = name;


    // Email
    document.getElementById(
        "profileEmail"
    ).textContent = email;


    // Avatar
    document.getElementById(
        "profileAvatar"
    ).textContent =
        name
            .charAt(0)
            .toUpperCase();


    // Role
    document.getElementById(
        "profileRole"
    ).textContent =
        role;


    // Form
    document.getElementById(
        "fullName"
    ).value = name;


    document.getElementById(
        "email"
    ).value = email;


    document.getElementById(
        "occupation"
    ).value = role;


    // Created date
    const createdAt =
        userProfile.createdAt;


    if (createdAt) {

        const date =
            new Date(
                createdAt.replace(" ", "T")
            );


        if (!isNaN(date.getTime())) {

            document.getElementById(
                "memberSince"
            ).textContent =
                "Member since " +
                date.toLocaleDateString(
                    "en-IN",
                    {
                        month: "long",
                        year: "numeric"
                    }
                );
        }
    }
}


/* =========================================
   UPDATE STATISTICS
========================================= */

function updateStatistics() {

    const totalGoals =
        goals.length;


    const completedGoals =
        goals.filter(
            goal =>
                goal.status === "COMPLETED"
        ).length;


    const activeGoals =
        goals.filter(
            goal =>
                goal.status === "IN_PROGRESS"
        ).length;


    const totalMinutes =
        timeEntries.reduce(
            (total, entry) =>
                total +
                Number(
                    entry.durationMinutes || 0
                ),
            0
        );


    const timeText =
        formatDuration(
            totalMinutes
        );


    document.getElementById(
        "totalGoals"
    ).textContent =
        totalGoals;


    document.getElementById(
        "completedGoals"
    ).textContent =
        completedGoals;


    document.getElementById(
        "timeTracked"
    ).textContent =
        timeText;


    document.getElementById(
        "timeTrackedText"
    ).textContent =
        timeText +
        " tracked";


    document.getElementById(
        "activeGoalsText"
    ).textContent =
        activeGoals +
        (
            activeGoals === 1
                ? " goal currently in progress"
                : " goals currently in progress"
        );


    document.getElementById(
        "completedText"
    ).textContent =
        completedGoals +
        (
            completedGoals === 1
                ? " goal completed"
                : " goals completed"
        );
}


/* =========================================
   CALCULATE REAL PROGRESS
========================================= */

async function calculateAverageProgress() {

    if (goals.length === 0) {

        updateProgressDisplay(0);

        return;
    }


    let totalProgress = 0;


    for (const goal of goals) {

        try {

            const response =
                await fetch(
                    "/api/progress/" +
                    goal.goalId
                );


            if (response.ok) {

                const progress =
                    await response.json();


                totalProgress +=
                    Number(
                        progress.progressPercentage || 0
                    );

            } else if (
                goal.status === "COMPLETED"
            ) {

                totalProgress += 100;
            }

        } catch (error) {

            console.error(
                "Progress lookup failed:",
                error
            );


            if (
                goal.status === "COMPLETED"
            ) {

                totalProgress += 100;
            }
        }
    }


    const average =
        Math.round(
            totalProgress /
            goals.length
        );


    updateProgressDisplay(
        average
    );
}


/* =========================================
   UPDATE PROGRESS UI
========================================= */

function updateProgressDisplay(
    percentage
) {

    document.getElementById(
        "overallProgress"
    ).textContent =
        percentage + "%";


    document.getElementById(
        "profileProgressPercentage"
    ).textContent =
        percentage + "%";


    document.getElementById(
        "profileProgressFill"
    ).style.width =
        percentage + "%";


    document.getElementById(
        "progressText"
    ).textContent =
        "Overall goal progress is " +
        percentage + "%";


    const message =
        document.getElementById(
            "progressMessage"
        );


    if (percentage === 0) {

        message.textContent =
            "Create goals to start tracking your progress.";

    } else if (percentage < 50) {

        message.textContent =
            "Keep going! You are building steady progress.";

    } else if (percentage < 100) {

        message.textContent =
            "Great work! Keep pushing toward your goals.";

    } else {

        message.textContent =
            "Excellent! All your goals are complete.";
    }
}


/* =========================================
   FORMAT TIME
========================================= */

function formatDuration(minutes) {

    const total =
        Math.max(
            0,
            Number(minutes || 0)
        );


    const hours =
        Math.floor(
            total / 60
        );


    const remainingMinutes =
        total % 60;


    return (
        hours +
        "h " +
        remainingMinutes +
        "m"
    );
}


/* =========================================
   DATE
========================================= */

function updateDate() {

    const today =
        new Date();


    document.getElementById(
        "todayDate"
    ).textContent =
        today.toLocaleDateString(
            "en-IN",
            {
                day: "numeric",
                month: "long",
                year: "numeric"
            }
        );
}


/* =========================================
   LOGOUT
========================================= */

const logoutButton =
    document.getElementById(
        "logoutBtn"
    );


if (logoutButton) {

    logoutButton.addEventListener(
        "click",
        function () {

            localStorage.removeItem(
                "userEmail"
            );

            localStorage.removeItem(
                "loggedInEmail"
            );

            window.location.href =
                "login.html";
        }
    );
}