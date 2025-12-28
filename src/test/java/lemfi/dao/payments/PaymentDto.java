package lemfi.dao.payments;

import lemfi.model.TransactionType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentDto {
    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private Long userId;
    private String rawResponse;
}