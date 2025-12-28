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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Epic("Payments API")
@Feature("Get payments")
public class GetPaymentsTests {

    @Autowired
    PaymentDao paymentDao;

    @Autowired
    UserDao userDao;

    private String emailToCleanup;
    private final List<Long> paymentIdsToCleanup = new ArrayList<>();

    @BeforeEach
    void init() throws JsonProcessingException {
        Allure.step("Register random user", () -> {
            UserRegistrationStep.RegistrationResult result = UserRegistrationStep.registerRandomUser();
            Registration registration = result.request();
            emailToCleanup = registration.getEmail();

            Allure.parameter("registeredEmail", emailToCleanup);
            log.info("Registered user: email={}", emailToCleanup);

            assertNotNull(emailToCleanup, "Registered email must not be null");
            assertFalse(emailToCleanup.isBlank(), "Registered email must not be blank");
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
    @Story("Empty list")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /payments: returns empty list when user has no payments")
    void getPaymentsShouldReturnEmptyListWhenUserHasNoPayments() throws JsonProcessingException {
        Response response = Allure.step("Call GET /payments", () -> {
            Response r = PaymentRequester.getPayments();
            Allure.addAttachment("GET /payments response body", "application/json", r.getBody().asPrettyString(), ".json");
            return r;
        });

        Allure.step("Assert response and empty list", () -> {
            assertEquals(200, response.getStatusCode(), "GET /api/payments must succeed");

            List<?> payments = response.jsonPath().getList("$");
            assertNotNull(payments, "Payments list must not be null");
            assertTrue(payments.isEmpty(), "Payments list must be empty for user without payments");

            Allure.parameter("paymentsCount", payments.size());
            log.info("Payments count: {}", payments.size());
        });
    }

    @Test
    @Story("Created payment appears in list")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("GET /payments: contains created payment")
    void getPaymentsShouldContainCreatedPayment() throws JsonProcessingException {
        PaymentResult created = Allure.step("Create payment (precondition)", () -> createPayment(new BigDecimal("3.00"), "EUR"));

        Long createdId = Allure.step("Extract created payment id", () -> {
            Long id = created.response().jsonPath().getLong("id");
            assertNotNull(id, "Payment id must be returned in create payment response");
            paymentIdsToCleanup.add(id);

            Allure.parameter("createdPaymentId", id);
            log.info("Created payment id={}", id);
            return id;
        });

        Response getResponse = Allure.step("Call GET /payments", () -> {
            Response r = PaymentRequester.getPayments();
            Allure.addAttachment("GET /payments response body", "application/json", r.getBody().asPrettyString(), ".json");
            return r;
        });

        Allure.step("Assert created payment is present and fields are correct", () -> {
            assertEquals(200, getResponse.getStatusCode(), "GET /api/payments must succeed");

            List<Long> ids = getResponse.jsonPath().getList("id", Long.class);
            assertNotNull(ids, "Payments ids list must not be null");
            assertTrue(ids.contains(createdId), "Payments list must contain created payment id");

            int index = ids.indexOf(createdId);
            Allure.parameter("createdPaymentIndexInList", index);
            log.info("Created payment index in list={}", index);

            String type = getResponse.jsonPath().getString("[" + index + "].type");
            assertEquals(created.payment().getTransactionType().toString(), type,
                    "Payment type in list must match created payment type");

            BigDecimal actualAmount = new BigDecimal(getResponse.jsonPath().getString("[" + index + "].amount"));
            BigDecimal expectedAmount = created.payment().getAmount().getAmount();
            assertEquals(0, expectedAmount.compareTo(actualAmount), "Payment amount in list must match created amount");

            String rawResponse = getResponse.jsonPath().getString("[" + index + "].rawResponse");
            assertNotNull(rawResponse, "rawResponse in list must not be null");
            assertFalse(rawResponse.isBlank(), "rawResponse in list must not be blank");

            Allure.addAttachment("Found item rawResponse", "application/json", rawResponse, ".json");
        });
    }

    @Test
    @Story("Schema validation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /payments: valid schema for each item")
    void getPaymentsShouldReturnValidSchemaForEachItem() throws JsonProcessingException {
        PaymentResult p1 = Allure.step("Create payment #1", () -> createPayment(new BigDecimal("1.00"), "EUR"));
        Long id1 = Allure.step("Extract payment #1 id", () -> {
            Long id = p1.response().jsonPath().getLong("id");
            assertNotNull(id);
            paymentIdsToCleanup.add(id);
            return id;
        });

        PaymentResult p2 = Allure.step("Create payment #2", () -> createPayment(new BigDecimal("2.50"), "EUR"));
        Long id2 = Allure.step("Extract payment #2 id", () -> {
            Long id = p2.response().jsonPath().getLong("id");
            assertNotNull(id);
            paymentIdsToCleanup.add(id);
            return id;
        });

        Allure.parameter("createdPaymentId1", id1);
        Allure.parameter("createdPaymentId2", id2);

        Response response = Allure.step("Call GET /payments", () -> {
            Response r = PaymentRequester.getPayments();
            Allure.addAttachment("GET /payments response body", "application/json", r.getBody().asPrettyString(), ".json");
            return r;
        });

        Allure.step("Assert list is not empty and validate each item schema", () -> {
            assertEquals(200, response.getStatusCode(), "GET /api/payments must succeed");

            List<Long> ids = response.jsonPath().getList("id", Long.class);
            assertNotNull(ids, "Payments list must not be null");
            assertFalse(ids.isEmpty(), "Payments list must not be empty");

            Allure.parameter("paymentsCount", ids.size());
            log.info("Payments list size={}", ids.size());

            for (int i = 0; i < ids.size(); i++) {
                final int index = i;
                Allure.step("Validate item #" + index, () -> {
                    Long id = response.jsonPath().getLong("[" + index + "].id");
                    assertNotNull(id, "Payment id must not be null");
                    assertTrue(id > 0, "Payment id must be > 0");

                    String type = response.jsonPath().getString("[" + index + "].type");
                    assertNotNull(type, "Payment type must not be null");
                    assertFalse(type.isBlank(), "Payment type must not be blank");

                    String amountStr = response.jsonPath().getString("[" + index + "].amount");
                    assertNotNull(amountStr, "Payment amount must not be null");
                    BigDecimal amount = new BigDecimal(amountStr);
                    assertTrue(amount.compareTo(BigDecimal.ZERO) > 0, "Payment amount must be > 0");

                    String rawResponse = response.jsonPath().getString("[" + index + "].rawResponse");
                    assertNotNull(rawResponse, "rawResponse must not be null");
                    assertFalse(rawResponse.isBlank(), "rawResponse must not be blank");
                });
            }
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