package lemfi.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
@UtilityClass
public class RequestHelper {

    public static Response postRequest(URI uri, Object object) throws JsonProcessingException {
        String json = ObjectMapperHelper.getMapper().writeValueAsString(object);
        log.info("{} >> {}", uri, json);
        return RestAssured
                .given()
                .filter(AllureRestAssuredHelper.filter())
                .contentType(ContentType.JSON)
                .body(json)
                .when()
                .post(uri)
                .then()
                .extract()
                .response();
    }

    public static Response postRequestWithoutBody(URI uri) throws JsonProcessingException {
        return RestAssured
                .given()
                .contentType(ContentType.JSON)
                .when()
                .post(uri)
                .then()
                .extract()
                .response();
    }

    public static Response getRequest(URI uri) {
        log.info("{} >> GET", uri);
        return RestAssured
                .given()
                .filter(AllureRestAssuredHelper.filter())
                .when()
                .get(uri)
                .then()
                .extract()
                .response();
    }
}
