package com.lemfi.qa;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationHandler {

    public void authenticate(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(new UserAuthenticationToken(userId));
    }

    public long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getCredentials();
    }

}