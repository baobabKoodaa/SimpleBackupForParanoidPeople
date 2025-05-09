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

public class Core {

    static void createBackup(String checkListFilePath, String repositoryPath) throws IOException, NoSuchAlgorithmException {
        // TODO validate parameters correspond to valid-looking file and folder
        String timestampAtStart = Utils.timestamp();
        Set<String> targetPathStrings = getTargetPathStringsFromCheckList(checkListFilePath);
        List<BackupTargetFile> allTargets = collectAllFilesFromCheckListTargetPaths(targetPathStrings);
        List<BackupTargetFile> failedTargets = new ArrayList<>();
        Pair job = calculateJobSize(allTargets);
        System.out.println("Number of target files to backup: " + job.count + ", totaling " + Utils.formatSize(job.size));
        File snapshotFile = initializeNewSnapshotFile(repositoryPath, timestampAtStart);
        File repoFilesDir = getOrCreateRepoFilesDir(repositoryPath);
        Set<String> existing = loadKnownSetOfAlreadyBackupedUpFilesHashes(repoFilesDir);
        // TODO make a copy of checklist into backup location, with timestamp
        try (FileWriter snapshotWriter = new FileWriter(snapshotFile, StandardCharsets.UTF_8)) {
            ProgressIndicator progressIndicator = new ProgressIndicator(job.size, job.count, "files from checklist are backed up to repository");
            for (BackupTargetFile btf : allTargets) {
                // TODO file (write-)lock from beginning of SHA to the end of copy?
                try {
                    String hash = Utils.sha256(btf.originPath.toFile());
                    if (!existing.contains(hash)) {
                        // File does not already exist in backup repository, so we need to copy it.
                        // We want to copy the file under a temp name first, because the copy might fail and in that case
                        // we want the file name to indicate that this partial/failed copy is not a proper copy of the file.
                        File originalFile = btf.originPath.toFile();
                        File copyOfFile = new File(repoFilesDir.getAbsolutePath() + File.separator + "temp-" + hash + "-" + timestampAtStart + ".tmp");
                        if (copyOfFile.exists()) {
                            throw new UnexpectedException("We were about to copy a file to a temporary path, but the path already has an existing file." +
                                    "As a precaution we do not overwrite the path: " + copyOfFile.getAbsolutePath() +
                                    "\nThis error might occur if the clock in your computer is not operating normally or if this software has a bug.");
                        }
                        Utils.copy(originalFile, copyOfFile);
                        // Once copy has finished successfully, attempt to rename the file to just the hash (no extension).
                        // TODO verify that originalFilePath != copyOfFilePath != siblingPath
                        Files.move(copyOfFile.toPath(), copyOfFile.toPath().resolveSibling(hash));
                        existing.add(hash);
                    }

                    // Now that file exists in backup repository, append path/hash pair to current snapshot.
                    snapshotWriter.write(btf.originPath.toString() + Utils.SEPARATOR_BETWEEN_PATH_AND_HASH + hash + "\n");
                    snapshotWriter.flush();
                } catch (Exception ex) {
                    failedTargets.add(btf);
                    System.err.println("Unable to copy file " + btf.originPath.toAbsolutePath().toString() + " because of error " + ex.getMessage());
                    ex.printStackTrace();
                }
                progressIndicator.tick(btf.sizeBytes);
            }
            progressIndicator.done();
        }
        if (!failedTargets.isEmpty()) {
            System.out.println("Failed to copy " + failedTargets.size() + " targets:");
            for (BackupTargetFile btf : failedTargets) {
                // TODO write failures to checklist-failures.txt
                System.out.println(btf.originPath.toAbsolutePath().toString());
            }
        }
    }

