package lemfi.model.personalDataUpdate;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import lemfi.helper.PropertiesHelper;
import lemfi.helper.RequestHelper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Properties;

@Slf4j
@UtilityClass
public class PersonalDataRequester {

    private final static Properties properties = PropertiesHelper.loadProperties();
    private final static String API_URL = properties.getProperty("api.url");
    private final static URI PERSONAL_DATA = URI.create(API_URL + "/api/personal-data");

    public static Response updatePersonalData(PersonalData personalData) throws JsonProcessingException {
        log.info("Update personal data");
        return RequestHelper.postRequest(PERSONAL_DATA, personalData);
    }

    public static Response updatePersonalDataWithoutBody() throws JsonProcessingException {
        log.info("Update personal data without body");
        return RequestHelper.postRequestWithoutBody(PERSONAL_DATA);
    }
}
