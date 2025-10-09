package com.lemfi.qa.domain.model;


import com.lemfi.qa.domain.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUserResponse {
    private UserDTO user;
    private Message message;
}
