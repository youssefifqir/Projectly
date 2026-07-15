package com.example.Projectly.service.facade.security;

import com.example.Projectly.ws.dto.auth.AuthenticationRequest;
import com.example.Projectly.ws.dto.auth.RefreshRequest;
import com.example.Projectly.ws.dto.auth.RegistrationRequest;
import com.example.Projectly.ws.dto.auth.AuthenticationResponse;

public interface AuthenticationService {

    AuthenticationResponse login(AuthenticationRequest request);

    void register(RegistrationRequest request);

    AuthenticationResponse refreshToken(RefreshRequest req);

    void logout(String authorizationHeader);
}
