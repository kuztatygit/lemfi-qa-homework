package com.lemfi.qa.mapper;


import com.lemfi.qa.domain.dto.UserDTO;
import com.lemfi.qa.domain.entity.User;

public class UserDTOMapper {

    public static UserDTO toUserDTO(User user) {
        var userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setEmail(user.getEmail());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setSurname(user.getSurname());
        userDTO.setPersonalId(user.getPersonalId());
        return userDTO;
    }

}
