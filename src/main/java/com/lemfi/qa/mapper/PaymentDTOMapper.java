package com.lemfi.qa.mapper;


import com.lemfi.qa.domain.dto.PaymentDTO;
import com.lemfi.qa.domain.entity.Payment;

public class PaymentDTOMapper {

    public static PaymentDTO toPaymentDTO(Payment payment) {
        var paymentDTO = new PaymentDTO();
        paymentDTO.setId(payment.getId());
        paymentDTO.setAmount(payment.getAmount());
        paymentDTO.setTransactionType(payment.getType());
        paymentDTO.setRawResponse(payment.getRawResponse());
        return paymentDTO;
    }
}