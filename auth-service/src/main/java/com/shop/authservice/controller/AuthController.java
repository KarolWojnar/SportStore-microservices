package com.shop.authservice.controller;

import com.shop.authservice.model.SuccessResponse;
import com.shop.authservice.model.dto.*;
import com.shop.authservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        return ResponseEntity.ok((userService.validateToken(request, response)));
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

    @GetMapping("/admin")
    public ResponseEntity<?> getUser(@RequestHeader("X-User-Role") @NonNull String userRole,
                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                     @RequestParam(value = "search", defaultValue = "", required = false) String search,
                                     @RequestParam(value = "role", defaultValue = "", required = false) String role,
                                     @RequestParam(value = "enabled", defaultValue = "", required = false) Boolean enabled){
        List<UserAdminInfo> users = userService.getAllUsers(userRole, page, search, role, enabled);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/admin/{id}")
    public ResponseEntity<?> changeUserStatus(@RequestHeader("X-User-Role") @NonNull String userRole,
                                              @PathVariable String id, @RequestBody UserStatusRequest status){
        userService.changeUserStatus(userRole, id, status);
        return ResponseEntity.ok(new SuccessResponse("User status changed"));
    }

    @PatchMapping("/admin/{id}/role")
    public ResponseEntity<?> setAdmin(@RequestHeader("X-User-Role") @NonNull String userRole,
                                            @PathVariable String id) {
        userService.setAdmin(userRole, id);
        return ResponseEntity.ok(new SuccessResponse("User role changed"));
    }
}
