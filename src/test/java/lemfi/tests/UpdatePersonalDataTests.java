package lemfi.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.qameta.allure.*;
import io.restassured.response.Response;
import lemfi.dao.user.User;
import lemfi.dao.user.UserDao;
import lemfi.helper.AllureLogAttachments;
import lemfi.helper.ApiErrorAssertions;
import lemfi.helper.UserRegistrationStep;
import lemfi.model.personalDataUpdate.PersonalData;
import lemfi.model.personalDataUpdate.PersonalDataRequester;
import lemfi.model.signUp.Registration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Epic("User profile API")
@Feature("Update personal data")
public class UpdatePersonalDataTests {

    @Autowired
    UserDao userDao;

    private String emailToCleanup;
    private Registration registration;

    @BeforeEach
    void init() throws JsonProcessingException {
        Allure.step("Register random user", () -> {
            UserRegistrationStep.RegistrationResult result = UserRegistrationStep.registerRandomUser();
            registration = result.request();
            emailToCleanup = registration.getEmail();

            Allure.parameter("registeredEmail", emailToCleanup);
            log.info("Registered user email={}", emailToCleanup);

            assertNotNull(emailToCleanup, "Registered email must not be null");
            assertFalse(emailToCleanup.isBlank(), "Registered email must not be blank");
        });
    }

    @AfterEach
    void cleanup() {
        Allure.step("Cleanup user by email", () -> {
            if (emailToCleanup != null && !emailToCleanup.isBlank()) {
                try {
                    log.info("Cleanup user with email={}", emailToCleanup);
                    userDao.deleteByEmail(emailToCleanup);
                } catch (Exception e) {
                    log.warn("Cleanup failed for email={}: {}", emailToCleanup, e.getMessage(), e);
                }
            }
        });

        AllureLogAttachments.attachTestLogIfExists();
    }

    @Test
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PUT /personal-data: updates personal data and persists it to DB (including second update)")
    void updatePersonalData() throws JsonProcessingException {

        PersonalData personalData = Allure.step("Prepare random personal data payload #1", () -> {
            PersonalData pd = PersonalData.ofRandom();
            Allure.parameter("personalData.firstName.1", pd.getFirstName());
            Allure.parameter("personalData.surname.1", pd.getSurname());
            Allure.parameter("personalData.personalId.1", pd.getPersonalId());
            return pd;
        });

        Response addPersonalDataResponse = Allure.step("Call updatePersonalData (first time)", () -> {
            Response r = PersonalDataRequester.updatePersonalData(personalData);
            Allure.addAttachment("Update personal data #1 response", "application/json", r.getBody().asPrettyString(), ".json");
            return r;
        });

        Allure.step("Assert first update response", () -> {
            assertEquals(200, addPersonalDataResponse.getStatusCode(),
                    "Personal data update must succeed");
            assertPersonalDataResponse(addPersonalDataResponse, personalData);
        });

        User user = Allure.step("Read user from DB after first update", () -> userDao.selectUser(registration.getEmail()));

        Allure.step("Assert first update is persisted in DB", () -> {
            assertPersonalDataInDb(user, personalData);
        });

        PersonalData updatePersonalData = Allure.step("Prepare random personal data payload #2", () -> {
            PersonalData pd = PersonalData.ofRandom();
            Allure.parameter("personalData.firstName.2", pd.getFirstName());
            Allure.parameter("personalData.surname.2", pd.getSurname());
            Allure.parameter("personalData.personalId.2", pd.getPersonalId());
            return pd;
        });

        Response updatePersonalDataResponse = Allure.step("Call updatePersonalData (second time)", () -> {
            Response r = PersonalDataRequester.updatePersonalData(updatePersonalData);
            Allure.addAttachment("Update personal data #2 response", "application/json", r.getBody().asPrettyString(), ".json");
            return r;
        });

        Allure.step("Assert second update response", () -> {
            assertEquals(200, updatePersonalDataResponse.getStatusCode(),
                    "Personal data update must succeed");
            assertPersonalDataResponse(updatePersonalDataResponse, updatePersonalData);
        });

        User updatedUser = Allure.step("Read user from DB after second update", () -> userDao.selectUser(registration.getEmail()));

        Allure.step("Assert second update is persisted in DB", () -> {
            assertPersonalDataInDb(updatedUser, updatePersonalData);
        });
    }

