package com.recoverai.backend.controller;

import com.recoverai.backend.dto.auth.AuthResponseDto;
import com.recoverai.backend.dto.auth.MerchantLoginRequestDto;
import com.recoverai.backend.dto.auth.MerchantRegisterRequestDto;
import com.recoverai.backend.dto.auth.MerchantResponseDto;
import com.recoverai.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<MerchantResponseDto> register(@Valid @RequestBody MerchantRegisterRequestDto request) {
        MerchantResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody MerchantLoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
