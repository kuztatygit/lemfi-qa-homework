package com.lemfi.qa.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Getter
@Setter
@Data
public class UpdatePersonalDataRequestDTO {

    @NotEmpty(message = "First name cannot be null or blank")
    @Pattern(regexp = "^[^0-9]+$", message = "First name cannot contain numbers")
    @JsonProperty("firstName")
    private String firstName;

    @NotEmpty(message = "Surname cannot be null or blank")
    @Pattern(regexp = "^[^0-9]+$", message = "Surname cannot contain numbers")
    @JsonProperty("surname")
    private String surname;

    @NotEmpty(message = "Personal ID cannot be null")
    @Pattern(regexp = "^[0-9]{9}$", message = "Personal ID must be a 9-digit numeric value")
    @JsonProperty("personalId")
    private Long personalId;

}
