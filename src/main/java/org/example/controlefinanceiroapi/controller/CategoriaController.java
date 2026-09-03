package org.example.controlefinanceiroapi.controller;

import jakarta.validation.Valid;
import org.example.controlefinanceiroapi.entity.Categoria;
import org.example.controlefinanceiroapi.service.CategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    public ResponseEntity<Categoria> criar(@RequestBody @Valid Categoria categoria) {
        Categoria criada = categoriaService.criarCategoria(categoria.getNome());
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping
    public ResponseEntity<List<Categoria>> listarTodos() {
        return ResponseEntity.ok(categoriaService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Categoria> listarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.listarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Categoria> editar(@PathVariable Long id, @RequestBody @Valid Categoria categoria) {
        return ResponseEntity.ok(categoriaService.editar(id, categoria));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        categoriaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
