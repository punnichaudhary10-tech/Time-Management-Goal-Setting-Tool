package timetracking;

import java.time.LocalDateTime;

public class TimeTrackingTest {

    public static void main(String[] args) {

        TimeTrackingService service = new TimeTrackingService();

        TimeEntry entry = new TimeEntry(
                0,
                1,
                1,
                LocalDateTime.of(2026, 10, 8, 10, 0),
                LocalDateTime.of(2026, 10, 8, 11, 30),
                0
        );

        // Add time entry
        service.addTimeEntry(entry);

        // Display entries of Goal ID 1
        System.out.println("\n----- TIME ENTRIES -----");

        for (TimeEntry e : service.getEntriesByGoal(1)) {
            System.out.println(e);
            System.out.println("------------------------");
        }

        // Display total time
        int totalMinutes = service.getTotalMinutesForGoal(1);

        System.out.println(
                "\nTotal time spent on Goal ID 1: "
                        + totalMinutes + " minutes"
        );
    }
}