package com.intellitrack;
import com.intellitrack.security.*;
import com.intellitrack.service.AuthService;
import com.intellitrack.dto.LoginRequest;
import com.intellitrack.entity.User;
import com.intellitrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LoginPortalTest {
    @Test void studentPasswordCannotIssueTokensThroughAnyPortal(){
        var users=mock(UserRepository.class);var jwt=mock(JwtTokenProvider.class);var auth=new AuthService();
        ReflectionTestUtils.setField(auth,"userRepository",users);ReflectionTestUtils.setField(auth,"jwtTokenProvider",jwt);
        var student=new User("Student","Test","student@example.test",new BCryptPasswordEncoder().encode("test-password"),"student");
        when(users.findByEmailIgnoreCase(student.getEmail())).thenReturn(Optional.of(student));
        for(String audience:new String[]{"student","staff",null}){
            var request=new LoginRequest(student.getEmail(),"test-password");request.setAudience(audience);
            assertThrows(IllegalArgumentException.class,()->auth.authenticate(request));
        }
        verifyNoInteractions(jwt);verify(users,never()).save(any());
    }
    @Test void unknownGoogleAccountCannotCreateStudentThroughStaffPortal()throws Exception{
        var users=mock(UserRepository.class);var auth=mock(AuthService.class);var jwt=mock(JwtTokenProvider.class);
        when(auth.isEmailDomainAllowed("new@example.test")).thenReturn(true);
        when(users.findByEmailIgnoreCase("new@example.test")).thenReturn(Optional.empty());
        var handler=new OAuth2AuthenticationSuccessHandler();ReflectionTestUtils.setField(handler,"userRepository",users);ReflectionTestUtils.setField(handler,"authService",auth);ReflectionTestUtils.setField(handler,"jwtTokenProvider",jwt);ReflectionTestUtils.setField(handler,"frontendRedirect","https://app.example.test");
        var profile=new org.springframework.security.oauth2.core.user.DefaultOAuth2User(java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")),java.util.Map.of("email","new@example.test","email_verified",true,"sub","google-id"),"sub");
        var request=new MockHttpServletRequest();request.setAttribute(LoginAudience.ATTRIBUTE,"staff");var response=new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request,response,new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(profile,null,profile.getAuthorities()));
        assertTrue(response.getRedirectedUrl().contains("wrong_portal"));verify(users,never()).save(any());verifyNoInteractions(jwt);
    }
    @Test void wrongPortalNeverIssuesTokens(){
        var users=mock(UserRepository.class);var jwt=mock(JwtTokenProvider.class);var auth=new AuthService();
        ReflectionTestUtils.setField(auth,"userRepository",users);ReflectionTestUtils.setField(auth,"jwtTokenProvider",jwt);
        var user=new User("Staff","Test","staff@example.test",new BCryptPasswordEncoder().encode("test-password"),"coordinator");
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        var request=new LoginRequest(user.getEmail(),"test-password");request.setAudience("student");
        assertThrows(IllegalArgumentException.class,()->auth.authenticate(request));verifyNoInteractions(jwt);verify(users,never()).save(any());
        assertThrows(IllegalArgumentException.class,()->LoginAudience.requireRole("staff","student"));
        for(String role:new String[]{"adviser","coordinator","administrator"})assertDoesNotThrow(()->LoginAudience.requireRole("staff",role));
    }
    @Test void googleCallbackCannotOverrideSavedPortal(){
        var repository=new PortalAuthorizationRequestRepository();var start=new MockHttpServletRequest();var response=new MockHttpServletResponse();
        start.setParameter("audience","staff");
        var authorization=OAuth2AuthorizationRequest.authorizationCode().authorizationUri("https://accounts.example.test/auth").clientId("client").redirectUri("https://app.example.test/callback").state("random-state").build();
        repository.saveAuthorizationRequest(authorization,start,response);
        var callback=new MockHttpServletRequest();callback.setSession(start.getSession());callback.setParameter("state","random-state");callback.setParameter("audience","student");
        assertNotNull(repository.removeAuthorizationRequest(callback,response));assertEquals("staff",callback.getAttribute(LoginAudience.ATTRIBUTE));
        assertNull(repository.removeAuthorizationRequest(callback,response));
    }
}
