package com.shop.customer.controller;


import com.shop.customer.exception.UserException;
import com.shop.customer.model.ErrorResponse;
import com.shop.customer.model.dto.UserDto;
import com.shop.customer.service.UserService;
import com.shop.customer.service.ValidationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class UserContoller {

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
}
