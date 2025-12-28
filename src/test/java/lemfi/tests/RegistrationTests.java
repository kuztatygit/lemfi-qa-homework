package lemfi.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.qameta.allure.*;
import io.restassured.response.Response;
import lemfi.dao.user.User;
import lemfi.dao.user.UserDao;
import lemfi.helper.AllureLogAttachments;
import lemfi.helper.ApiErrorAssertions;
import lemfi.model.signUp.Registration;
import lemfi.model.signUp.SignUpRequester;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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
@Epic("Auth API")
@Feature("User registration")
public class RegistrationTests {

    @Autowired
    UserDao userDao;

    private final List<String> emailsToCleanup = new ArrayList<>();

    @AfterEach
    void cleanup() {
        Allure.step("Cleanup users created in tests", () -> {
            for (String email : emailsToCleanup) {
                try {
                    log.info("Cleanup user with email: {}", email);
                    userDao.deleteByEmail(email);
                } catch (Exception e) {
                    log.warn("Cleanup failed for email={}: {}", email, e.getMessage(), e);
                }
            }
            emailsToCleanup.clear();
        });

        AllureLogAttachments.attachTestLogIfExists();
    }

    @Test
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /signup: registration succeeds and user is saved to DB")
    void registration() throws JsonProcessingException {
        Registration registration = Allure.step("Prepare random registration payload", () -> {
            Registration r = Registration.ofRandom();
            emailsToCleanup.add(r.getEmail());

            Allure.parameter("registeredEmail", r.getEmail());
            log.info("Check registration user. Email: {}", r.getEmail());

            assertNotNull(r.getEmail(), "Email must not be null");
            return r;
        });

        Response response = Allure.step("Call POST /signup", () -> {
            Response r = SignUpRequester.signUp(registration);
            Allure.addAttachment("POST /signup response body", "application/json", r.getBody().asPrettyString(), ".json");
            return r;
        });

        Allure.step("Assert response status code", () -> {
            assertEquals(200, response.getStatusCode());
        });

        User user = Allure.step("Read user from DB by email", () -> {
            User u = userDao.selectUser(registration.getEmail());
            return u;
        });

        Allure.step("Assert DB user fields", () -> {
            assertAll(
                    () -> assertNotNull(user, "User must exist in DB"),
                    () -> assertEquals(registration.getEmail(), user.getEmail(),
                            "Email stored in DB must match registration email"),
                    () -> assertNotNull(user.getId(), "User id must be generated"),
                    () -> assertTrue(user.getId() > 0, "User id must be > 0"),
                    () -> assertNotNull(user.getPassword(), "Password/hash must be saved"),
                    () -> assertFalse(user.getPassword().isBlank(), "Password/hash must not be blank")
            );
        });
    }

    @Test
    @Story("Duplicate registration")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /signup: returns 400 when user already exists")
    void registrationUserAlreadyExist() throws JsonProcessingException {
        Registration registration = Allure.step("Prepare random registration payload", () -> {
            Registration r = Registration.ofRandom();
            emailsToCleanup.add(r.getEmail());
            Allure.parameter("duplicateEmail", r.getEmail());
            return r;
        });

        Response firstResponse = Allure.step("Call POST /signup (first time)", () -> {
            Response r = SignUpRequester.signUp(registration);
            Allure.addAttachment("First signup response", "application/json", r.getBody().asPrettyString(), ".json");
            return r;
        });

        Response secondResponse = Allure.step("Call POST /signup (second time, expect fail)", () -> {
            Response r = SignUpRequester.signUp(registration);
            Allure.addAttachment("Second signup response", "application/json", r.getBody().asPrettyString(), ".json");
            return r;
        });

        Allure.step("Assert duplicate registration response", () -> {
            assertAll(
                    () -> assertEquals(200, firstResponse.getStatusCode()),
                    () -> assertEquals(400, secondResponse.getStatusCode()),
                    () -> assertEquals(
                            "Email already exists",
                            secondResponse.jsonPath().getString("message")
                    )
            );
        });
    }

    @Test
    @Story("Validation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /signup: returns 400 when body is missing")
    void registrationWithoutBody() throws JsonProcessingException {
        log.info("Check registration without body");

        Response response = Allure.step("Call POST /signup without body", () -> {
            Response r = SignUpRequester.signUpWithoutBody();
            Allure.addAttachment("Signup without body response", "application/json", r.getBody().asPrettyString(), ".json");
            return r;
        });

        Allure.step("Assert response", () -> {
            assertEquals(400, response.getStatusCode());
            assertEquals("Body is required", response.jsonPath().getString("message"));
        });
    }

    @ParameterizedTest(name = "Registration should fail with invalid email: \"{0}\"")
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
    @Story("Validation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /signup: invalid email validation")
    void registrationWithInvalidEmail(String email) throws JsonProcessingException {
        checkInvalidRegistration(
                b -> b.email(email),
                "Invalid email"
        );
    }

    @ParameterizedTest(name = "Registration should fail with invalid password: \"{0}\"")
    @ValueSource(strings = {
            "1234567",
            "12345678",
            "abcdefgh",
            "password1",
            "",
            " "
    })
    @NullSource
    @Story("Validation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /signup: invalid password validation")
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

        Allure.step("Negative case: " + expectedMessage, () -> {
            ApiErrorAssertions.ApiNegativeAsserts.assertBadRequest(
                    () -> Registration.ofRandom().toBuilder(),
                    modifier,
                    Registration.RegistrationBuilder::build,
                    req -> {
                        Response r = null;
                        try {
                            r = SignUpRequester.signUp(req);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        Allure.addAttachment("Negative signup response", "application/json", r.getBody().asPrettyString(), ".json");
                        return r;
                    },
                    expectedMessage
            );
        });
    }
}
