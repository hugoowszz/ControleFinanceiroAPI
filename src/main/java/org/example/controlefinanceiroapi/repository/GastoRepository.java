package org.example.controlefinanceiroapi.repository;

import org.example.controlefinanceiroapi.entity.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long> {

    @Query("SELECT g FROM Gasto g WHERE g.usuario_id = :usuarioId")
    List<Gasto> findAllByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT g FROM Gasto g WHERE g.id = :id AND g.usuario_id = :usuarioId")
    Optional<Gasto> findByIdAndUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM Gasto g WHERE g.id = :id AND g.usuario_id = :usuarioId")
    boolean existsByIdAndUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
