import java.io.*;
import java.security.NoSuchAlgorithmException;

public class Main {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        System.out.println("hello world " + System.getProperty("user.dir"));

        // With arguments: run a specific action
        if (args.length >= 2) {
            Core.createBackup(args[0], args[1]);
            return;
        }

        // Without arguments: enter command line UI
        UI ui = new UI();
        ui.launch();
    }
}

