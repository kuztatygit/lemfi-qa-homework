package lemfi.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import lemfi.dao.user.User;
import lemfi.dao.user.UserDao;
import lemfi.helper.ApiErrorAssertions;
import lemfi.model.signUp.Registration;
import lemfi.model.signUp.SignUpRequester;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class RegistrationTests {
    @Autowired
    UserDao userDao;
    private final List<String> emailsToCleanup = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String email : emailsToCleanup) {
            log.info("Cleanup user with email: {}", email);
            userDao.deleteByEmail(email);
        }
        emailsToCleanup.clear();
    }

    @Test
    void registration() throws JsonProcessingException {
        Registration registration = Registration.ofRandom();
        emailsToCleanup.add(registration.getEmail());
        log.info("Check registration user. Email: {}", registration.getEmail());

        Response response = SignUpRequester.signUp(registration);
        assertEquals(200, response.getStatusCode());

        User user = userDao.selectUser(registration.getEmail());

        assertAll(
                () -> assertNotNull(user, "User must exist in DB"),
                () -> assertEquals(registration.getEmail(), user.getEmail(), "Email stored in DB must match registration email"),
                () -> assertNotNull(user.getId(), "User id must be generated"),
                () -> assertTrue(user.getId() > 0, "User id must be > 0"),
                () -> assertNotNull(user.getPassword(), "Password/hash must be saved"),
                () -> assertFalse(user.getPassword().isBlank(), "Password/hash must not be blank"));
    }

    @Test
    void registrationUserAlreadyExist() throws JsonProcessingException {
        Registration registration = Registration.ofRandom();
        emailsToCleanup.add(registration.getEmail());

        Response firstResponse = SignUpRequester.signUp(registration);
        Response secondResponse = SignUpRequester.signUp(registration);
        assertAll(
                () -> assertEquals(200, firstResponse.getStatusCode()),
                () -> assertEquals(400, secondResponse.getStatusCode()),
                () -> assertEquals(
                        "Email already exists",
                        secondResponse.jsonPath().getString("message")
                )
        );
    }

    @Test
    void registrationWithoutBody() throws JsonProcessingException {
        log.info("Check registration without body");
        Response response = SignUpRequester.signUpWithoutBody();
        assertEquals(400, response.getStatusCode());
        assertEquals("Body is required", response.jsonPath().getString("message"));
    }

    @ParameterizedTest(
            name = "Registration should fail with invalid email: \"{0}\""
    )
    @ValueSource(strings = {
            "test@@gmail.com",
            "test",
            "test@gmail",
            "te st@gmail",
            " test@gmail.com",
            "",
            " "
    })
    @NullSource
    void registrationWithInvalidEmail(String email) throws JsonProcessingException {
        checkInvalidRegistration(
                b -> b.email(email),
                "Invalid email"
        );
    }

    @ParameterizedTest(
            name = "Registration should fail with invalid password: \"{0}\""
    )
    @ValueSource(strings = {
            "1234567",
            "12345678",
            "abcdefgh",
            "password1",
            "",
            " "
    })
    @NullSource
    void registrationWithInvalidPassword(String password) throws JsonProcessingException {
        checkInvalidRegistration(
                b -> b.password(password),
                "Invalid password"
        );
    }

    @SneakyThrows
    private void checkInvalidRegistration(
            Consumer<Registration.RegistrationBuilder> modifier,
            String expectedMessage
    ) throws JsonProcessingException {

        ApiErrorAssertions.ApiNegativeAsserts.assertBadRequest(
                () -> Registration.ofRandom().toBuilder(),
                modifier,
                Registration.RegistrationBuilder::build,
                SignUpRequester::signUp,
                expectedMessage
        );
    }
}