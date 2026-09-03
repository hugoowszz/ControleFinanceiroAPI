package org.example.controlefinanceiroapi.controller;

import jakarta.validation.Valid;
import org.example.controlefinanceiroapi.dto.LoginRequest;
import org.example.controlefinanceiroapi.dto.LoginResponse;
import org.example.controlefinanceiroapi.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        LoginResponse response = usuarioService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
}
