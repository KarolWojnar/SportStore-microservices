package com.shop.authservice.controller;

import com.shop.authservice.exception.UserException;
import com.shop.authservice.model.ErrorResponse;
import com.shop.authservice.model.dto.AuthUser;
import com.shop.authservice.model.dto.ResetPassword;
import com.shop.authservice.model.dto.UserDto;
import com.shop.authservice.service.UserService;
import com.shop.authservice.service.ValidationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDto user, BindingResult br){
        if (br.hasErrors()) {
            return ResponseEntity.badRequest().body(ValidationUtil.buildValidationErrors(br));
        }
        try {
            return new ResponseEntity<>(Map.of("user", userService.createUser(user)), HttpStatus.CREATED);
        } catch (UserException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Validation failed", Map.of("email", e.getMessage())));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthUser authRequest, HttpServletResponse response) {
        try {
            return ResponseEntity.ok(userService.login(authRequest, response));
        } catch (UserException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/recovery-password")
    public ResponseEntity<?> recoveryPassword(@RequestBody String email) {
        try {
            return ResponseEntity.ok(Map.of("message", userService.recoveryPassword(email)));
        } catch (UserException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/check-reset-code/{code}")
    public ResponseEntity<?> checkResetCode(@PathVariable String code) {
        try {
            return ResponseEntity.ok(userService.checkResetCode(code));
        } catch (UserException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPassword resetPassword) {
        try {
            return ResponseEntity.ok(Map.of("message", userService.resetPassword(resetPassword)));
        } catch (UserException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout(HttpServletResponse response, HttpServletRequest request) {
        try {
            userService.logout(response, request);
            return ResponseEntity.noContent().build();
        } catch (UserException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/activate/{activationCode}")
    public ResponseEntity<?> activate(@PathVariable String activationCode) {
        try {
            return ResponseEntity.ok(userService.activate(activationCode));
        } catch (UserException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/isLoggedIn")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> isLoggedIn() {
        try {
            return ResponseEntity.ok(Map.of("isLoggedIn", userService.isLoggedIn()));
        } catch (UserException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getRole() {
        try {
            return ResponseEntity.ok(Map.of("role", userService.getRole()));
        } catch (UserException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

}
