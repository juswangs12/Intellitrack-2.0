package com.intellitrack;

import com.intellitrack.controller.AuthController;
import com.intellitrack.service.AuthService;
import com.intellitrack.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.dao.DataAccessResourceFailureException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthDatabaseFailureTest {
    @Test void databaseFailureIsNotDisguisedAsAnExpiredSessionOrBadPassword(){
        var service=mock(AuthService.class);var controller=new AuthController();
        ReflectionTestUtils.setField(controller,"authService",service);
        var request=new LoginRequest();var binding=mock(BindingResult.class);
        when(service.authenticate(request)).thenThrow(new DataAccessResourceFailureException("Database unavailable"));
        when(service.refreshToken("token")).thenThrow(new DataAccessResourceFailureException("Database unavailable"));
        assertThrows(DataAccessResourceFailureException.class,()->controller.login(request,binding));
        assertThrows(DataAccessResourceFailureException.class,()->controller.refreshToken(Map.of("refreshToken","token")));
        when(service.refreshToken("invalid")).thenThrow(new RuntimeException("Invalid refresh token"));
        assertEquals(401,controller.refreshToken(Map.of("refreshToken","invalid")).getStatusCode().value());
    }
}
