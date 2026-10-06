package com.directoriocristiano.service;

import com.directoriocristiano.config.ModeratorEmails;
import com.directoriocristiano.dto.AuthResponse;
import com.directoriocristiano.dto.GoogleAuthRequest;
import com.directoriocristiano.dto.LoginRequest;
import com.directoriocristiano.dto.RegisterRequest;
import com.directoriocristiano.dto.UserProfileResponse;
import com.directoriocristiano.exception.ResourceNotFoundException;
import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.model.enums.AuthProvider;
import com.directoriocristiano.model.enums.UserType;
import com.directoriocristiano.model.enums.VerificationStep;
import com.directoriocristiano.repository.UserRepository;
import com.directoriocristiano.security.JwtProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Collections;

@Service
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final ModeratorEmails moderatorEmails;
    private final String googleClientId;
    private final boolean googleDemoModeEnabled;
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            ModeratorEmails moderatorEmails,
            @Value("${app.google.client-id}") String googleClientId,
            @Value("${app.google.demo-mode-enabled}") boolean googleDemoModeEnabled) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.moderatorEmails = moderatorEmails;
        this.googleClientId = googleClientId;
        this.googleDemoModeEnabled = googleDemoModeEnabled;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya est\u00e1 registrado");
        }

        User user = User.builder()
                .email(request.email())
                .displayName(request.displayName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .userType(resolveUserType(request.userType(), request.acceptAgreement()))
                .entrepreneurAgreementAt(agreementDate(request.userType(), request.acceptAgreement()))
                .moderator(moderatorEmails.contains(request.email()))
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

    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        String email;
        String displayName;
        String googleSub = null;
        AuthProvider provider;

        if (request.idToken() != null && !request.idToken().isBlank()) {
            if (googleClientId == null || googleClientId.isBlank()) {
                throw new IllegalArgumentException("El inicio de sesión con Google todavía no está configurado");
            }
            GoogleIdToken.Payload payload = verifyGoogleIdToken(request.idToken());
            email = payload.getEmail();
            Object name = payload.get("name");
            displayName = name != null ? name.toString() : email;
            googleSub = payload.getSubject();
            provider = AuthProvider.google;
        } else if (request.demoEmail() != null && !request.demoEmail().isBlank()) {
            if (!googleDemoModeEnabled) {
                throw new IllegalArgumentException("El modo de demostración de Google está deshabilitado");
            }
            email = request.demoEmail();
            displayName = request.demoDisplayName() != null && !request.demoDisplayName().isBlank()
                    ? request.demoDisplayName()
                    : email;
            provider = AuthProvider.google_demo;
        } else {
            throw new IllegalArgumentException("Falta idToken o demoEmail");
        }

        User user = findOrCreateGoogleUser(email, displayName, googleSub, provider,
                request.userType(), request.acceptAgreement());

        String token = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        return new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName(), token, refreshToken);
    }

    private User findOrCreateGoogleUser(String email, String displayName, String googleSub, AuthProvider provider,
                                        UserType requestedUserType, Boolean acceptAgreement) {
        if (googleSub != null) {
            var bySub = userRepository.findByGoogleSub(googleSub);
            if (bySub.isPresent()) {
                return bySub.get();
            }
        }

        var byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            if (googleSub != null && existing.getGoogleSub() == null) {
                existing.setGoogleSub(googleSub);
                existing = userRepository.save(existing);
            }
            return existing;
        }

        User user = User.builder()
                .email(email)
                .displayName(displayName)
                .userType(resolveUserType(requestedUserType, acceptAgreement))
                .entrepreneurAgreementAt(agreementDate(requestedUserType, acceptAgreement))
                .moderator(moderatorEmails.contains(email))
                .authProvider(provider)
                .googleSub(googleSub)
                .isVerified(true)
                .pastoralVerification(false)
                .verificationStep(VerificationStep.verified)
                .build();

        return userRepository.save(user);
    }

    private GoogleIdToken.Payload verifyGoogleIdToken(String idTokenString) {
        try {
            GoogleIdToken idToken = getGoogleIdTokenVerifier().verify(idTokenString);
            if (idToken == null) {
                throw new BadCredentialsException("Token de Google inválido");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new BadCredentialsException("Token de Google inválido");
        }
    }

    private synchronized GoogleIdTokenVerifier getGoogleIdTokenVerifier() {
        if (googleIdTokenVerifier == null) {
            googleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
        }
        return googleIdTokenVerifier;
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

    /**
     * Toda cuenta nace como cliente (FR-001); solo es emprendedora si además acepta el acuerdo de
     * honestidad en la misma petición (FR-002).
     */
    private static UserType resolveUserType(UserType requested, Boolean acceptAgreement) {
        return requested == UserType.entrepreneur && Boolean.TRUE.equals(acceptAgreement)
                ? UserType.entrepreneur
                : UserType.buyer;
    }

    private static Instant agreementDate(UserType requested, Boolean acceptAgreement) {
        return resolveUserType(requested, acceptAgreement) == UserType.entrepreneur ? Instant.now() : null;
    }

    public UserProfileResponse getProfile(User user) {
        return UserProfileResponse.from(user);
    }
}
