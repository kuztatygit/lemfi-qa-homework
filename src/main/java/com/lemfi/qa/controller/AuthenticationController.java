package com.lemfi.qa.controller;



import com.lemfi.qa.AuthenticationHandler;
import com.lemfi.qa.domain.dto.RegistrationRequestDTO;
import com.lemfi.qa.domain.dto.UserDTO;
import com.lemfi.qa.domain.model.Message;
import com.lemfi.qa.domain.model.RegisterUserResponse;
import com.lemfi.qa.service.RegistrationService;
import com.lemfi.qa.validation.RegistrationRequestValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/public")
public class AuthenticationController {

    private final RegistrationService registrationService;
    private final RegistrationRequestValidator registrationRequestValidator;
    private final AuthenticationHandler authenticationHandler;

    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody @Valid RegistrationRequestDTO req) {
        var validationStatus = registrationRequestValidator.validate(req);
        if (!validationStatus.isValid()) {
            return ResponseEntity.status(400).body(validationStatus.getMessage());
        }

        Optional<UserDTO> registeredUser = registrationService.registerUser(req);
        if (registeredUser.isPresent()) {
            authenticationHandler.authenticate(registeredUser.get().getId());
            return ResponseEntity.status(200).body(new RegisterUserResponse(registeredUser.get(), new Message("SUCCESS", "User registered")));
        }

        return ResponseEntity.status(400).body(new Message("FAIL", "Something went wrong"));
    }


}