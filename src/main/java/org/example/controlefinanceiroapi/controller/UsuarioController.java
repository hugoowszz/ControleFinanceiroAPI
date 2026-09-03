package org.example.controlefinanceiroapi.controller;

import jakarta.validation.Valid;
import org.example.controlefinanceiroapi.entity.Usuario;
import org.example.controlefinanceiroapi.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<Usuario> criar(@RequestBody @Valid Usuario usuario) {
        Usuario criado = usuarioService.criarUsuario(usuario.getNome(), usuario.getEmail(), usuario.getSenha());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> listarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.listarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> editar(@PathVariable Long id, @RequestBody @Valid Usuario usuario) {
        return ResponseEntity.ok(usuarioService.editar(id, usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        usuarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
