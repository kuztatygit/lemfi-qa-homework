package lemfi.model.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Data
@Builder(toBuilder = true)
public class Amount {
    private String currency;
    private BigDecimal amount;

    public static Amount ofRandom() {
        return Amount.builder()
                .amount(BigDecimal.valueOf(
                        ThreadLocalRandom.current().nextLong(1, 1_000_000), 2))
                .currency(null)
                .build();
    }
}
