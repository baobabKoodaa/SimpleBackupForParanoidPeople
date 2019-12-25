import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.net.URI;

public class Main {

    public static void main(String[] args) {
        System.out.println("hello world " + System.getProperty("user.dir"));
        Scanner scanner = new Scanner(System.in);

        // With arguments: run a specific action
        if (args.length > 0) {
            createBackup(args[0]);
            return;
        }

        // Without arguments: enter command line UI
        while (true) {

            // Print options
            System.out.println("This is SimpleBackupForParanoidPeople. What would you like to do?");
            //TODO: 0: backup to <previous-target> using <previous-checklist>
            System.out.println("1: Create backup from checklist");
            System.out.println("2: Verify backup against checklist");
            System.out.println("3: Restore files from backup");
            System.out.println("4: Exit");

            // Parse user input
            String userInput = scanner.next();
            int choice = 0;
            try {
                choice = Integer.parseInt(userInput);
            } catch (Exception ex) {
                System.out.println("Invalid input! You must enter a number.");
                continue;
            }

            // Execute user input
            if (choice == 1) {
                System.out.println("Please enter path to checklist file. If you need more instructions, enter 'help'.");
                String checkListPath = scanner.next();
                if (checkListPath.equals("help")) {
                    System.out.println("A checklist file contains paths to the files or folders that you wish to backup.");
                    System.out.println("An example checklist file is available in " + new File("backup-checklist-example.txt").getAbsolutePath());
                    System.out.println("It's a good idea to copy the example file and modify it.");
                    System.out.println("If you instead decide to create a new file, remember to save it with UTF-8 encoding.");
                    System.out.println("Please enter path to checklist file.");
                    checkListPath = scanner.next();
                }
                createBackup(checkListPath);
            } else if (choice == 2) {

            } else if (choice == 3) {

            } else if (choice == 4) {
                return;
            } else {
                System.out.println("Invalid input! Number out of range.");
                continue;
            }
        }
    }

    public static void createBackup(String checkListFilePath) {
        File checkListFile = new File(checkListFilePath);
        if (!checkListFile.exists()) {
            throw new IllegalArgumentException("File not found: " + checkListFile.getAbsolutePath());
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(checkListFile), "UTF-8"))) {
            System.out.println("Walking through file trees... This may be very fast or very slow depending on two things: the amount of files in checklist, and whether your OS has indexed the files or if it has to read them from disk.");
            while (true) {
                // Each line represents a root of a file tree.
                //     Example 1: "D:\music" may be a directory with many files within nested subdirectories.
                //     Example 2: "D:\song.mp3" may be a single file. It can be treated as the root of a file tree which contains 1 file. // TODO test that this doesnt include the surrounding folder etc.
                String line = br.readLine();
                if (line == null) break;
                line = line.replace("\uFEFF", ""); // Remove UTF-8 BOM
                line = line.replace(" ", "%20");

                // We want to collect all paths from the file tree represented by line.
                Stream<Path> pathStream = Files.walk(new File(line).toPath());
                final AtomicLong counter = new AtomicLong();
                Optional<Pair> jobSize = pathStream
                        .peek(stat -> {
                            if (counter.incrementAndGet() % 5000 == 0) {
                                // TODO use systemTime to avoid spamming too many messages?
                                System.out.println(counter.get() + " files discovered.");
                            }
                        })
                        .filter(path -> !path.toFile().isDirectory())
                        .map(path -> new Pair(path.toFile().length(), 1))
                        .reduce((pair1, pair2) -> new Pair(pair1.size + pair2.size, pair1.count + pair2.count));
                System.out.println("Total files in " + line + " ===> " + jobSize.get().count + " totaling size " + (jobSize.get().size * 1.0 / 1e9) + " GB");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class Pair {
    long size;
    long count;

    public Pair(long size, long count) {
        this.size = size;
        this.count = count;
    }
}