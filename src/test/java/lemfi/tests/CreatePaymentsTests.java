package lemfi.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import lemfi.dao.payments.PaymentDao;
import lemfi.dao.payments.PaymentDto;
import lemfi.dao.user.UserDao;
import lemfi.helper.ApiErrorAssertions;
import lemfi.helper.CleanupHelper;
import lemfi.helper.UserRegistrationStep;
import lemfi.model.payment.Amount;
import lemfi.model.payment.Payment;
import lemfi.model.payment.PaymentRequester;
import lemfi.model.signUp.Registration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class CreatePaymentsTests {

    @Autowired
    PaymentDao paymentDao;

    @Autowired
    UserDao userDao;

    private String emailToCleanup;
    private Long paymentIdToCleanup;
    private Long userId;

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
        CleanupHelper.cleanupPaymentAndUser(paymentDao, paymentIdToCleanup, userDao, emailToCleanup);
    }

    @Test
    void createPayment() throws JsonProcessingException {
        Amount amount = Amount.builder()
                .amount(new BigDecimal("0.01"))
                .currency("EUR")
                .build();

        Payment payment = Payment.ofRandom()
                .toBuilder()
                .amount(amount)
                .build();

        Response paymentResponse = PaymentRequester.createPayment(payment);

        assertEquals(200, paymentResponse.getStatusCode(), "Create payment must succeed");

        paymentIdToCleanup = paymentResponse.jsonPath().getLong("id");
        assertNotNull(paymentIdToCleanup, "Payment id must be returned in create payment response");

        assertAll(
                () -> assertEquals(payment.getTransactionType().toString(), paymentResponse.jsonPath().getString("type"),
                        "Transaction type in response must match request"),
                () -> assertEquals("EUR", paymentResponse.jsonPath().getString("amount.currency"),
                        "Currency in response must match request"),

                () -> assertEquals(amount.getAmount(), new BigDecimal(paymentResponse.jsonPath().getString("amount.amount")),
                        "Amount in response must match request"));

        PaymentDto paymentDto = paymentDao.selectPayment(paymentIdToCleanup);
        assertNotNull(paymentDto, "Payment must be created in DB");

        assertAll(
                () -> assertEquals(payment.getTransactionType(), paymentDto.getType(),
                        "Transaction type in DB must match request"),
                () -> assertEquals(amount.getAmount(), paymentDto.getAmount(),
                        "Amount in DB must match request"),
                () -> assertEquals(userId, paymentDto.getUserId(),
                        "Payment must belong to registered user"));

        String responseBody = paymentResponse.getBody().asString();
        String rawResponseFromDb = paymentDto.getRawResponse();
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode responseJson = objectMapper.readTree(responseBody);
        JsonNode dbJson = objectMapper.readTree(rawResponseFromDb);

        assertEquals(responseJson, dbJson,
                "rawResponse stored in DB must match actual API response");
    }

    @Test
    void createPaymentShouldFailWhenAmountObjectIsNull() throws JsonProcessingException {
        checkInvalidPayment(
                p -> p.amount(null),
                a -> {
                },
                "Invalid amount");
    }

    @Test
    void createPaymentShouldFailWhenAmountValueIsNull() throws JsonProcessingException {
        checkInvalidPayment(
                p -> {
                },
                a -> a.amount(null),
                "Invalid amount");
    }

    @ParameterizedTest(name = "Create payment should fail when amount value is invalid: {0}")
    @MethodSource("invalidAmountValues")
    void createPaymentShouldFailWhenAmountValueIsInvalid(BigDecimal invalidValue) throws JsonProcessingException {
        checkInvalidPayment(
                p -> {
                },
                a -> a.amount(invalidValue),
                "Invalid amount"
        );
    }

    static Stream<BigDecimal> invalidAmountValues() {
        return Stream.of(
                BigDecimal.ZERO,
                new BigDecimal("-1.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.0001")
        );
    }

    @ParameterizedTest(name = "Create payment should fail with invalid currency: \"{0}\"")
    @ValueSource(strings = {"", " ", "EURO", "AAA", "E U", "US", "USDD", "123", "@@@"})
    @NullSource
    void createPaymentShouldFailWhenCurrencyIsInvalid(String currency) throws JsonProcessingException {
        checkInvalidPayment(
                p -> {
                },
                a -> a.currency(currency),
                "Invalid currency"
        );
    }

    @ParameterizedTest(name = "Create payment should fail with invalid accountHolderPersonalId: \"{0}\"")
    @ValueSource(strings = {"", " ", "ABC", "12", "123456789012345678901234567890"})
    @NullSource
    void createPayment_shouldFail_whenAccountHolderPersonalIdInvalid(String personalId) throws JsonProcessingException {
        checkInvalidPayment(
                p -> p.accountHolderPersonalId(personalId),
                a -> {
                },
                "Invalid accountHolderPersonalId"
        );
    }

    @ParameterizedTest(name = "Create payment should fail with invalid accountHolderFullName: \"{0}\"")
    @ValueSource(strings = {"", " ", "1", "12345", "@@@", " Test", "Test "})
    @NullSource
    void createPayment_shouldFail_whenAccountHolderFullNameInvalid(String fullName) throws JsonProcessingException {
        checkInvalidPayment(
                p -> p.accountHolderFullName(fullName),
                a -> {
                },
                "Invalid accountHolderFullName"
        );
    }

    @ParameterizedTest(name = "Create payment should fail with invalid investorId: {0}")
    @ValueSource(longs = {0L, -1L})
    @NullSource
    void createPayment_shouldFail_whenInvestorIdInvalid(Long investorId)
            throws JsonProcessingException {

        checkInvalidPayment(p -> p.investorId(investorId),
                a -> {
                }, "Invalid investorId");
    }

    @ParameterizedTest(name = "Create payment should fail with invalid accountNumber: \"{0}\"")
    @ValueSource(strings = {"", " ", "ABC", "123", "1234567890123456789012345678901234567890"})
    @NullSource
    void createPayment_shouldFail_whenAccountNumberInvalid(String accountNumber) throws JsonProcessingException {
        checkInvalidPayment(
                p -> p.accountNumber(accountNumber),
                a -> {
                }, "Invalid accountNumber");
    }

    @Test
    void createPayment_shouldFail_whenTransactionTypeIsNull() throws JsonProcessingException {
        checkInvalidPayment(
                p -> p.transactionType(null),
                a -> {
                }, "Invalid transactionType");
    }

    @Test
    void createPayment_shouldFail_whenBookingDateIsNull() throws JsonProcessingException {
        checkInvalidPayment(
                p -> p.bookingDate(null),
                a -> {
                }, "Invalid bookingDate");
    }

    @Test
    void createPayment_shouldFail_whenBookingDateInFuture() throws JsonProcessingException {
        checkInvalidPayment(
                p -> p.bookingDate(java.time.LocalDate.now().plusDays(1)),
                a -> {
                },
                "Invalid bookingDate"
        );
    }

    @Test
    void createPayment_shouldFail_whenBodyMissing() throws JsonProcessingException {
        Response response = PaymentRequester.createPaymentWithoutBody();
        assertEquals(400, response.getStatusCode());
        assertEquals("Body is required", response.jsonPath().getString("message"));
    }

    @SneakyThrows
    private void checkInvalidPayment(
            Consumer<Payment.PaymentBuilder> paymentModifier,
            Consumer<Amount.AmountBuilder> amountModifier,
            String expectedMessage
    ) {
        ApiErrorAssertions.ApiNegativeAsserts.assertBadRequest(
                () -> {
                    Amount.AmountBuilder amountBuilder = Amount.ofRandom().toBuilder();
                    amountModifier.accept(amountBuilder);
                    Amount amount = amountBuilder.build();

                    return Payment.ofRandom()
                            .toBuilder()
                            .amount(amount);
                },
                paymentModifier,
                Payment.PaymentBuilder::build,
                PaymentRequester::createPayment,
                expectedMessage
        );
    }
}