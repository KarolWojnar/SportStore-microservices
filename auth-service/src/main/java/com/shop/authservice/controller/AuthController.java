package com.shop.authservice.controller;

import com.shop.authservice.model.SuccessResponse;
import com.shop.authservice.model.dto.AuthUser;
import com.shop.authservice.model.dto.LoginStatus;
import com.shop.authservice.model.dto.ResetPassword;
import com.shop.authservice.model.dto.UserDto;
import com.shop.authservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDto user) {
        return new ResponseEntity<>(userService.createUser(user), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthUser authRequest, HttpServletResponse response) {
        return ResponseEntity.ok(userService.login(authRequest, response));
    }

    @PostMapping("/google")
    public ResponseEntity<?> loginSuccess(@RequestBody Map<String, String> idToken, HttpServletResponse response) {
        return ResponseEntity.ok(userService.loginSuccessGoogle(idToken, response));
    }

    @PostMapping("/recovery-password")
    public ResponseEntity<?> recoveryPassword(@RequestBody String email) {
        return ResponseEntity.ok(new SuccessResponse(userService.recoveryPassword(email)));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(HttpServletRequest request, HttpServletResponse response) {
        userService.validateToken(request, response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-reset-code/{code}")
    public ResponseEntity<?> checkResetCode(@PathVariable String code) {
        return ResponseEntity.ok(userService.checkResetCode(code));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPassword resetPassword) {
        return ResponseEntity.ok(new SuccessResponse(userService.resetPassword(resetPassword)));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout(HttpServletResponse response, HttpServletRequest request) {
        userService.logout(response, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activate/{activationCode}")
    public ResponseEntity<?> activate(@PathVariable String activationCode) {
        return ResponseEntity.ok(userService.activate(activationCode));
    }

    @GetMapping("/isLoggedIn")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> isLoggedIn() {
        return ResponseEntity.ok(new LoginStatus(userService.isLoggedIn()));
    }

    @GetMapping("/role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getRole() {
        return ResponseEntity.ok(new LoginStatus(userService.getRole()));
    }
}
