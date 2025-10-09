package com.lemfi.qa.service;


import com.lemfi.qa.domain.dto.TransactionRequestDTO;
import com.lemfi.qa.domain.entity.Payment;
import com.lemfi.qa.repository.PaymentRepository;
import com.lemfi.qa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.lemfi.qa.domain.model.TransactionType.FUNDING;
import static com.lemfi.qa.mapper.PaymentEntityMapper.transactionReqToPaymentEntity;


@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentService {

    private final UserService userService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public Optional<Long> handleAddFundsPayment(TransactionRequestDTO transactionRequestDTO) {
        var user = userService.getCurrentUser();

        return user.map(maybeUser -> {
            Payment payment = transactionReqToPaymentEntity(transactionRequestDTO, FUNDING, maybeUser);
            maybeUser.setBalance(maybeUser.getBalance().add(payment.getAmount()));

            paymentRepository.save(payment);
            userRepository.save(maybeUser);

            return payment.getId();
        });
    }

}