    static void verifyBackup(String checkListFilePath, String repositoryPath, boolean fast) throws IOException {
        System.out.println("Verification step 1: verifying that files from checklist are found in backup repository's snapshot and files folder... (file contents will not be verified yet)");
        Set<String> targetPathStrings = getTargetPathStringsFromCheckList(checkListFilePath);
        List<BackupTargetFile> allTargets = collectAllFilesFromCheckListTargetPaths(targetPathStrings);
        File repoFilesDir = getOrCreateRepoFilesDir(repositoryPath);
        Set<String> existing = loadKnownSetOfAlreadyBackupedUpFilesHashes(repoFilesDir);
        HashMap<String, String> latestSnapshot = loadLatestSnapshotMap(repoFilesDir);
        int[] count = new int[2];
        final int FOUND = 1;
        final int NOT_FOUND = 0;
        for (BackupTargetFile btf : allTargets) {
            String fp = btf.originPath.toString();
            String hash = latestSnapshot.get(fp);
            if (hash == null) {
                System.out.println("Warning! File found from checklist path but not from backup repository's latest snapshot: " + fp);
                count[NOT_FOUND]++;
            } else if (!existing.contains(hash)) {
                System.out.println("Warning! File found from checklist path and from snapshot but not from backup repository's files folder: " + fp);
                count[NOT_FOUND]++;
            } else {
                count[FOUND]++;
            }
        }
        String foundPercent = Utils.nicePercent(count[FOUND] * 1.0 / (count[FOUND] + count[NOT_FOUND]));
        if (count[NOT_FOUND] == 0) {
            System.out.println("SUCCESS! All files in checklist paths were found in backup repository (note that file contents were not verified in this step).");
        } else {
            System.out.println("WARNING! Only " + foundPercent + " of files in checklist paths were found in backup repository (perhaps new files were created after latest backup was initiated?) (note that file contents were not verified in this step).");
        }


        System.out.println("Verification step 2: verifying file contents...");
        // TODO if fast then take a small sample of files for step2
        // TODO Recalculate SHA256 for all files in "files" folder and compare against SHA256 in filename
        // TODO Recalculate SHA256 for all files in checklist (compare against known set + verify that file is listed with correct hash in latest snapshot)

        // TODO summary of results
    }

    static void detectDuplicates(String repositoryPath, int mbThreshold) throws IOException {
        System.out.println("Detecting duplicates based on latest snapshot file...");
        File repoFilesDir = getOrCreateRepoFilesDir(repositoryPath);
        HashMap<String, String> latestSnapshot = loadLatestSnapshotMap(repoFilesDir);

        // Count duplicates
        ElementCounter reverseSnapshot = new ElementCounter();
        for (String hash : latestSnapshot.values()) {
            reverseSnapshot.add(hash);
        }
        long totalC = latestSnapshot.size();
        long uniqueC = reverseSnapshot.size();
        long duplicateC = totalC - uniqueC;

        // Collect file paths for large duplicates
        long byteThreshold = 1000000L * mbThreshold;
        Map<String, ArrayList<String>> duplicateFilePaths = new HashMap<>();
        Map<String, Long> duplicateBytes = new TreeMap<>();
        for (String fp : latestSnapshot.keySet()) {
            String hash = latestSnapshot.get(fp);
            if (reverseSnapshot.get(hash) > 1) {
                // If we are here, then this file is a duplicate
                long bytes = new File(fp).length();
                if (bytes < byteThreshold) {
                    // We don't care about small duplicates
                    continue;
                }
                // Write down size (possibly overwriting identical value)
                duplicateBytes.put(hash, bytes);
                if (!duplicateFilePaths.containsKey(hash)) {
                    // First instance of this duplicate file in this loop, so initialize list of paths
                    duplicateFilePaths.put(hash, new ArrayList<>());
                }
                // Add this path to list of file paths for this hash
                duplicateFilePaths.get(hash).add(fp);
            }
        }

        // Print file paths for large duplicates
        List<Map.Entry<String, Long>> entries = new ArrayList<>(duplicateBytes.entrySet());
        entries.sort(Map.Entry.comparingByValue());
        for (Map.Entry<String, Long> entry : entries) {
            String hash = entry.getKey();
            // Verify that more than 1 path was found (in cases where a file is deleted after creating snapshot, it's possible
            // that we find duplicates in snapshot but then don't locate all the versions of the actual file, in which
            // case there would be just one (or zero) filepaths in duplicateFilePaths
            if (duplicateFilePaths.size() < 2) {
                continue;
            }
            System.out.println();
            System.out.println(Utils.formatSize(duplicateBytes.get(hash)));
            for (String fp : duplicateFilePaths.get(hash)) {
                System.out.println("    " + fp);
            }
        }

        // Print total duplicate count, including small files
        System.out.println("Total files in snapshot: " + totalC + " (" + uniqueC + " unique) (" + duplicateC + " duplicate).");
    }

