package lemfi.model.payment;

import lemfi.model.TransactionType;
import lombok.Builder;
import lombok.Data;
import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

@Data
@Builder(toBuilder = true)
public class Payment {
    private String accountHolderFullName;
    private String accountHolderPersonalId;
    private TransactionType transactionType;
    private Long investorId;
    private Amount amount;
    private LocalDate bookingDate;
    private String accountNumber;

    private static final Faker faker = new Faker();

    public static Payment ofRandom() {
        return Payment.builder()
                .accountHolderFullName(faker.name().fullName())
                .accountHolderPersonalId(faker.idNumber().valid())
                .transactionType(TransactionType.FUNDING)
                .investorId(ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L))
                .amount(null)
                .bookingDate(LocalDate.now())
                .accountNumber(faker.regexify("[0-9]{12}"))
                .build();
    }
}
