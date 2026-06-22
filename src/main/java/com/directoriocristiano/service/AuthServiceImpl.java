package com.directoriocristiano.service;

import com.directoriocristiano.dto.AuthResponse;
import com.directoriocristiano.dto.LoginRequest;
import com.directoriocristiano.dto.RegisterRequest;
import com.directoriocristiano.dto.UserProfileResponse;
import com.directoriocristiano.exception.ResourceNotFoundException;
import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.model.enums.VerificationStep;
import com.directoriocristiano.repository.UserRepository;
import com.directoriocristiano.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya est\u00e1 registrado");
        }

        User user = User.builder()
                .email(request.email())
                .displayName(request.displayName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .userType(request.userType())
                .isVerified(false)
                .pastoralVerification(false)
                .church(request.church())
                .pastorName(request.pastorName())
                .verificationStep(VerificationStep.unverified)
                .build();

        user = userRepository.save(user);

        String token = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        return new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName(), token, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inv\u00e1lidas"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inv\u00e1lidas");
        }

        String token = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        return new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName(), token, refreshToken);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Token de refresco inv\u00e1lido");
        }

        var userId = jwtProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", userId));

        String token = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId());

        return new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName(), token, newRefreshToken);
    }

    public UserProfileResponse getProfile(User user) {
        return UserProfileResponse.from(user);
    }
}
