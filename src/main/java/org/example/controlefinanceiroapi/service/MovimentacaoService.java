package org.example.controlefinanceiroapi.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.controlefinanceiroapi.entity.Movimentacao;
import org.example.controlefinanceiroapi.repository.CategoriaRepository;
import org.example.controlefinanceiroapi.repository.MovimentacaoRepository;
import org.example.controlefinanceiroapi.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MovimentacaoService {

    private final MovimentacaoRepository movimentacaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final SecurityUtils securityUtils;

    public MovimentacaoService(MovimentacaoRepository movimentacaoRepository, CategoriaRepository categoriaRepository, SecurityUtils securityUtils) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.categoriaRepository = categoriaRepository;
        this.securityUtils = securityUtils;
    }

    public Movimentacao criar(Long categoria_id, String descricao, double valor, String tipo, String formaPagamento, LocalDate data) {
        Long usuarioId = securityUtils.getCurrentUserId();

        if (categoria_id != null && !categoriaRepository.existsByIdAndUsuarioId(categoria_id, usuarioId)) {
            throw new IllegalArgumentException("Categoria inválida ou não pertence ao usuário.");
        }

        String tipoNormalizado = (tipo != null && tipo.equalsIgnoreCase("ENTRADA")) ? "ENTRADA" : "SAIDA";

        Movimentacao nova = new Movimentacao();
        nova.setUsuario_id(usuarioId);
        nova.setCategoria_id(categoria_id);
        nova.setDescricao(descricao);
        nova.setValor(valor);
        nova.setTipo(tipoNormalizado);
        nova.setFormaPagamento(formaPagamento);
        nova.setData(data != null ? data : LocalDate.now());

        return movimentacaoRepository.save(nova);
    }

    public Movimentacao criar(Movimentacao mov) {
        return criar(
                mov.getCategoria_id(),
                mov.getDescricao(),
                mov.getValor(),
                mov.getTipo(),
                mov.getFormaPagamento(),
                mov.getData()
        );
    }

    public List<Movimentacao> listarTodos() {
        Long usuarioId = securityUtils.getCurrentUserId();
        return movimentacaoRepository.findAllByUsuarioId(usuarioId);
    }

    public Movimentacao listarPorId(Long id) {
        Long usuarioId = securityUtils.getCurrentUserId();
        return movimentacaoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(EntityNotFoundException::new);
    }

    public Movimentacao editar(Long id, Movimentacao nova) {
        Long usuarioId = securityUtils.getCurrentUserId();
        Movimentacao mov = movimentacaoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(EntityNotFoundException::new);

        if (nova.getCategoria_id() != null && !categoriaRepository.existsByIdAndUsuarioId(nova.getCategoria_id(), usuarioId)) {
            throw new IllegalArgumentException("Categoria inválida ou não pertence ao usuário.");
        }

        mov.setCategoria_id(nova.getCategoria_id());
        mov.setDescricao(nova.getDescricao());
        mov.setValor(nova.getValor());
        if (nova.getTipo() != null) {
            mov.setTipo(nova.getTipo().equalsIgnoreCase("ENTRADA") ? "ENTRADA" : "SAIDA");
        }
        mov.setFormaPagamento(nova.getFormaPagamento());
        if (nova.getData() != null) {
            mov.setData(nova.getData());
        }

        return movimentacaoRepository.save(mov);
    }

    public void excluir(Long id) {
        Long usuarioId = securityUtils.getCurrentUserId();
        if (movimentacaoRepository.existsByIdAndUsuarioId(id, usuarioId)) {
            movimentacaoRepository.deleteById(id);
        } else {
            throw new EntityNotFoundException();
        }
    }
}