    @Test
    @Story("Validation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PUT /personal-data: returns 400 when body is missing")
    void updatePersonalDataWithoutBody() throws JsonProcessingException {
        Response addPersonalDataResponse = Allure.step("Call updatePersonalData without body", () -> {
            Response r = PersonalDataRequester.updatePersonalDataWithoutBody();
            Allure.addAttachment("Update personal data without body response", "application/json", r.getBody().asPrettyString(), ".json");
            return r;
        });

        Allure.step("Assert response", () -> {
            assertEquals(400, addPersonalDataResponse.getStatusCode());
            assertEquals("Body is required", addPersonalDataResponse.jsonPath().getString("message"));
        });
    }

    @ParameterizedTest(name = "Update personal Data with invalid firstName: \"{0}\"")
    @ValueSource(strings = {"12345", "test1", "", " "})
    @NullSource
    @Story("Validation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PUT /personal-data: invalid firstName validation")
    void updateDataWithInvalidFirstName(String firstName) throws JsonProcessingException {
        checkInvalidPersonalData(
                b -> b.firstName(firstName), "Invalid firstName");
    }

    @ParameterizedTest(name = "Update personal Data with invalid surname: \"{0}\"")
    @ValueSource(strings = {"12345", "test1", "", " "})
    @NullSource
    @Story("Validation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PUT /personal-data: invalid surname validation")
    void updateDataWithInvalidSurname(String surname) throws JsonProcessingException {
        checkInvalidPersonalData(
                b -> b.surname(surname), "Invalid surname");
    }

    @ParameterizedTest(name = "Update personal Data with invalid personalId: \"{0}\"")
    @ValueSource(longs = {0L, -1L, 12L})
    @NullSource
    @Story("Validation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("PUT /personal-data: invalid personalId validation")
    void updateDataWithInvalidPersonalId(Long personalId) throws JsonProcessingException {
        checkInvalidPersonalData(
                b -> b.personalId(personalId), "Invalid personalId");
    }

    @SneakyThrows
    private void checkInvalidPersonalData(
            Consumer<PersonalData.PersonalDataBuilder> modifier,
            String expectedMessage) {

        Allure.step("Negative case: " + expectedMessage, () -> {
            ApiErrorAssertions.ApiNegativeAsserts.assertBadRequest(
                    () -> PersonalData.ofRandom().toBuilder(),
                    modifier,
                    PersonalData.PersonalDataBuilder::build,
                    req -> {
                        Response r = null;
                        try {
                            r = PersonalDataRequester.updatePersonalData(req);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        Allure.addAttachment("Negative update personal data response", "application/json", r.getBody().asPrettyString(), ".json");
                        return r;
                    },
                    expectedMessage
            );
        });
    }

    private void assertPersonalDataResponse(Response response, PersonalData expectedPersonalData) {
        assertAll(
                () -> assertEquals(expectedPersonalData.getFirstName(), response.jsonPath().getString("firstName"),
                        "First name in response must match personal data request"),
                () -> assertEquals(expectedPersonalData.getSurname(), response.jsonPath().getString("surname"),
                        "Surname in response must match personal data request"),
                () -> assertEquals(expectedPersonalData.getPersonalId(), response.jsonPath().getLong("personalId"),
                        "Personal ID in response must match personal data request"));
    }

    private void assertPersonalDataInDb(User user, PersonalData expected) {
        assertAll(
                () -> assertNotNull(user, "User must exist in DB"),
                () -> assertEquals(registration.getEmail(), user.getEmail(), "Email in DB must match registered user email"),
                () -> assertEquals(expected.getFirstName(), user.getFirstName(), "First name in DB must match personal data request"),
                () -> assertEquals(expected.getSurname(), user.getSurname(), "Surname in DB must match personal data request"),
                () -> assertEquals(expected.getPersonalId(), user.getPersonalId(), "Personal ID in DB must match personal data request")
        );
    }
}
