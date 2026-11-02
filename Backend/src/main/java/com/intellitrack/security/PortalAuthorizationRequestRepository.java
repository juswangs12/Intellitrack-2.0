package com.intellitrack.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/** Read portal choice from the state-validated saved request, never callback query parameters. */
public class PortalAuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private final HttpSessionOAuth2AuthorizationRequestRepository delegate = new HttpSessionOAuth2AuthorizationRequestRepository();
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) { return delegate.loadAuthorizationRequest(request); }
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorization, HttpServletRequest request, HttpServletResponse response) {
        if (authorization != null) authorization = OAuth2AuthorizationRequest.from(authorization)
            .attributes(attributes -> attributes.put(LoginAudience.ATTRIBUTE, LoginAudience.normalize(request.getParameter("audience")))).build();
        delegate.saveAuthorizationRequest(authorization, request, response);
    }
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest saved = delegate.removeAuthorizationRequest(request, response);
        if (saved != null) request.setAttribute(LoginAudience.ATTRIBUTE, saved.getAttribute(LoginAudience.ATTRIBUTE));
        return saved;
    }
}
