package org.example.controlefinanceiroapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private String token;
    private String tipo = "Bearer";
    private Long usuarioId;
    private String email;
    private String nome;

    public LoginResponse(String token, Long usuarioId, String email, String nome) {
        this.token = token;
        this.tipo = "Bearer";
        this.usuarioId = usuarioId;
        this.email = email;
        this.nome = nome;
    }
}
