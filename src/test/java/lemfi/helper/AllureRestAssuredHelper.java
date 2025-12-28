package lemfi.helper;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.filter.Filter;

public final class AllureRestAssuredHelper {

    private AllureRestAssuredHelper() {
    }

    private static final Filter FILTER = new AllureRestAssured()
            .setRequestTemplate("http-request.ftl")
            .setResponseTemplate("http-response.ftl");

    public static Filter filter() {
        return FILTER;
    }
}