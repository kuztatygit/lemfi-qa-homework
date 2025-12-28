package lemfi.helper;

import io.qameta.allure.Allure;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AllureLogAttachments {

    private AllureLogAttachments() {
    }

    public static void attachTestLogIfExists() {
        try {
            Path logPath = Path.of("build/test.log");
            if (Files.exists(logPath)) {
                Allure.addAttachment("test.log", "text/plain",
                        Files.readString(logPath), ".log");
            }
        } catch (Exception ignored) {

        }
    }
}
