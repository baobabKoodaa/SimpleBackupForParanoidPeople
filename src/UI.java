import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

/** Simple command-line UI. */
public class UI {

    public static void runUI() throws IOException, NoSuchAlgorithmException {
        Scanner scanner = new Scanner(System.in);

        while (true) {

            // Print options
            System.out.println("This is SimpleBackupForParanoidPeople. What would you like to do?");
            System.out.println("1: Backup your files");
            System.out.println("2: Verify backup");
            System.out.println("3: Restore files from backup");
            System.out.println("4: Help");
            System.out.println("5: Exit");

            // Parse user input
            String userInput = scanner.next();
            int mainMenuChoice = 0;
            try {
                mainMenuChoice = Integer.parseInt(userInput);
            } catch (Exception ex) {
                System.out.println("Invalid input! You must enter a number.");
                continue;
            }

            // Execute user input
            if (mainMenuChoice == 1) {
                System.out.println("FROM: Please enter path to checklist file. If you need more instructions, enter 'help'.");
                String checkListPath = scanner.next();
                if (checkListPath.equals("help")) {
                    System.out.println("A checklist file contains paths to the files or folders that you wish to backup.");
                    System.out.println("An example checklist file is available in " + new File("backup-checklist-example.txt").getAbsolutePath());
                    System.out.println("It's a good idea to copy the example file and modify it.");
                    System.out.println("If you instead decide to create a new file, remember to save it with UTF-8 encoding.");
                    System.out.println("Please enter path to checklist file.");
                    checkListPath = scanner.next();
                }
                if (!new File(checkListPath).isFile()) {
                    System.out.println("Error! Given path does not correspond to a checklist file: " + new File(checkListPath).getAbsolutePath());
                    continue;
                }
                System.out.println("TO: Please enter path to backup repository (where your backup will be stored, e.g. E:\\backup).");
                String repositoryPath = scanner.next();
                if (!new File(repositoryPath).isDirectory()) {
                    System.out.println("Error! Given path is not an existing directory: " + new File(repositoryPath).getAbsolutePath());
                }
                // TODO verify from user that interpreted paths are ok
                Core.createBackup(checkListPath, repositoryPath);
            } else if (mainMenuChoice == 2) {
                System.out.println("You can choose either fast or slow verification. "
                        + "Fast verification is likely to uncover typical errors. "
                        + "Slow verification is more comprehensive, but it can take a "
                        + "very long time because file contents are read from disk and "
                        + " compared for both original files and copies in the backup repository.\n\n" +
                        "1: Fast verification\n" +
                        "2: Slow verification\n");
                String verificationChoice = scanner.next();
                if (verificationChoice.equals("1")) {
                    // TODO ask for checklist-path and repository-path (reuse code)
                } else if (verificationChoice.equals("2")) {
                    // TODO ask for checklist-path and repository-path (reuse code)
                } else {
                    System.out.println("Invalid input!");
                    continue;
                }
            } else if (mainMenuChoice == 3) {

            } else if (mainMenuChoice == 4) {

            } else if (mainMenuChoice == 5) {
                return;
            } else {
                System.out.println("Invalid input! Number out of range.");
                continue;
            }
        }
    }
}
