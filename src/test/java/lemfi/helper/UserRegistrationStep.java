package lemfi.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import lemfi.model.signUp.Registration;
import lemfi.model.signUp.SignUpRequester;
import lombok.experimental.UtilityClass;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UtilityClass
public class UserRegistrationStep {

    public record RegistrationResult(Registration request, Response response) {
    }

    public static RegistrationResult registerRandomUser() throws JsonProcessingException {
        Registration registration = Registration.ofRandom();

        Response response = SignUpRequester.signUp(registration);
        assertEquals(200, response.getStatusCode(), "User registration must succeed");

        return new RegistrationResult(registration, response);
    }
}