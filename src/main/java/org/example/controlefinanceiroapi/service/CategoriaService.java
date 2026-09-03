package org.example.controlefinanceiroapi.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.controlefinanceiroapi.entity.Categoria;
import org.example.controlefinanceiroapi.repository.CategoriaRepository;
import org.example.controlefinanceiroapi.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final SecurityUtils securityUtils;

    public CategoriaService(CategoriaRepository categoriaRepository, SecurityUtils securityUtils) {
        this.categoriaRepository = categoriaRepository;
        this.securityUtils = securityUtils;
    }

    public Categoria criarCategoria(String nome) {
        Long usuarioId = securityUtils.getCurrentUserId();
        Categoria novaCategoria = new Categoria();
        novaCategoria.setNome(nome);
        novaCategoria.setUsuario_id(usuarioId);
        return categoriaRepository.save(novaCategoria);
    }

    public List<Categoria> listarTodos() {
        Long usuarioId = securityUtils.getCurrentUserId();
        return categoriaRepository.findAllByUsuarioId(usuarioId);
    }

    public Categoria listarPorId(Long id) {
        Long usuarioId = securityUtils.getCurrentUserId();
        return categoriaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(EntityNotFoundException::new);
    }

    public Categoria editar(Long id, Categoria novaCategoria) {
        Long usuarioId = securityUtils.getCurrentUserId();
        Categoria categoria = categoriaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(EntityNotFoundException::new);

        categoria.setNome(novaCategoria.getNome());
        return categoriaRepository.save(categoria);
    }

    public void excluir(Long id) {
        Long usuarioId = securityUtils.getCurrentUserId();
        if (categoriaRepository.existsByIdAndUsuarioId(id, usuarioId)) {
            categoriaRepository.deleteById(id);
        } else {
            throw new EntityNotFoundException();
        }
    }
}
