package com.shop.authservice.service;

import com.shop.authservice.exception.UserException;
import com.shop.authservice.model.ActivationType;
import com.shop.authservice.model.ProviderType;
import com.shop.authservice.model.Roles;
import com.shop.authservice.model.dto.*;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ActivationRepository activationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextWrapper securityContextWrapper;
    private final KafkaEventService kafkaEventService;
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
        kafkaEventService.sendRegistrationEvent(newUser.getEmail(), activation);

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
        return getCredentials(token, refreshToken, user.getId(), response);
    }


    @Transactional
    public Map<String, Object> loginSuccessGoogle(Map<String, String> tokenGoogle, HttpServletResponse response) {
        String idToken = tokenGoogle.get("idToken");
        String email = jwtUtil.getEmailFromGoogleToken(idToken);
        User user = userRepository.findByEmail(email).orElse(createGoogleUser(email));
        String refreshToken = jwtUtil.generateToken(user, refreshExp);
        String token = jwtUtil.generateToken(user, exp);
        return getCredentials(token, refreshToken, user.getId(), response);
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

    private Map<String, Object> getCredentials(String access, String refresh, Long userId, HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie("Refresh-token", refresh);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        response.addCookie(refreshTokenCookie);
        boolean cartNotEmpty = isCartNotEmpty(userId);
        return Map.of("token", access, "cartHasItems", cartNotEmpty);
    }

    private boolean isCartNotEmpty(Long userId) throws RuntimeException {
        try {
            return kafkaEventService.checkCartNotEmptyRequest(userId)
                    .get(2, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("Error while checking cart for user {}", userId, e);
            return false;
        }
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
        User user = userRepository
                .findByEmailAndEnabledAndProvider(email, true, ProviderType.LOCAL)
                .orElseThrow(() -> new UserException("Email not found."));
        Activation activation = activationRepository.save(new Activation(user, ActivationType.RESET_PASSWORD));
        kafkaEventService.sendPasswordResetEvent(user.getEmail(), activation);
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

    public ValidUser validateToken(HttpServletRequest request, HttpServletResponse response) {
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
        Optional<User> user = securityContextWrapper.getCurrentUser();
        ValidUser validUser = new ValidUser();
        if (user.isPresent()) {
            validUser.setRole(user.get().getRole().name());
            validUser.setUserId(user.get().getId());
            validUser.setEmail(user.get().getEmail());
        }
        return validUser;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void clearInactive() {
        List<Activation> codes = activationRepository.findAllByExpiresAtBefore(java.time.LocalDateTime.now());
        List<User> users = codes.stream()
                .filter(c -> c.getType() == ActivationType.REGISTRATION)
                .map(Activation::getUser)
                .toList();
        activationRepository.deleteAll(codes);
        userRepository.deleteAll(users);
        log.info("Deleted {} accounts.", users.size());
    }

    public List<UserAdminInfo> getAllUsers(String userRole, int page, String search, String role, Boolean enabled) {
        if (!userRole.equals(Roles.ROLE_ADMIN.name())) {
            throw new UserException("You are not authorized to view this page.");
        }

        try {
            Pageable pageable = PageRequest.of(page, 10);
            Page<User> users;

            boolean hasSearch = search != null && !search.isEmpty();
            boolean hasRole = role != null && !role.isEmpty();
            boolean hasEnabled = enabled != null;

            if (!hasSearch && !hasRole && !hasEnabled) {
                users = userRepository.findAll(pageable);
            } else {
                Roles roleEnum = hasRole ? Roles.valueOf(role) : null;
                users = userRepository.findAllBySearch(
                        hasSearch ? "%" + search.toLowerCase() + "%" : null,
                        roleEnum,
                        hasEnabled ? enabled : null,
                        pageable
                );
            }
            List<UserCustomerDto> customerDto = kafkaEventService.getCustomerByUserIds(users.getContent().stream().map(User::getId).toList())
                    .get(5, TimeUnit.SECONDS);
            List<UserAdminInfo> userAdminList = new ArrayList<>();
            for (User user : users) {
                UserCustomerDto customer = customerDto.stream()
                        .filter(c -> c.getId().equals(user.getId()))
                        .findFirst()
                        .orElse(null);
                userAdminList.add(UserAdminInfo.toDto(user, customer));
            }
            return userAdminList;
        } catch (Exception e) {
            throw new UserException("Error while getting users.", e);
        }
    }

    public void changeUserStatus(String userRole, String id, UserStatusRequest status) {
        if (!userRole.equals(Roles.ROLE_ADMIN.name())) {
            throw new UserException("You are not authorized to view this page.");
        }
        try {
            User user = userRepository.findById(Long.parseLong(id))
                    .orElseThrow(() -> new UserException("User not found."));
            user.setEnabled(status.isUserStatus());
            userRepository.save(user);
        } catch (Exception e) {
            throw new UserException("Error while changing user status.", e);
        }
    }

    public void setAdmin(String userRole, String id) {
        if (!userRole.equals(Roles.ROLE_ADMIN.name())) {
            throw new UserException("You are not authorized to view this page.");
        }
        try {
            User user = userRepository.findById(Long.parseLong(id))
                    .orElseThrow(() -> new UserException("User not found."));
            user.setRole(Roles.ROLE_ADMIN);
            userRepository.save(user);

        } catch (Exception e) {
            throw new UserException("Error while changing user role.", e);
        }
    }
}
