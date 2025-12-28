package lemfi.model.signUp;

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
public class SignUpRequester {
    private final static Properties properties = PropertiesHelper.loadProperties();
    private final static String API_URL = properties.getProperty("api.url");
    private final static URI SIGN_UP = URI.create(API_URL + "/public/sign-up");

    public static Response signUp(Registration registration) throws JsonProcessingException {
        log.info("Registration user");
        return RequestHelper.postRequest(SIGN_UP, registration);
    }

    public static Response signUpWithoutBody() throws JsonProcessingException {
        log.info("Registration user without body");
        return RequestHelper.postRequestWithoutBody(SIGN_UP);
    }
}
