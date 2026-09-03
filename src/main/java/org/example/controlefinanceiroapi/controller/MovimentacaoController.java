package org.example.controlefinanceiroapi.controller;

import org.example.controlefinanceiroapi.entity.Movimentacao;
import org.example.controlefinanceiroapi.service.MovimentacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movimentacoes")
public class MovimentacaoController {

    private final MovimentacaoService movimentacaoService;

    public MovimentacaoController(MovimentacaoService movimentacaoService) {
        this.movimentacaoService = movimentacaoService;
    }

    @PostMapping
    public ResponseEntity<Movimentacao> criar(@RequestBody Movimentacao movimentacao) {
        Movimentacao criada = movimentacaoService.criar(movimentacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping
    public ResponseEntity<List<Movimentacao>> listarTodos() {
        return ResponseEntity.ok(movimentacaoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movimentacao> listarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(movimentacaoService.listarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Movimentacao> editar(@PathVariable Long id, @RequestBody Movimentacao movimentacao) {
        return ResponseEntity.ok(movimentacaoService.editar(id, movimentacao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        movimentacaoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
