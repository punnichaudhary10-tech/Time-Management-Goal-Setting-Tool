package admin;

public class AdminTest {

    public static void main(String[] args) {

        AdminService service = new AdminService();

        // =====================================
        // 1. VIEW USERS
        // =====================================
        System.out.println("\n===== VIEW USERS =====");

        service.viewAllUsers();


        // =====================================
        // 2. CREATE GOAL PARAMETER
        // =====================================
        System.out.println("\n===== CREATE GOAL PARAMETER =====");

        GoalParameter parameter = new GoalParameter(
                0,
                "STUDY_TEST",
                "TIME",
                "MINUTES",
                null,
                null
        );

        service.createGoalParameter(parameter);


        // =====================================
        // 3. VIEW GOAL PARAMETERS
        // =====================================
        System.out.println("\n===== GOAL PARAMETERS =====");

        int testParameterId = -1;

        for (GoalParameter p : service.getAllGoalParameters()) {

            System.out.println(p);
            System.out.println("--------------------------");

            if (p.getGoalType().equals("STUDY_TEST")) {
                testParameterId = p.getParameterId();
            }
        }


        // =====================================
        // 4. UPDATE TEST PARAMETER
        // =====================================
        if (testParameterId != -1) {

            System.out.println(
                    "\n===== UPDATE GOAL PARAMETER ====="
            );

            service.updateGoalParameter(
                    testParameterId,
                    "STUDY",
                    "TIME_SPENT",
                    "HOURS"
            );
        }


        // =====================================
        // 5. ADD USAGE LOG
        // =====================================
        System.out.println("\n===== ADD USAGE LOG =====");

        service.addUsageLog(
                1,
                "Tested Admin Backend"
        );


        // =====================================
        // 6. VIEW USAGE LOGS
        // =====================================
        System.out.println("\n===== VIEW USAGE LOGS =====");

        service.viewUsageLogs();


        // =====================================
        // 7. DELETE TEST GOAL PARAMETER
        // =====================================
        if (testParameterId != -1) {

            System.out.println(
                    "\n===== DELETE TEST PARAMETER ====="
            );

            service.deleteGoalParameter(
                    testParameterId
            );
        }

        System.out.println(
                "\nAdmin Backend Test Completed!"
        );
    }
}