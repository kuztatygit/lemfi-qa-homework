package lemfi.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import lemfi.dao.user.User;
import lemfi.dao.user.UserDao;
import lemfi.helper.ApiErrorAssertions;
import lemfi.helper.UserRegistrationStep;
import lemfi.model.personalDataUpdate.PersonalData;
import lemfi.model.personalDataUpdate.PersonalDataRequester;
import lemfi.model.signUp.Registration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class UpdatePersonalDataTests {
    @Autowired
    UserDao userDao;
    private String emailToCleanup;
    private Registration registration;

    @BeforeEach
    void init() throws JsonProcessingException {
        UserRegistrationStep.RegistrationResult result = UserRegistrationStep.registerRandomUser();
        registration = result.request();
        emailToCleanup = registration.getEmail();
    }

    @AfterEach
    void cleanup() {
        if (emailToCleanup != null && !emailToCleanup.isBlank()) {
            userDao.deleteByEmail(emailToCleanup);
        }
    }

    @Test
    void updatePersonalData() throws JsonProcessingException {
        PersonalData personalData = PersonalData.ofRandom();
        Response addPersonalDataResponse = PersonalDataRequester.updatePersonalData(personalData);
        assertEquals(200, addPersonalDataResponse.getStatusCode(),
                "Personal data update must succeed");

        assertPersonalDataResponse(addPersonalDataResponse, personalData);
        User user = userDao.selectUser(registration.getEmail());
        assertPersonalDataInDb(user, personalData);

        PersonalData updatePersonalData = PersonalData.ofRandom();
        Response updatePersonalDataResponse = PersonalDataRequester.updatePersonalData(updatePersonalData);
        assertEquals(200, updatePersonalDataResponse.getStatusCode(),
                "Personal data update must succeed");
        User updatedUser = userDao.selectUser(registration.getEmail());

        assertPersonalDataResponse(updatePersonalDataResponse, updatePersonalData);
        assertPersonalDataInDb(updatedUser, updatePersonalData);
    }

    @Test
    void updatePersonalDataWithoutBody() throws JsonProcessingException {
        Response addPersonalDataResponse = PersonalDataRequester.updatePersonalDataWithoutBody();
        assertEquals(400, addPersonalDataResponse.getStatusCode());
        assertEquals("Body is required", addPersonalDataResponse.jsonPath().getString("message"));
    }

    @ParameterizedTest(
            name = "Update personal Data with invalid firstName: \"{0}\""
    )
    @ValueSource(strings = {"12345", "test1", "", " "})

    @NullSource
    void updateDataWithInvalidFirstName(String firstName) throws JsonProcessingException {
        checkInvalidPersonalData(
                b -> b.firstName(firstName), "Invalid firstName");
    }

    @ParameterizedTest(
            name = "Update personal Data with invalid surname: \"{0}\""
    )
    @ValueSource(strings = {"12345", "test1", "", " "})

    @NullSource
    void updateDataWithInvalidSurname(String surname) throws JsonProcessingException {
        checkInvalidPersonalData(
                b -> b.surname(surname), "Invalid surname");
    }

    @ParameterizedTest(
            name = "Update personal Data with invalid personalId: \"{0}\""
    )
    @ValueSource(longs = {0L, -1L, 12L})

    @NullSource
    void updateDataWithInvalidPersonalId(Long personalId) throws JsonProcessingException {
        checkInvalidPersonalData(
                b -> b.personalId(personalId), "Invalid personalId");
    }

    @SneakyThrows
    private void checkInvalidPersonalData(
            Consumer<PersonalData.PersonalDataBuilder> modifier,
            String expectedMessage) {

        ApiErrorAssertions.ApiNegativeAsserts.assertBadRequest(
                () -> PersonalData.ofRandom().toBuilder(),
                modifier,
                PersonalData.PersonalDataBuilder::build,
                PersonalDataRequester::updatePersonalData,
                expectedMessage);
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