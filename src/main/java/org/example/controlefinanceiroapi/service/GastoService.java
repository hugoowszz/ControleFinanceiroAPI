package org.example.controlefinanceiroapi.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.controlefinanceiroapi.entity.Gasto;
import org.example.controlefinanceiroapi.repository.CategoriaRepository;
import org.example.controlefinanceiroapi.repository.GastoRepository;
import org.example.controlefinanceiroapi.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GastoService {

    private final GastoRepository gastoRepository;
    private final CategoriaRepository categoriaRepository;
    private final SecurityUtils securityUtils;

    public GastoService(GastoRepository gastoRepository, CategoriaRepository categoriaRepository, SecurityUtils securityUtils) {
        this.gastoRepository = gastoRepository;
        this.categoriaRepository = categoriaRepository;
        this.securityUtils = securityUtils;
    }

    public Gasto criarGasto(Long categoria_id, String descricao, double valor, String formaPagamento, LocalDate data) {
        Long usuarioId = securityUtils.getCurrentUserId();

        if (categoria_id != null && !categoriaRepository.existsByIdAndUsuarioId(categoria_id, usuarioId)) {
            throw new IllegalArgumentException("Categoria inválida ou não pertence ao usuário.");
        }

        Gasto novoGasto = new Gasto();
        novoGasto.setUsuario_id(usuarioId);
        novoGasto.setCategoria_id(categoria_id);
        novoGasto.setDescricao(descricao);
        novoGasto.setValor(valor);
        novoGasto.setFormaPagamento(formaPagamento);
        novoGasto.setData(data != null ? data : LocalDate.now());

        return gastoRepository.save(novoGasto);
    }

    public Gasto criarGasto(Gasto gasto) {
        return criarGasto(
                gasto.getCategoria_id(),
                gasto.getDescricao(),
                gasto.getValor(),
                gasto.getFormaPagamento(),
                gasto.getData()
        );
    }

    public List<Gasto> listarTodos() {
        Long usuarioId = securityUtils.getCurrentUserId();
        return gastoRepository.findAllByUsuarioId(usuarioId);
    }

    public Gasto listarPorId(Long id) {
        Long usuarioId = securityUtils.getCurrentUserId();
        return gastoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(EntityNotFoundException::new);
    }

    public Gasto editar(Long id, Gasto novoGasto) {
        Long usuarioId = securityUtils.getCurrentUserId();
        Gasto gasto = gastoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(EntityNotFoundException::new);

        if (novoGasto.getCategoria_id() != null && !categoriaRepository.existsByIdAndUsuarioId(novoGasto.getCategoria_id(), usuarioId)) {
            throw new IllegalArgumentException("Categoria inválida ou não pertence ao usuário.");
        }

        gasto.setCategoria_id(novoGasto.getCategoria_id());
        gasto.setDescricao(novoGasto.getDescricao());
        gasto.setValor(novoGasto.getValor());
        gasto.setFormaPagamento(novoGasto.getFormaPagamento());
        if (novoGasto.getData() != null) {
            gasto.setData(novoGasto.getData());
        }

        return gastoRepository.save(gasto);
    }

    public void excluir(Long id) {
        Long usuarioId = securityUtils.getCurrentUserId();
        if (gastoRepository.existsByIdAndUsuarioId(id, usuarioId)) {
            gastoRepository.deleteById(id);
        } else {
            throw new EntityNotFoundException();
        }
    }
}
