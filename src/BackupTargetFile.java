import java.nio.file.Path;

/** Helper class to facilitate efficient file traversal and progress indication. */
class BackupTargetFile implements Comparable<BackupTargetFile> {
    Path originPath;
    long sizeBytes; // It's ok if size is not always 100% accurate, we use it to measure progress etc.

    public BackupTargetFile(Path originPath, long sizeBytes) {
        this.originPath = originPath;
        this.sizeBytes = sizeBytes;
    }

    // If originPath is the same but size is different, it indicates that the file was changed.
    // In any case we only want to keep a single BackupTargetFile per originPath
    // Therefore, two files are considered equal if their originPath is equal.

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

    @Override
    public int compareTo(BackupTargetFile o) {
        return originPath.toString().compareTo(o.originPath.toString());
    }
}