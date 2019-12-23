import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.net.URI;

public class Main {

    public static void main(String[] args) {
        System.out.println("hello world " + System.getProperty("user.dir"));
        if (args.length > 0) {
            createBackup(args[0]);
        }
    }

    public static void createBackup(String backupRoot) {
        File checkListFile = new File(backupRoot + File.separator + "backup-checklist.txt");
        if (!checkListFile.exists()) {
            throw new IllegalArgumentException("Target folder must contain backup-checklist.txt! Unable to find it in " + checkListFile.getAbsolutePath());
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(checkListFile), "UTF-8"))) {
            while (true) {
                // Each line represents a root of a file tree.
                //     Example 1: "D:\mp3" may be a directory with many files within nested subdirectories.
                //     Example 2: "D:\song.mp3" may be a single file.
                String line = br.readLine();
                if (line == null) break;
                line = line.replace("\uFEFF", ""); // Remove UTF-8 BOM
                line = line.replace(" ", "%20");

                // We want to collect all paths from the file tree represented by line.
                // Parallelize collection of file paths within subdirectories.
                Stream<Path> pathStream = Files.walk(new File(line).toPath());
                Set<Path> paths = pathStream.collect(Collectors.toSet()); // TODO also collect file size for each file
                for (Path p : paths) {
                    System.out.println(p);
                }
                // TODO calculate sum of file sizes and display to user in some fashion
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
