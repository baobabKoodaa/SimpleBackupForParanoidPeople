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
                System.out.println("This is SimpleBackupForParanoidPeople. What would you like to do?");
                System.out.println("B: Backup your files");
                System.out.println("V: Verify backup");
                System.out.println("R: Restore files from backup");
                System.out.println("D: Detect duplicates");
                System.out.println("C: Compare snapshots");
                System.out.println("H: Help");
                System.out.println("Q: Exit");
                String mainMenuChoice = scanner.nextLine().trim().toUpperCase();
                if (mainMenuChoice.equals("B")) {
                    String checkListPath = getCheckListPath();
                    String repositoryPath = getRepositoryPath();
                    // TODO verify from user that interpreted paths are ok
                    Core.createBackup(checkListPath, repositoryPath);
                } else if (mainMenuChoice.equals("V")) {
                    boolean fastVerification = getVerificationChoice();
                    String checkListPath = getCheckListPath();
                    String repositoryPath = getRepositoryPath();
                    Core.verifyBackup(checkListPath, repositoryPath, fastVerification);
                } else if (mainMenuChoice.equals("R")) {
                    String repositoryPath = getRepositoryPath();
                    String restoreLocation = getRestoreLocation();
                    Core.restoreBackup(repositoryPath, restoreLocation);
                } else if (mainMenuChoice.equals("D")) {
                    String repositoryPath = getRepositoryPath();
                    int mbThreshold = getMbThreshold();
                    Core.detectDuplicates(repositoryPath, mbThreshold);
                } else if (mainMenuChoice.equals("C")) {
                    System.out.println("Please enter path for snapshot 1/2 (e.g. E:\\backup\\filepath-snapshots\\snapshot-2018-12-31.txt");
                    String snapshot1 = getSnapshotPath();
                    System.out.println("Please enter path for snapshot 2/2 (e.g. E:\\backup\\filepath-snapshots\\snapshot-2018-12-31.txt");
                    String snapshot2 = getSnapshotPath();
                    Core.compareSnapshots(snapshot1, snapshot2);
                } else if (mainMenuChoice.equals("H")) {
                    printHelp();
                } else if (mainMenuChoice.equals("Q")) {
                    // Exit
                    return;
                } else {
                    throw new IllegalArgumentException("Invalid input! Choose from the presented options.");
                }
            } catch (IllegalArgumentException ex) {
                // Whenever the user gives invalid input, we show an error message and jump back to main menu.
                System.out.println(ex.getMessage());
            }
        }
    }

    private String getCheckListPath() {
        System.out.println("Please enter path to checklist file. If you need more instructions, enter 'help'.");
        String checkListPath = scanner.nextLine();
        if (checkListPath.equals("help")) {
            printCheckListHelp();
            System.out.println("Please enter path to checklist file.");
            checkListPath = scanner.nextLine();
        }
        if (!new File(checkListPath).isFile()) {
            throw new IllegalArgumentException("Error! Given path does not correspond to a checklist file: " + new File(checkListPath).getAbsolutePath());
        }
        return checkListPath;
    }

    private String getRepositoryPath() {
        System.out.println("Please enter path to backup repository (where your backup is stored, e.g. E:\\backup).");
        String repositoryPath = scanner.nextLine();
        if (!new File(repositoryPath).isDirectory()) {
            throw new IllegalArgumentException("Error! Given path is not an existing directory: " + new File(repositoryPath).getAbsolutePath());
        }
        return repositoryPath;
    }

    private int getMbThreshold() {
        System.out.println("MB threshold for duplicates so we don't complain about small duplicates? (e.g. 100)");
        return Integer.parseInt(scanner.nextLine());
    }

    private boolean getVerificationChoice() {
        System.out.println("You can choose either fast or slow verification.");
        System.out.println("Fast verification is likely to uncover typical errors.");
        System.out.println("Slow verification is more comprehensive, but it can take a");
        System.out.println("very long time because file contents for all files are read from disk and");
        System.out.println("compared for both original files and copies in the backup repository.");
        System.out.println("1: Fast verification");
        System.out.println("2: Slow verification");
        String verificationChoice = scanner.nextLine();
        if (verificationChoice.equals("1")) {
            return true;
        } else if (verificationChoice.equals("2")) {
            return false;
        } else {
            throw new IllegalArgumentException("Error! Input must be either '1' or '2'.");
        }
    }

    private String getSnapshotPath() {
        String snapshotPath = scanner.nextLine();
        if (!new File(snapshotPath).isFile()) {
            throw new IllegalArgumentException("Error! Given path is not an existing file: " + new File(snapshotPath).getAbsolutePath());
        }
        return snapshotPath;
    }

    private String getRestoreLocation() {
        System.out.println("Where do you wish to restore files? e.g. C:\\restored-from-backup");
        String restoreLocation = scanner.nextLine();
        if (!new File(restoreLocation).isDirectory()) {
            throw new IllegalArgumentException("Error! Restore location does not point to an existing directory." + new File(restoreLocation).getAbsolutePath());
        }
        return restoreLocation;
    }

    private void printHelp() {
        System.out.println("This program is intended for periodic backups of your personal data.");
        System.out.println("First, create a checklist of paths that you wish to backup.");
        printCheckListHelp();
        System.out.println("Once you have a checklist, this program can backup the files found in checklist paths.");
        System.out.println("For more information, visit www.attejuvonen.fi/simple-backup-for-paranoid-people");
    }

    private void printCheckListHelp() {
        System.out.println("A checklist file contains paths to the files or folders that you wish to backup/verify.");
        System.out.println("An example checklist file is available in " + new File("backup-checklist-example.txt").getAbsolutePath());
        System.out.println("It's a good idea to copy the example file and modify it.");
        System.out.println("If you instead decide to create a new file, remember to save it with UTF-8 encoding.");
    }
}
