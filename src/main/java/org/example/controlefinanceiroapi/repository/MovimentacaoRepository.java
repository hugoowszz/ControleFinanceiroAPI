package org.example.controlefinanceiroapi.repository;

import org.example.controlefinanceiroapi.entity.Movimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    @Query("SELECT m FROM Movimentacao m WHERE m.usuario_id = :usuarioId")
    List<Movimentacao> findAllByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT m FROM Movimentacao m WHERE m.id = :id AND m.usuario_id = :usuarioId")
    Optional<Movimentacao> findByIdAndUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Movimentacao m WHERE m.id = :id AND m.usuario_id = :usuarioId")
    boolean existsByIdAndUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
