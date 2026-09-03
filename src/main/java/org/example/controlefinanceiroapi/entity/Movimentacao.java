package org.example.controlefinanceiroapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "movimentacoes")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuario_id;

    private Long categoria_id;

    private String descricao;

    private double valor;

    private String tipo; // "ENTRADA" ou "SAIDA"

    private String formaPagamento;

    private LocalDate data;
}