    static void restoreBackup(String repositoryPath, String restoreLocation) throws IOException {
        // Get latest snapshot
        File repoFilesDir = getOrCreateRepoFilesDir(repositoryPath);
        HashMap<String, String> latestSnapshot = loadLatestSnapshotMap(repoFilesDir);
        System.out.println("Number of repo files to restore: " + latestSnapshot.size());

        // Calculate total size for progress indicator
        List<BackupTargetFile> allFiles = collectAllFilesFromCheckListTargetPaths(Set.of(repoFilesDir.toString()));
        Pair job = calculateJobSize(allFiles);
        ProgressIndicator progressIndicator = new ProgressIndicator(job.size, job.count, "files from backup are restored");

        // Copy files
        for (String originalFilepath : latestSnapshot.keySet()) {
            String hash = latestSnapshot.get(originalFilepath);
            File backupFile = new File(repoFilesDir, hash);

            // Fix platform specific special characters
            String fixedOriginalFilepath = originalFilepath
                    .replace(":", "")
                    .replace('\\', '/')
                    .replace('/', File.separatorChar);

            // Copy as .tmp first, rename after successful copy (because an error during copy can lead to partially copied file, we want the .tmp to indicate that)
            File restoreDir = new File(restoreLocation);
            File tmpCopy = new File(restoreDir, fixedOriginalFilepath + ".tmp");
            File finalCopy = new File(restoreDir, fixedOriginalFilepath);
            tmpCopy.getParentFile().mkdirs();
            Utils.copy(backupFile, tmpCopy);
            Files.move(tmpCopy.toPath(), finalCopy.toPath());
            long sizeBytes = finalCopy.length();
            progressIndicator.tick(sizeBytes);
        }
        progressIndicator.done();
    }

