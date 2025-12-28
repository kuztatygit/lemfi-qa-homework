package lemfi.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import lemfi.dao.payments.PaymentDao;
import lemfi.dao.user.UserDao;
import lemfi.helper.CleanupHelper;
import lemfi.helper.UserRegistrationStep;
import lemfi.model.payment.Amount;
import lemfi.model.payment.Payment;
import lemfi.model.payment.PaymentRequester;
import lemfi.model.signUp.Registration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GetPaymentsTests {

    @Autowired
    PaymentDao paymentDao;

    @Autowired
    UserDao userDao;

    private String emailToCleanup;
    private final List<Long> paymentIdsToCleanup = new ArrayList<>();

    @BeforeEach
    void init() throws JsonProcessingException {
        UserRegistrationStep.RegistrationResult result = UserRegistrationStep.registerRandomUser();
        Registration registration = result.request();
        emailToCleanup = registration.getEmail();
    }

    @AfterEach
    void cleanup() {
        for (Long paymentId : paymentIdsToCleanup) {
            CleanupHelper.cleanupPaymentAndUser(paymentDao, paymentId, userDao, null);
        }
        paymentIdsToCleanup.clear();

        CleanupHelper.cleanupPaymentAndUser(paymentDao, null, userDao, emailToCleanup);
    }

    @Test
    void getPaymentsShouldReturnEmptyListWhenUserHasNoPayments() throws JsonProcessingException {
        Response response = PaymentRequester.getPayments();
        assertEquals(200, response.getStatusCode(), "GET /api/payments must succeed");

        List<?> payments = response.jsonPath().getList("$");
        assertNotNull(payments, "Payments list must not be null");
        assertTrue(payments.isEmpty(), "Payments list must be empty for user without payments");
    }

    @Test
    void getPaymentsShouldContainCreatedPayment() throws JsonProcessingException {
        PaymentResult created = createPayment(new BigDecimal("3.00"), "EUR");

        Long createdId = created.response().jsonPath().getLong("id");
        assertNotNull(createdId, "Payment id must be returned in create payment response");
        paymentIdsToCleanup.add(createdId);

        Response getResponse = PaymentRequester.getPayments();
        assertEquals(200, getResponse.getStatusCode(), "GET /api/payments must succeed");

        List<Long> ids = getResponse.jsonPath().getList("id", Long.class);
        assertNotNull(ids, "Payments ids list must not be null");
        assertTrue(ids.contains(createdId), "Payments list must contain created payment id");

        int index = ids.indexOf(createdId);

        String type = getResponse.jsonPath().getString("[" + index + "].type");
        assertEquals(created.payment().getTransactionType().toString(), type,
                "Payment type in list must match created payment type");

        BigDecimal actualAmount = new BigDecimal(getResponse.jsonPath().getString("[" + index + "].amount"));
        BigDecimal expectedAmount = created.payment().getAmount().getAmount();
        assertEquals(0, expectedAmount.compareTo(actualAmount), "Payment amount in list must match created amount");

        String rawResponse = getResponse.jsonPath().getString("[" + index + "].rawResponse");
        assertNotNull(rawResponse, "rawResponse in list must not be null");
        assertFalse(rawResponse.isBlank(), "rawResponse in list must not be blank");
    }

    @Test
    void getPaymentsShouldReturnValidSchemaForEachItem() throws JsonProcessingException {
        PaymentResult p1 = createPayment(new BigDecimal("1.00"), "EUR");
        Long id1 = p1.response().jsonPath().getLong("id");
        assertNotNull(id1);
        paymentIdsToCleanup.add(id1);

        PaymentResult p2 = createPayment(new BigDecimal("2.50"), "EUR");
        Long id2 = p2.response().jsonPath().getLong("id");
        assertNotNull(id2);
        paymentIdsToCleanup.add(id2);

        Response response = PaymentRequester.getPayments();
        assertEquals(200, response.getStatusCode(), "GET /api/payments must succeed");

        List<Long> ids = response.jsonPath().getList("id", Long.class);
        assertNotNull(ids, "Payments list must not be null");
        assertFalse(ids.isEmpty(), "Payments list must not be empty");

        for (int i = 0; i < ids.size(); i++) {
            Long id = response.jsonPath().getLong("[" + i + "].id");
            assertNotNull(id, "Payment id must not be null");
            assertTrue(id > 0, "Payment id must be > 0");

            String type = response.jsonPath().getString("[" + i + "].type");
            assertNotNull(type, "Payment type must not be null");
            assertFalse(type.isBlank(), "Payment type must not be blank");

            String amountStr = response.jsonPath().getString("[" + i + "].amount");
            assertNotNull(amountStr, "Payment amount must not be null");
            BigDecimal amount = new BigDecimal(amountStr);
            assertTrue(amount.compareTo(BigDecimal.ZERO) > 0, "Payment amount must be > 0");

            String rawResponse = response.jsonPath().getString("[" + i + "].rawResponse");
            assertNotNull(rawResponse, "rawResponse must not be null");
            assertFalse(rawResponse.isBlank(), "rawResponse must not be blank");
        }
    }

    private PaymentResult createPayment(BigDecimal amount, String currency) throws JsonProcessingException {
        Amount buildAmount = Amount.builder()
                .amount(amount)
                .currency(currency)
                .build();

        Payment payment = Payment.ofRandom()
                .toBuilder()
                .amount(buildAmount)
                .build();

        Response paymentResponse = PaymentRequester.createPayment(payment);
        assertEquals(200, paymentResponse.getStatusCode(), "Create payment must succeed");

        return new PaymentResult(payment, paymentResponse);
    }

    public record PaymentResult(Payment payment, Response response) {
    }
}
