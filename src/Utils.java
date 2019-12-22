import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
}
