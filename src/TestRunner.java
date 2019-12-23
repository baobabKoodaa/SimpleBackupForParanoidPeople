import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class TestRunner {

    File testFile = new File("temp" + File.separator + "test.txt");
    File testFile2 = new File("temp" + File.separator + "test2.txt");
    File testFile3 = new File("temp" + File.separator + "test3.txt");

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        System.out.println("Running tests...");
        TestRunner testRunner = new TestRunner();
        testRunner.runTests();
    }

    private static void verify(Boolean bool, String errorMessage) {
        if (!bool) throw new AssertionError(errorMessage);
    }

    public void runTests() throws IOException, NoSuchAlgorithmException {
        createTestFilesIFNeeded();
        testSha256();
        testCopyWorksInExpectedCase();
        testCopyFailsWhenTargetFileExists();
        System.out.println("************* A-OK! All tests completed successfully. *********************");
    }

    private void testSha256() throws IOException, NoSuchAlgorithmException {
        verify(Utils.sha256(testFile).equals("G2+wiXqQEzErf83zrdFlMsShcXwf64nUG5m3z1dPrXQ="), "Sha256 of testFile does not match expected (hardcoded) value.");
        verify(!Utils.sha256(testFile).equals(Utils.sha256(testFile2)), "Sha256 function returns same hash for 2 different files.");
        verify(Utils.sha256(testFile).equals(Utils.sha256(testFile3)), "Sha256 function does not return the same hash for 2 files with identical content.");
    }

    private void createTestFilesIFNeeded() throws IOException {
        createTestFileIFNotExist(testFile, "This file exists to test I/O operations.");
        createTestFileIFNotExist(testFile2, "This file exists to test I/O operations. It has slightly different content than the first test file.");
        // testFile3 has same content as testFile to verify that sha256 calculation returns same hash for both files.
        createTestFileIFNotExist(testFile3, "This file exists to test I/O operations.");
    }

    private void createTestFileIFNotExist(File file, String content) throws IOException {
        if (!file.exists()) {
            System.out.println("    Writing test file to " + file.getAbsolutePath());
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
        }
    }

    private void testCopyWorksInExpectedCase() throws IOException, NoSuchAlgorithmException {
        System.out.println("Testing copy");
        File copyOfTestFile = new File("temp" + File.separator + "test-" + Utils.timestamp() + ".txt");
        System.out.println("    Copying test file");
        System.out.println("        from " + testFile.getAbsolutePath());
        System.out.println("        to " + copyOfTestFile.getAbsolutePath());
        Utils.copy(testFile, copyOfTestFile);
        verify(Utils.sha256(testFile).equals(Utils.sha256(copyOfTestFile)), "Copied file does not have same content as original!");
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
