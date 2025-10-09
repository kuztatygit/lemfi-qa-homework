package com.lemfi.qa.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemfi.qa.domain.model.TransactionType;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class TransactionRequestDTO {

    @NotEmpty(message = "Account number is required")
    @JsonProperty("accountNumber")
    private String accountNumber;

    @NotEmpty(message = "Account holder full name is required")
    private String accountHolderFullName;

    @NotEmpty(message = "Account holder personal ID is required")
    private String accountHolderPersonalId;

    @NotEmpty(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotEmpty(message = "Investor ID is required")
    private String investorId;

    @Valid
    private AmountDTO amount;

    @NotEmpty(message = "Booking date is required")
    @PastOrPresent(message = "Booking date must be in the past or present")
    private LocalDate bookingDate;

    @Getter
    @Setter
    public static class AmountDTO {
        @NotEmpty(message = "Currency is required")
        private String currency;

        @NotEmpty(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than or equal to 0.01")
        private BigDecimal amount;
    }
}
