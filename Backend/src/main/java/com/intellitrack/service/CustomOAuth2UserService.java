package com.intellitrack.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    @Autowired private AuthService authService;
    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User profile = super.loadUser(request);
        if (!Boolean.TRUE.equals(profile.getAttribute("email_verified")))
            throw new OAuth2AuthenticationException("verified_email_required");
        if (!authService.isEmailDomainAllowed(profile.getAttribute("email")))
            throw new OAuth2AuthenticationException("email_domain_not_allowed");
        // Account creation/linking happens after portal validation in the success handler.
        return profile;
    }
}
