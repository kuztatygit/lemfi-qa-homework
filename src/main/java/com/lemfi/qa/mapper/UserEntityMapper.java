package com.lemfi.qa.mapper;


import com.lemfi.qa.domain.dto.RegistrationRequestDTO;
import com.lemfi.qa.domain.entity.User;

public class UserEntityMapper {

    public static User registrationReqToUserEntity(RegistrationRequestDTO registrationRequestDTO) {
        var userEntity = new User();
        userEntity.setEmail(registrationRequestDTO.getEmail());
        userEntity.setPassword(registrationRequestDTO.getPassword());
        return userEntity;
    }

}