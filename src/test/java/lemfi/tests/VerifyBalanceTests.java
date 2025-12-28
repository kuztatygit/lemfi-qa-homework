package lemfi.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.qameta.allure.*;
import io.restassured.response.Response;
import lemfi.dao.payments.PaymentDao;
import lemfi.dao.user.UserDao;
import lemfi.helper.AllureLogAttachments;
import lemfi.helper.CleanupHelper;
import lemfi.helper.UserRegistrationStep;
import lemfi.model.payment.Amount;
import lemfi.model.payment.Payment;
import lemfi.model.payment.PaymentRequester;
import lemfi.model.signUp.Registration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Epic("Payments API")
@Feature("Verify balance")
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
        Allure.step("Register random user", () -> {
            UserRegistrationStep.RegistrationResult result = UserRegistrationStep.registerRandomUser();

            Registration registration = result.request();
            Response registrationResponse = result.response();

            emailToCleanup = registration.getEmail();
            userId = registrationResponse.jsonPath().getLong("id");

            Allure.parameter("registeredEmail", emailToCleanup);
            Allure.parameter("registeredUserId", userId);

            log.info("Registered user id={}, email={}", userId, emailToCleanup);

            assertNotNull(userId, "User id must be returned in registration response");
        });
    }

    @AfterEach
    void cleanup() {
        Allure.step("Cleanup created payments", () -> {
            for (Long paymentId : paymentIdsToCleanup) {
                try {
                    CleanupHelper.cleanupPaymentAndUser(paymentDao, paymentId, userDao, null);
                    log.info("Cleaned paymentId={}", paymentId);
                } catch (Exception e) {
                    log.warn("Cleanup failed for paymentId={}: {}", paymentId, e.getMessage(), e);
                }
            }
            paymentIdsToCleanup.clear();
        });

        Allure.step("Cleanup user by email", () -> {
            try {
                CleanupHelper.cleanupPaymentAndUser(paymentDao, null, userDao, emailToCleanup);
                log.info("Cleaned user email={}", emailToCleanup);
            } catch (Exception e) {
                log.warn("Cleanup failed for email={}: {}", emailToCleanup, e.getMessage(), e);
            }
        });


        AllureLogAttachments.attachTestLogIfExists();
    }

    @Test
    @Story("No payments")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /balance: balance is zero when user has no payments")
    void balanceShouldBeZeroWhenUserHasNoPayments() throws JsonProcessingException {
        Response getBalanceResponse = Allure.step("Call GET /balance", () -> {
            Response r = PaymentRequester.getBalance();
            Allure.addAttachment("GET /balance response body", "application/json", r.getBody().asPrettyString(), ".json");
            return r;
        });

        Allure.step("Assert status code and balance=0", () -> {
            assertEquals(200, getBalanceResponse.getStatusCode(), "Get balance must succeed");

            BigDecimal actualBalance = new BigDecimal(getBalanceResponse.jsonPath().getString("balance"));
            Allure.parameter("actualBalance", actualBalance);

            assertEquals(0, BigDecimal.ZERO.compareTo(actualBalance), "Balance must be 0 for user without payments");
            log.info("Balance for user without payments = {}", actualBalance);
        });
    }

    @Test
    @Story("Single payment")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("GET /balance: equals single payment amount")
    void balanceShouldEqualSinglePaymentAmount() throws JsonProcessingException {
        PaymentResult paymentResult = Allure.step("Create payment (precondition)", () ->
                createPayment(new BigDecimal("5.00"), "EUR")
        );

        Long paymentId = Allure.step("Extract created payment id", () -> {
            Long id = paymentResult.response().jsonPath().getLong("id");
            assertNotNull(id, "Payment id must be returned in create payment response");
            paymentIdsToCleanup.add(id);

            Allure.parameter("paymentId", id);
            log.info("Created paymentId={}", id);
            return id;
        });

        Response getBalanceResponse = Allure.step("Call GET /balance", () -> {
            Response r = PaymentRequester.getBalance();
            Allure.addAttachment("GET /balance response body", "application/json", r.getBody().asPrettyString(), ".json");
            return r;
        });

        Allure.step("Assert balance equals created payment amount", () -> {
            assertEquals(200, getBalanceResponse.getStatusCode(), "Get balance must succeed");

            BigDecimal actualBalance = new BigDecimal(getBalanceResponse.jsonPath().getString("balance"));
            BigDecimal expectedBalance = paymentResult.payment().getAmount().getAmount();

            Allure.parameter("expectedBalance", expectedBalance);
            Allure.parameter("actualBalance", actualBalance);

            assertEquals(0, expectedBalance.compareTo(actualBalance), "Balance must match created payment amount");
            log.info("Balance check: expected={}, actual={}, paymentId={}", expectedBalance, actualBalance, paymentId);
        });
    }

    @Test
    @Story("Multiple payments")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("GET /balance: sums two payments")
    void balance_shouldSumTwoPayments() throws JsonProcessingException {

        PaymentResult p1 = Allure.step("Create payment #1", () -> createPayment(new BigDecimal("5.00"), "EUR"));
        Long p1Id = Allure.step("Extract payment #1 id", () -> {
            Long id = p1.response().jsonPath().getLong("id");
            assertNotNull(id, "Payment id must be returned in create payment response");
            paymentIdsToCleanup.add(id);
            Allure.parameter("paymentId1", id);
            return id;
        });

        PaymentResult p2 = Allure.step("Create payment #2", () -> createPayment(new BigDecimal("3.50"), "EUR"));
        Long p2Id = Allure.step("Extract payment #2 id", () -> {
            Long id = p2.response().jsonPath().getLong("id");
            assertNotNull(id, "Payment id must be returned in create payment response");
            paymentIdsToCleanup.add(id);
            Allure.parameter("paymentId2", id);
            return id;
        });

        Response getBalanceResponse = Allure.step("Call GET /balance", () -> {
            Response r = PaymentRequester.getBalance();
            Allure.addAttachment("GET /balance response body", "application/json", r.getBody().asPrettyString(), ".json");
            return r;
        });

        Allure.step("Assert balance equals sum of two payments", () -> {
            assertEquals(200, getBalanceResponse.getStatusCode(), "Get balance must succeed");

            BigDecimal actualBalance = new BigDecimal(getBalanceResponse.jsonPath().getString("balance"));

            BigDecimal expectedBalance = p1.payment().getAmount().getAmount()
                    .add(p2.payment().getAmount().getAmount());

            Allure.parameter("expectedBalance", expectedBalance);
            Allure.parameter("actualBalance", actualBalance);

            assertEquals(0, expectedBalance.compareTo(actualBalance), "Balance must equal sum of two payments");
            log.info("Balance sum check: expected={}, actual={}, p1Id={}, p2Id={}", expectedBalance, actualBalance, p1Id, p2Id);
        });
    }

    @Step("Create payment: amount={amount}, currency={currency}")
    private PaymentResult createPayment(BigDecimal amount, String currency) throws JsonProcessingException {
        Amount buildAmount = Amount.builder()
                .amount(amount)
                .currency(currency)
                .build();

        Payment payment = Payment.ofRandom()
                .toBuilder()
                .amount(buildAmount)
                .build();

        Allure.parameter("createPayment.amount", amount);
        Allure.parameter("createPayment.currency", currency);

        Response paymentResponse = PaymentRequester.createPayment(payment);
        Allure.addAttachment("POST /payments response body", "application/json", paymentResponse.getBody().asPrettyString(), ".json");

        assertEquals(200, paymentResponse.getStatusCode(), "Create payment must succeed");

        return new PaymentResult(payment, paymentResponse);
    }

    public record PaymentResult(Payment payment, Response response) {
    }
}
