package lemfi.helper;

import lemfi.dao.payments.PaymentDao;
import lemfi.dao.user.UserDao;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class CleanupHelper {

    public static void cleanupPaymentAndUser(
            PaymentDao paymentDao,
            Long paymentIdToCleanup,
            UserDao userDao,
            String emailToCleanup
    ) {
        if (paymentIdToCleanup != null) {
            try {
                log.info("Cleanup payment with id: {}", paymentIdToCleanup);
                paymentDao.deletePaymentById(paymentIdToCleanup);
            } catch (Exception e) {
                log.warn("Failed to cleanup payment id {}: {}", paymentIdToCleanup, e.getMessage());
            }
        }

        if (emailToCleanup != null && !emailToCleanup.isBlank()) {
            try {
                log.info("Cleanup user with email: {}", emailToCleanup);
                userDao.deleteByEmail(emailToCleanup);
            } catch (Exception e) {
                log.warn("Failed to cleanup user {}: {}", emailToCleanup, e.getMessage());
            }
        }
    }
}
