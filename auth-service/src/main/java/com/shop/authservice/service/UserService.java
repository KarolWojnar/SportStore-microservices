package com.shop.authservice.service;

import com.shop.authservice.exception.UserException;
import com.shop.authservice.model.ActivationType;
import com.shop.authservice.model.ProviderType;
import com.shop.authservice.model.Roles;
import com.shop.authservice.model.dto.AuthUser;
import com.shop.authservice.model.dto.ResetPassword;
import com.shop.authservice.model.dto.UserDto;
import com.shop.authservice.model.entity.Activation;
import com.shop.authservice.model.entity.User;
import com.shop.authservice.repository.ActivationRepository;
import com.shop.authservice.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ActivationRepository activationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextWrapper securityContextWrapper;
    private final JwtService jwtUtil;
    @Value("${jwt.exp}")
    private int exp;
    @Value("${jwt.refresh.exp}")
    private int refreshExp;

    @Transactional
    public UserDto createUser(UserDto user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserException("Email already exists.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User newUser = userRepository.save(UserDto.toUserEntity(user));
        Activation activation = activationRepository.save(new Activation(newUser, ActivationType.REGISTRATION));
        //todo: notification-service: send email
        log.info("User created: {}", newUser.getId());
        return UserDto.toUserDto(newUser);
    }

    public Map<String, Object> login(AuthUser authRequest, HttpServletResponse response) {
        if (authRequest.getEmail() == null || authRequest.getPassword() == null) {
            throw new UserException("Email or password is null.");
        }
        User user = userRepository.findByEmailAndProvider(authRequest.getEmail(), ProviderType.LOCAL)
                .orElseThrow(() -> new UserException("User not found."));

        String token = getAuth(authRequest, user);
        String refreshToken = jwtUtil.generateToken(user, refreshExp);
        return getCredentials(token, refreshToken, authRequest.getEmail(), response);
    }


    @Transactional
    public Map<String, Object> loginSuccessGoogle(Map<String, String> tokenGoogle, HttpServletResponse response) {
        String idToken = tokenGoogle.get("idToken");
        String email = jwtUtil.getEmailFromGoogleToken(idToken);
        User user = userRepository.findByEmail(email).orElse(getOrCreateUser(email));
        String refreshToken = jwtUtil.generateToken(user, refreshExp);
        String token = jwtUtil.generateToken(user, exp);
        return getCredentials(token, refreshToken, email, response);
    }

    public User getOrCreateUser(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.orElseGet(() -> createGoogleUser(email));
    }

    private User createGoogleUser(String email) {
        User user = new User();
        user.setPassword(passwordEncoder.encode("XXXXXXXX"));
        user.setEmail(email);
        user.setProvider(ProviderType.GOOGLE);
        user.setRole(Roles.ROLE_CUSTOMER);
        user.setEnabled(true);
        User createdUser = userRepository.save(user);
        log.info("Google user created: {}", createdUser.getId());
        return createdUser;
    }

    private Map<String, Object> getCredentials(String access, String refresh, String email, HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie("Refresh-token", refresh);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        response.addCookie(refreshTokenCookie);
        boolean cartNotEmpty = isCartNotEmpty(email);
        return Map.of("token", access, "cartHasItems", cartNotEmpty);
    }

    private boolean isCartNotEmpty(String email) {
//        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserException("User not found."));
//        Cart cart = cartService.getCart(user.getId());
//        return cart != null && !cart.getProducts().isEmpty();
        //todo: checkCart
        return false;
    }

    public String getAuth(AuthUser authRequest, User user) {
        log.info("Authenticating user: {}", authRequest.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getEmail(),
                            authRequest.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new UserException("Invalid email or password.", e);
        }
        return jwtUtil.generateToken(user, exp);
    }

    @Transactional
    public String recoveryPassword(String email) {
        log.info("Recovery password for email: {}", email);
        User user = userRepository.findByEmailAndEnabled(email, true).orElseThrow(() -> new UserException("Email not found."));
        Activation activation = activationRepository.save(new Activation(user, ActivationType.RESET_PASSWORD));
        //todo: send email
//        emailService.sendEmailResetPassword(email, activation);
        return "Code sent! Check your email.";
    }

    public boolean checkResetCode(String code) {
        Activation activation = activationRepository.findByActivationCodeAndTypeAndExpiresAtAfter(code, ActivationType.RESET_PASSWORD, LocalDateTime.now())
                .orElseThrow(() -> new UserException("Activation code not found."));
        return activation.getActivationCode().equals(code);
    }

    @Transactional
    public String resetPassword(ResetPassword resetPassword) {
        if (!resetPassword.getPassword().equals(resetPassword.getConfirmPassword())) {
            throw new UserException("Passwords do not match.");
        }
        Activation activation = activationRepository.findByActivationCodeAndTypeAndExpiresAtAfter(resetPassword.getCode(), ActivationType.RESET_PASSWORD, LocalDateTime.now())
                .orElseThrow(() -> new UserException("Activation code not found."));
        User user = userRepository.findById(activation.getUser().getId())
                .orElseThrow(() -> new UserException("User not found."));
        user.setPassword(passwordEncoder.encode(resetPassword.getPassword()));
        userRepository.save(user);
        activationRepository.delete(activation);
        return "Password reset successfully.";
    }

    public void logout(HttpServletResponse response, HttpServletRequest request) {
        String accessToken = extractAccessToken(request);
        if (accessToken != null) {
            long expirationTime = jwtUtil.extractExpiration(accessToken).getTime() - System.currentTimeMillis();
            jwtUtil.addToBlackList(accessToken, expirationTime, "access");
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("Refresh-token".equals(cookie.getName())) {
                    long expirationRefreshTime = jwtUtil.extractExpiration(cookie.getValue()).getTime() - System.currentTimeMillis();
                    jwtUtil.addToBlackList(cookie.getValue(), expirationRefreshTime, "refresh");
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
        SecurityContextHolder.clearContext();
    }


    private String extractAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    @Transactional
    public Map<String, String> activate(String activationCode) {
        Activation activation = activationRepository.findByActivationCodeAndTypeAndExpiresAtAfter(activationCode, ActivationType.REGISTRATION, LocalDateTime.now())
                .orElseThrow(() -> new UserException("Activation code not found."));
        User user = userRepository.findById(activation.getUser().getId())
                .orElseThrow(() -> new UserException("User not found."));
        user.setEnabled(true);
        userRepository.save(user);
        activationRepository.delete(activation);
        log.info("Account {} activated successfully", user.getId());
        return Map.of("email", user.getEmail());
    }

    public boolean isLoggedIn() {
        try {
            Optional<User> user = securityContextWrapper.getCurrentUser();
            return user.isPresent() && user.get().isEnabled();
        } catch (Exception e) {
            throw new UserException("User not found.", e);
        }
    }

    public String getRole() {
        try {
            User user = securityContextWrapper.getCurrentUser()
                    .orElseThrow(() -> new UserException("User not found."));
            return user.getRole().name();
        } catch (Exception e) {
            throw new UserException("User not found.", e);
        }
    }

    public void validateToken(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = extractAccessToken(request);
        String refreshToken;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("Refresh-token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    if (jwtUtil.isBlacklisted(refreshToken)) {
                        logout(response, request);
                        throw new UserException("Token is in blacklist");
                    }
                }
            }
        }
        if (accessToken != null && jwtUtil.isBlacklisted(accessToken)) {
            logout(response, request);
            throw new UserException("Token is in blacklist");
        }
    }
}
