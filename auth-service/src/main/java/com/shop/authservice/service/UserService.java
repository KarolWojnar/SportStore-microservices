package com.shop.authservice.service;

import com.shop.authservice.exception.UserException;
import com.shop.authservice.model.ActivationType;
import com.shop.authservice.model.Roles;
import com.shop.authservice.model.dto.AuthUser;
import com.shop.authservice.model.dto.ResetPassword;
import com.shop.authservice.model.dto.UserDetailsDto;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ActivationRepository activationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
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
        String token = getAuth(authRequest);
        String refreshToken = jwtUtil.generateToken(authRequest.getEmail(), refreshExp);

        Cookie refreshTokenCookie = new Cookie("Refresh-token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        response.addCookie(refreshTokenCookie);
        boolean cartNotEmpty = isCartNotEmpty(authRequest.getEmail());
        return Map.of("token", token, "cartHasItems", cartNotEmpty);
    }

    private boolean isCartNotEmpty(String email) {
//        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserException("User not found."));
//        Cart cart = cartService.getCart(user.getId());
//        return cart != null && !cart.getProducts().isEmpty();
        //todo: checkCart
        return false;
    }

    public String getAuth(AuthUser authRequest) {
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
        return jwtUtil.generateToken(authRequest.getEmail(), exp);
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

    public UserDetailsDto getUserDetails() {
        User user = userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new UserException("User not found."));
//        Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
        return UserDetailsDto.toDto(user);
    }


    public List<UserDetailsDto> getAllUsers(int page, String search, String role, String enabled) {
        Pageable pageable = PageRequest.of(page, 10);

        String emailSearch = (search != null && !search.isEmpty()) ? "%" + search.toLowerCase() + "%" : null;
        Roles roleEnum = (role != null && !role.isEmpty()) ? Roles.valueOf(role) : null;
        Boolean enabledBool = (enabled != null && !enabled.isEmpty()) ? Boolean.parseBoolean(enabled) : null;

        List<User> users;
        if (emailSearch != null || roleEnum != null || enabledBool != null) {
            users = userRepository.findByFilters(emailSearch, roleEnum, enabledBool, pageable);
        } else {
            users = userRepository.findAll(pageable).getContent();
        }

        if (users.isEmpty()) {
            throw new UsernameNotFoundException("No users found.");
        }

        return users.stream()
                .map(UserDetailsDto::toDto)
                .collect(Collectors.toList());
    }

    public void changeUserStatus(Long id, boolean status) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserException("User not found."));
        user.setEnabled(status);
        userRepository.save(user);
        log.info("Changed status of user {} to {}", user.getId(), status);
    }

    public void changeUserRole(Long id, String role) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserException("User not found."));
        user.setRole(Roles.valueOf(role));
        userRepository.save(user);
        log.info("Changed role of user {} to {}", user.getId(), role);
    }

    public void logout(HttpServletResponse response, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        long expirationTime = jwtUtil.extractExpiration(extractAccessToken(request)).getTime() - System.currentTimeMillis();
        jwtUtil.addToBlackList(extractAccessToken(request), expirationTime, "access");

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
            Optional<User> user = userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
            return user.isPresent() && user.get().isEnabled();
        } catch (Exception e) {
            throw new UserException("User not found.", e);
        }
    }

    public String getRole() {
        try {
            User user = userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName())
                    .orElseThrow(() -> new UserException("User not found."));
            return user.getRole().name();
        } catch (Exception e) {
            throw new UserException("User not found.", e);
        }
    }

}
