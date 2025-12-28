package lemfi.dao.payments;

import lemfi.model.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentDao {
    private final JdbcTemplate jdbcTemplate;

    public PaymentDto selectPayment(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM payments WHERE userId = ?",
                (rs, rowNum) -> {
                    PaymentDto paymentDto = new PaymentDto();
                    paymentDto.setId(rs.getLong("id"));
                    paymentDto.setType(TransactionType.valueOf(rs.getString("type")));
                    paymentDto.setAmount(rs.getBigDecimal("amount"));
                    paymentDto.setUserId(rs.getLong("user_id"));
                    paymentDto.setRawResponse(rs.getString("raw_response"));
                    return paymentDto;
                },
                userId
        );
    }

    public void deletePaymentById(Long userId) {
        jdbcTemplate.update(
                "DELETE FROM payments WHERE userId = ?",
                userId
        );
    }
}
