import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

/** Simple command-line UI. */
public class UI {

    Scanner scanner = new Scanner(System.in);

    public void launch() throws IOException, NoSuchAlgorithmException {

        while (true) {
            try {
                mainMenu();
            } catch (IllegalArgumentException ex) {
                // Whenever the user gives invalid input, we show an error message and jump back to main menu.
                System.out.println(ex.getMessage());
            }
        }
    }

    private void mainMenu() throws IOException, NoSuchAlgorithmException {
        System.out.println("This is SimpleBackupForParanoidPeople. What would you like to do?");
        System.out.println("1: Backup your files");
        System.out.println("2: Verify backup");
        System.out.println("3: Restore files from backup");
        System.out.println("4: Help");
        System.out.println("5: Exit");
        int mainMenuChoice = getMainMenuChoice();
        if (mainMenuChoice == 1) {
            String checkListPath = getCheckListPath();
            String repositoryPath = getRepositoryPath();
            // TODO verify from user that interpreted paths are ok
            Core.createBackup(checkListPath, repositoryPath);
        } else if (mainMenuChoice == 2) {
            boolean fastVerification = getVerificationChoice();
            String checkListPath = getCheckListPath();
            String repositoryPath = getRepositoryPath();
            //Core.verifyBackup(checkListPath, repositoryPath, fastVerification);
        } else if (mainMenuChoice == 3) {
            // TODO: restore
        } else if (mainMenuChoice == 4) {
            // TODO: help
        } else if (mainMenuChoice == 5) {
            // Exit
            return;
        } else {
            throw new IllegalArgumentException("Invalid input! Number out of range.");
        }
    }

    private int getMainMenuChoice() {
        try {
            return Integer.parseInt(scanner.next());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid input! You must enter a number.");
        }
    }

    private String getCheckListPath() {
        System.out.println("Please enter path to checklist file. If you need more instructions, enter 'help'.");
        String checkListPath = scanner.next();
        if (checkListPath.equals("help")) {
            System.out.println("A checklist file contains paths to the files or folders that you wish to backup/verify.");
            System.out.println("An example checklist file is available in " + new File("backup-checklist-example.txt").getAbsolutePath());
            System.out.println("It's a good idea to copy the example file and modify it.");
            System.out.println("If you instead decide to create a new file, remember to save it with UTF-8 encoding.");
            System.out.println("Please enter path to checklist file.");
            checkListPath = scanner.next();
        }
        if (!new File(checkListPath).isFile()) {
            throw new IllegalArgumentException("Error! Given path does not correspond to a checklist file: " + new File(checkListPath).getAbsolutePath());
        }
        return checkListPath;
    }

    private String getRepositoryPath() {
        System.out.println("Please enter path to backup repository (where your backup is stored, e.g. E:\\backup).");
        String repositoryPath = scanner.next();
        if (!new File(repositoryPath).isDirectory()) {
            throw new IllegalArgumentException("Error! Given path is not an existing directory: " + new File(repositoryPath).getAbsolutePath());
        }
        return repositoryPath;
    }

    private boolean getVerificationChoice() {
        System.out.println("You can choose either fast or slow verification. "
                + "Fast verification is likely to uncover typical errors. "
                + "Slow verification is more comprehensive, but it can take a "
                + "very long time because file contents are read from disk and "
                + " compared for both original files and copies in the backup repository.\n\n" +
                "1: Fast verification\n" +
                "2: Slow verification\n");
        String verificationChoice = scanner.next();
        if (verificationChoice.equals("1")) {
            return true;
        } else if (verificationChoice.equals("2")) {
            return false;
        } else {
            throw new IllegalArgumentException("Error! Input must be either '1' or '2'.");
        }
    }
}
