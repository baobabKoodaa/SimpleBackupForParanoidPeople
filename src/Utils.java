import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

public class Utils {

    // Path target = null;//newdir.resolve(SHA256xx);

    public static void copy(File source, File target) throws IOException {
        Files.copy(source.toPath(), target.toPath(), COPY_ATTRIBUTES);
    }

    public static String timestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss");
        return LocalDateTime.now().format(formatter);
    }

    public static String sha256(File file) throws NoSuchAlgorithmException, IOException {
        // Adapted from https://stackoverflow.com/a/32032908/4490400
        byte[] buffer= new byte[8192];
        int count;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        while ((count = bis.read(buffer)) > 0) {
            digest.update(buffer, 0, count);
        }
        bis.close();

        byte[] hash = digest.digest();

        // Return hash as base64-encoded string.
        return new String(Base64.getEncoder().encode(hash));
    }
}
