package org.example.controlefinanceiroapi.controller;

import jakarta.validation.Valid;
import org.example.controlefinanceiroapi.entity.Gasto;
import org.example.controlefinanceiroapi.service.GastoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gastos")
public class GastoController {

    private final GastoService gastoService;

    public GastoController(GastoService gastoService) {
        this.gastoService = gastoService;
    }

    @PostMapping
    public ResponseEntity<Gasto> criar(@RequestBody @Valid Gasto gasto) {
        Gasto criado = gastoService.criarGasto(
                gasto.getCategoria_id(),
                gasto.getDescricao(),
                gasto.getValor(),
                gasto.getFormaPagamento(),
                gasto.getData()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<Gasto>> listarTodos() {
        return ResponseEntity.ok(gastoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Gasto> listarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(gastoService.listarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Gasto> editar(@PathVariable Long id, @RequestBody @Valid Gasto gasto) {
        return ResponseEntity.ok(gastoService.editar(id, gasto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        gastoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
