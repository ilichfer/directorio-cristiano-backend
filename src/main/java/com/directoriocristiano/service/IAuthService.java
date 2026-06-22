package com.directoriocristiano.service;

import com.directoriocristiano.dto.AuthResponse;
import com.directoriocristiano.dto.LoginRequest;
import com.directoriocristiano.dto.RegisterRequest;
import com.directoriocristiano.dto.UserProfileResponse;
import com.directoriocristiano.model.entity.User;

public interface IAuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshToken);
    UserProfileResponse getProfile(User user);
}
