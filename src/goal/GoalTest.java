package goal;

import java.time.LocalDate;

public class GoalTest {

    public static void main(String[] args) {

        GoalService service = new GoalService();

        // 1. GET GOAL BY ID
        System.out.println("\n----- GET GOAL -----");

        Goal goal = service.getGoalById(2);

        if (goal != null) {
            System.out.println(goal);
        } else {
            System.out.println("Goal not found!");
        }


        // 2. UPDATE GOAL
        System.out.println("\n----- UPDATE GOAL -----");

        Goal updatedGoal = new Goal(
                2,
                1,
                "Learn Advanced Java",
                "Complete Java backend and JDBC",
                50.0,
                LocalDate.of(2026, 12, 15),
                "IN_PROGRESS"
        );

        service.updateGoal(updatedGoal);


        // Check updated goal
        Goal afterUpdate = service.getGoalById(2);

        if (afterUpdate != null) {
            System.out.println(afterUpdate);
        }


        // 3. DELETE GOAL
        System.out.println("\n----- DELETE GOAL -----");

        service.deleteGoal(2);


        // Check whether goal was deleted
        Goal afterDelete = service.getGoalById(2);

        if (afterDelete == null) {
            System.out.println("Goal ID 2 successfully deleted!");
        } else {
            System.out.println("Goal still exists!");
        }
    }
}