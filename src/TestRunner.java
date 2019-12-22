import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TestRunner {

    File testFile = new File("temp" + File.separator + "test.txt");

    public static void main(String[] args) throws IOException {
        System.out.println("Running tests...");
        TestRunner testRunner = new TestRunner();
        testRunner.runTests();
    }

    public void runTests() throws IOException {
        createTestFileIFNeeded();
        testCopyWorksInExpectedCase();
        testCopyFailsWhenTargetFileExists();
    }

    private void createTestFileIFNeeded() throws IOException {
        if (!testFile.exists()) {
            System.out.println("    Writing test file to " + testFile.getAbsolutePath());
            testFile.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(testFile);
            writer.write("This file exists to test I/O operations.\n");
            writer.close();
        }
    }

    private void testCopyWorksInExpectedCase() throws IOException {
        System.out.println("Testing copy");
        File copyOfTestFile = new File("temp" + File.separator + "test-" + Utils.timestamp() + ".txt");
        System.out.println("    Copying test file");
        System.out.println("        from " + testFile.getAbsolutePath());
        System.out.println("        to " + copyOfTestFile.getAbsolutePath());
        Utils.copy(testFile, copyOfTestFile);
        // TODO compare sha256 of original and copied file
    }

    private void testCopyFailsWhenTargetFileExists() throws IOException {
        File copyOfTestFile = new File("temp" + File.separator + "test-" + Utils.timestamp() + ".txt");
        System.out.println("    Verifying that existing files can't be overwritten with copy");
        try {
            Utils.copy(testFile, copyOfTestFile);
            Utils.copy(testFile, copyOfTestFile);
        } catch (IOException ex) {
            // We want exception to be thrown in this case.
            return;
        }
        // If no exception was thrown, there is a bug somewhere.
        throw new IOException("Error! Copy should fail when target file exists, but it did not fail during a test.");
    }

    // TODO prevent copy failure when output path is set incorrectly and copy "fails if the target file already exists or is a symbolic link, except if the source and target are the same file, in which case the method completes without copying the file."
    // TODO document that symbolic links are followed to the final destination
    // TODO deal with ioexception; "possible that the target file is incomplete or some of its file attributes have not been copied from the source file"
    // TODO document that file attributes are copied on a "best effort" basis
    //      - The copy library attempts to copy the file attributes associated with this file to the target file. The exact file attributes that are copied is platform and file system dependent and therefore unspecified. Minimally, the last-modified-time is copied to the target file if supported by both the source and target file store. Copying of file timestamps may result in precision loss.
    //      - In addition, if 2 files have the same contents, the file is copied only once, so if the files have different attributes, only one file's attributes are copied.
}
