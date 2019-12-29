import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.UnexpectedException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        System.out.println("hello world " + System.getProperty("user.dir"));
        Scanner scanner = new Scanner(System.in);

        // With arguments: run a specific action
        if (args.length >= 2) {
            createBackup(args[0], args[1]);
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
                createBackup(checkListPath, repositoryPath);
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

    public static void createBackup(String checkListFilePath, String repositoryPath) throws IOException, NoSuchAlgorithmException {
        String timestampAtStart = Utils.timestamp();
        Set<BackupTargetFile> allTargets = collectAllFilesFromCheckListTargetPaths(checkListFilePath);
        printBackupSizeInfo(allTargets);
        File snapshotFile = initializeSnapshot(repositoryPath, timestampAtStart);
        File repoFilesDir = initializeFilesDirectory(repositoryPath);
        // TODO load known set of hashes from files directory filenames
        try (BufferedWriter snapshotWriter = new BufferedWriter(new FileWriter(snapshotFile, StandardCharsets.UTF_8))) {
            for (BackupTargetFile btf : allTargets) {
                String hash = Utils.sha256(btf.originPath.toFile());
                // TODO file (write-)lock from beginning of SHA to the end of copy?
                // TODO check if hash already exists in known set
                // TODO add hash to known set
                File originalFile = btf.originPath.toFile();
                File copyOfFile = new File(repoFilesDir.getAbsolutePath() + File.separator + "temp-" + hash + "-" + timestampAtStart + ".tmp");
                if (copyOfFile.exists()) {
                    throw new UnexpectedException("We were about to copy a file to a temporary path, but the path already has an existing file." +
                            "As a precaution we do not overwrite the path: " + copyOfFile.getAbsolutePath() +
                            "\nThis error might occur if the clock in your computer is not operating normally or if this software has a bug.");
                }
                Utils.copy(originalFile, copyOfFile);

                // TODO once copy is finished rename file (safely) into backupDir\files\<hash>
                snapshotWriter.write(btf.originPath.toString() + Utils.SEPARATOR_BETWEEN_PATH_AND_HASH + hash + "\n");
                // TODO progress indication based on both number of files and total size
            }
        }
    }

    public static File initializeSnapshot(String repositoryPath, String timestamp) throws UnexpectedException {
        File snapshotFile = new File(repositoryPath + File.separator + "snapshot-" + timestamp + ".txt");
        if (snapshotFile.exists()) {
            // Safety precaution
            throw new UnexpectedException(
                    "Unable to create snapshot file, because file already exists: "
                            + snapshotFile.getAbsolutePath()
                            + "\nCheck that your computer's clock is operating normally and try again."
            );
        }
        System.out.println("Creating snapshot file in " + snapshotFile.getAbsolutePath());
        return snapshotFile;
    }

    public static File initializeFilesDirectory(String repositoryPath) {
        File repoFilesDir = new File(repositoryPath + File.separator + "files");
        repoFilesDir.mkdirs();
        return repoFilesDir;
    }

    public static void printBackupSizeInfo(Set<BackupTargetFile> allTargets) {
        long totalBytesNeeded = 0;
        for (BackupTargetFile btf : allTargets) {
            totalBytesNeeded += btf.sizeBytes;
        }
        System.out.println("Number of target files to backup: " + allTargets.size() + ", totaling " + Utils.formatSize(totalBytesNeeded));
    }

    public static Set<BackupTargetFile> collectAllFilesFromCheckListTargetPaths(String checkListFilePath) throws IOException {
        Set<BackupTargetFile> allTargets = new HashSet<>();
        Set<String> targetPathStrings = getTargetPathStringsFromCheckList(checkListFilePath);
        final AtomicLong counter = new AtomicLong();
        for (String targetPathString : targetPathStrings) {
            Path targetPath = new File(targetPathString).toPath();
            collectAllFilesFromTargetPath(targetPath, allTargets, counter);
        }
        return allTargets;
    }

    public static Set<String> getTargetPathStringsFromCheckList(String checkListFilePath) throws IOException {
        Set<String> targetPathStrings = new HashSet<>();
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
                if (line == null)
                    break;
                line = line.replace("\uFEFF", ""); // Remove UTF-8 BOM
                line = line.replace(" ", "%20");
                targetPathStrings.add(line);
            }
        }
        return targetPathStrings;
    }

    public static void collectAllFilesFromTargetPath(Path targetPath, Set<BackupTargetFile> allTargets, AtomicLong counter) throws IOException {
        try (Stream<Path> pathStream = Files.walk(targetPath)) {
            Set<BackupTargetFile> targets = pathStream
                    .peek(stat -> {
                        if (counter.incrementAndGet() % 20000 == 0) {
                            // TODO use systemTime to avoid spamming too many messages?
                            System.out.println(counter.get() + " files discovered.");
                        }
                    })
                    .filter(path -> !path.toFile().isDirectory())
                    .map(path -> new BackupTargetFile(path, path.toFile().length()))
                    .collect(Collectors.toSet());
            allTargets.addAll(targets);
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

class BackupTargetFile {
    Path originPath;
    long sizeBytes; // It's ok if size is not always 100% accurate, we use it to measure progress etc.

    public BackupTargetFile(Path originPath, long sizeBytes) {
        this.originPath = originPath;
        this.sizeBytes = sizeBytes;
    }

    // Two files are considered equal if their originPath is equal.
    // If originPath is the same but size is different, it indicates that the file was changed.
    // In any case we only want to keep a single BackupTargetFile per originPath.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackupTargetFile that = (BackupTargetFile) o;
        return originPath.equals(that.originPath);
    }

    @Override
    public int hashCode() {
        return originPath.toString().hashCode();
    }
}