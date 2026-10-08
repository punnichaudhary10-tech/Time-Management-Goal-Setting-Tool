package progress;

public class ProgressTest {

    public static void main(String[] args) {

        ProgressService service = new ProgressService();

        // 1. Set progress to 40%
        System.out.println("\n----- SET PROGRESS TO 40% -----");

        service.updateProgress(1, 40);

        Progress progress40 = service.getProgressByGoal(1);

        if (progress40 != null) {
            System.out.println(progress40);
        }


        // 2. Set progress to 100%
        System.out.println("\n----- SET PROGRESS TO 100% -----");

        service.updateProgress(1, 100);

        Progress progress100 = service.getProgressByGoal(1);

        if (progress100 != null) {
            System.out.println(progress100);
        }
    }
}