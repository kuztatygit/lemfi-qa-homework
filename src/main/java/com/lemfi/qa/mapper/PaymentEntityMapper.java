package com.lemfi.qa.mapper;


import com.lemfi.qa.domain.dto.TransactionRequestDTO;
import com.lemfi.qa.domain.entity.Payment;
import com.lemfi.qa.domain.entity.User;
import com.lemfi.qa.domain.model.TransactionType;
import lombok.SneakyThrows;

import static com.lemfi.qa.JsonConverter.toJsonString;


public class PaymentEntityMapper {

    @SneakyThrows
    public static Payment transactionReqToPaymentEntity(TransactionRequestDTO transactionRequestDTO, TransactionType type, User user) {
        var paymentEntity = new Payment();
        paymentEntity.setType(type);
        paymentEntity.setAmount(transactionRequestDTO.getAmount().getAmount());
        paymentEntity.setUser(user);
        paymentEntity.setRawResponse(toJsonString(transactionRequestDTO));
        return paymentEntity;
    }

}
