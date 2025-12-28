package lemfi.model.payment;

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
public class PaymentRequester {

    private final static Properties properties = PropertiesHelper.loadProperties();
    private final static String API_URL = properties.getProperty("api.url");
    private final static URI PAYMENT = URI.create(API_URL + "/api/add-funds");
    private final static URI GET_BALANCE = URI.create(API_URL + "/api/balance");
    private final static URI GET_PAYMENTS = URI.create(API_URL + "/api/payments");

    public static Response createPayment(Payment payment) throws JsonProcessingException {
        log.info("Create payment");
        return RequestHelper.postRequest(PAYMENT, payment);
    }

    public static Response createPaymentWithoutBody() throws JsonProcessingException {
        log.info("Create payment without body");
        return RequestHelper.postRequestWithoutBody(PAYMENT);
    }

    public static Response getBalance() throws JsonProcessingException {
        log.info("Get balance");
        return RequestHelper.getRequest(GET_BALANCE);
    }

    public static Response getPayments() throws JsonProcessingException {
        log.info("Get payments");
        return RequestHelper.getRequest(GET_PAYMENTS);
    }
}
