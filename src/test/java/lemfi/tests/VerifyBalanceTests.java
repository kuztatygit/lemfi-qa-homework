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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VerifyBalanceTests {

    @Autowired
    PaymentDao paymentDao;

    @Autowired
    UserDao userDao;

    private String emailToCleanup;
    private Long userId;

    private final List<Long> paymentIdsToCleanup = new ArrayList<>();

    @BeforeEach
    void init() throws JsonProcessingException {
        UserRegistrationStep.RegistrationResult result = UserRegistrationStep.registerRandomUser();

        Registration registration = result.request();
        Response registrationResponse = result.response();

        emailToCleanup = registration.getEmail();

        userId = registrationResponse.jsonPath().getLong("id");
        assertNotNull(userId, "User id must be returned in registration response");
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
    void balanceShouldBeZeroWhenUserHasNoPayments() throws JsonProcessingException {
        Response getBalanceResponse = PaymentRequester.getBalance();
        assertEquals(200, getBalanceResponse.getStatusCode(), "Get balance must succeed");

        BigDecimal actualBalance = new BigDecimal(getBalanceResponse.jsonPath().getString("balance"));
        assertEquals(0, BigDecimal.ZERO.compareTo(actualBalance), "Balance must be 0 for user without payments");
    }

    @Test
    void balanceShouldEqualSinglePaymentAmount() throws JsonProcessingException {
        PaymentResult paymentResult = createPayment(new BigDecimal("5.00"), "EUR");

        Long paymentId = paymentResult.response().jsonPath().getLong("id");
        assertNotNull(paymentId, "Payment id must be returned in create payment response");
        paymentIdsToCleanup.add(paymentId);

        Response getBalanceResponse = PaymentRequester.getBalance();
        assertEquals(200, getBalanceResponse.getStatusCode(), "Get balance must succeed");

        BigDecimal actualBalance = new BigDecimal(getBalanceResponse.jsonPath().getString("balance"));
        BigDecimal expectedBalance = paymentResult.payment().getAmount().getAmount();

        assertEquals(0, expectedBalance.compareTo(actualBalance), "Balance must match created payment amount");
    }

    @Test
    void balance_shouldSumTwoPayments() throws JsonProcessingException {

        PaymentResult p1 = createPayment(new BigDecimal("5.00"), "EUR");
        Long p1Id = p1.response().jsonPath().getLong("id");
        assertNotNull(p1Id, "Payment id must be returned in create payment response");
        paymentIdsToCleanup.add(p1Id);

        PaymentResult p2 = createPayment(new BigDecimal("3.50"), "EUR");
        Long p2Id = p2.response().jsonPath().getLong("id");
        assertNotNull(p2Id, "Payment id must be returned in create payment response");
        paymentIdsToCleanup.add(p2Id);

        Response getBalanceResponse = PaymentRequester.getBalance();
        assertEquals(200, getBalanceResponse.getStatusCode(), "Get balance must succeed");

        BigDecimal actualBalance = new BigDecimal(getBalanceResponse.jsonPath().getString("balance"));

        BigDecimal expectedBalance = p1.payment().getAmount().getAmount()
                .add(p2.payment().getAmount().getAmount());

        assertEquals(0, expectedBalance.compareTo(actualBalance), "Balance must equal sum of two payments");
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