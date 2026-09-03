package org.example.controlefinanceiroapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "gastos")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuario_id;

    private Long categoria_id;

    private String descricao;

    private double valor;

    private String formaPagamento;

    private LocalDate data;
}