    static HashMap<String, String> loadLatestSnapshotMap(File repoFilesDir) throws IOException {
        HashMap<String, String> latestSnapshotMap = new HashMap<>();
        File latestSnapshotFile = getLatestSnapshotFile(repoFilesDir);
        // TODO refactor filereaders into a method which returns list of strings.
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(latestSnapshotFile), "UTF-8"))) {
            while (true) {
                String line = br.readLine();
                if (line == null) break;
                line = line.replace("\uFEFF", ""); // Remove UTF-8 BOM
                String[] splitted = line.split(Utils.SEPARATOR_BETWEEN_PATH_AND_HASH);
                if (splitted.length != 2) {
                    System.out.println("Warning! Skipping malformed line in snapshot: " + line);
                    continue;
                }
                String filePath = splitted[0];
                String hash = splitted[1];
                latestSnapshotMap.put(filePath, hash);
            }
        }
        return latestSnapshotMap;
    }

    /** Iterate snapshots in repo, return latest snapshot file. */
    static File getLatestSnapshotFile(File repoFilesDir) throws IOException {
        File snapshotsDir = new File(repoFilesDir.getParentFile().getAbsolutePath() + File.separator + "filepath-snapshots" + File.separator);
        try (Stream<Path> pathStream = Files.walk(snapshotsDir.toPath())) {
            // Timestamp format allows us to find the latest snapshot by simply ordering filenames in alphabetical order.
            List<String> snapshotPathStrings = pathStream
                    .map(path -> path.getFileName().toString())
                    .sorted(Collections.reverseOrder())
                    .collect(Collectors.toList());
            if (snapshotPathStrings.isEmpty()) {
                throw new IllegalArgumentException("No snapshot files found in repository " + repoFilesDir.getAbsolutePath());
            }

            String latestSnapshotPathString = snapshotsDir.getAbsolutePath() + File.separator + snapshotPathStrings.get(0);
            System.out.println("Latest snapshot appears to be " + latestSnapshotPathString);
            return new File(latestSnapshotPathString);
        }
    }

    static File initializeNewSnapshotFile(String repositoryPath, String timestamp) throws IOException {
        File snapshotFile = new File(repositoryPath + File.separator + "filepath-snapshots" + File.separator + "snapshot-" + timestamp + ".txt");
        if (snapshotFile.exists()) {
            // Safety precaution
            throw new UnexpectedException(
                    "Unable to create snapshot file, because file already exists: "
                            + snapshotFile.getAbsolutePath()
                            + "\nCheck that your computer's clock is operating normally and try again."
            );
        }
        System.out.println("Creating snapshot file in " + snapshotFile.getAbsolutePath());
        snapshotFile.getParentFile().mkdirs();
        snapshotFile.createNewFile();
        return snapshotFile;
    }

    static File getOrCreateRepoFilesDir(String repositoryPath) {
        // This does nothing if the directory already exists.
        File repoFilesDir = new File(repositoryPath + File.separator + "files");
        repoFilesDir.mkdirs();
        return repoFilesDir;
    }

    static Set<String> loadKnownSetOfAlreadyBackupedUpFilesHashes(File repoFilesDir) throws IOException {
        // TODO progress indicator here
        try (Stream<Path> pathStream = Files.walk(repoFilesDir.toPath())) {
            Set<String> existing = pathStream
                    .map(path -> path.getFileName().toString())
                    .filter(s -> !s.endsWith(".tmp")) // Filter out broken copies.
                    .collect(Collectors.toSet());
            return existing;
        }
    }

    static Pair calculateJobSize(Collection<BackupTargetFile> allTargets) {
        long totalBytesNeeded = 0;
        for (BackupTargetFile btf : allTargets) {
            totalBytesNeeded += btf.sizeBytes;
        }
        return new Pair(totalBytesNeeded, allTargets.size());
    }

    static List<BackupTargetFile> collectAllFilesFromCheckListTargetPaths(Set<String> targetPathStrings) throws IOException {
        // Collect files
        Set<BackupTargetFile> allTargets = new HashSet<>();
        final AtomicLong counter = new AtomicLong();
        for (String targetPathString : targetPathStrings) {
            Path targetPath = new File(targetPathString).toPath();
            collectAllFilesFromTargetPath(targetPath, allTargets, counter);
        }

        // Convert collected files into ordered list
        List<BackupTargetFile> allTargetsAsOrderedList = new ArrayList<>(allTargets.size());
        for (BackupTargetFile btf : allTargets) {
            allTargetsAsOrderedList.add(btf);
        }
        Collections.sort(allTargetsAsOrderedList);
        return allTargetsAsOrderedList;
    }

    static Set<String> getTargetPathStringsFromCheckList(String checkListFilePath) throws IOException {
        Set<String> targetPathStrings = new HashSet<>();
        File checkListFile = new File(checkListFilePath);
        if (!checkListFile.exists()) {
            throw new IllegalArgumentException("File not found: " + checkListFile.getAbsolutePath());
        }
        // TODO refactor filereaders into a method which returns list of strings.
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(checkListFile), "UTF-8"))) {
            System.out.println("Checklist: walking through file trees... This may be very fast or very slow depending on two things: the amount of files in checklist, and whether your OS has indexed the files or if it has to read them from disk.");
            while (true) {
                // Each line represents a root of a file tree.
                //     Example 1: "D:\music" may be a directory with many files within nested subdirectories.
                //     Example 2: "D:\song.mp3" may be a single file. It can be treated as the root of a file tree which contains 1 file. // TODO test that this doesnt include the surrounding folder etc.
                String line = br.readLine();
                if (line == null)
                    break;
                line = line.replace("\uFEFF", ""); // Remove UTF-8 BOM
                targetPathStrings.add(line);
            }
        }
        return targetPathStrings;
    }

    static void collectAllFilesFromTargetPath(Path targetPath, Set<BackupTargetFile> allTargets, AtomicLong counter) throws IOException {
        try (Stream<Path> pathStream = Files.walk(targetPath)) {
            Set<BackupTargetFile> targets = pathStream
                    .peek(stat -> {
                        if (counter.incrementAndGet() % 20000 == 0) {
                            // TODO use systemTime to avoid spamming too many messages?
                            // TODO refactor into progressIndicator (although total count/size not known at this point)
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
