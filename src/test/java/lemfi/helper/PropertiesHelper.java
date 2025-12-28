package lemfi.helper;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class PropertiesHelper {
    public static Properties loadProperties() {
        String environment = System.getenv("ENVIROMENT");
        if (environment == null) {
            environment = "local";
        }
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("application-" + environment + "properties");
        if (stream == null) {
            throw new RuntimeException("Resource not found: application.properties");
        }
        Properties properties = new Properties();
        try {
            properties.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                stream.close();

            } catch (IOException e) {
                log.info("Failed to close inputStream");
            }
        }
        return properties;
    }
}
