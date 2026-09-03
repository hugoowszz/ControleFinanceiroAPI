package org.example.controlefinanceiroapi.repository;

import org.example.controlefinanceiroapi.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    @Query("SELECT c FROM Categoria c WHERE c.usuario_id = :usuarioId")
    List<Categoria> findAllByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT c FROM Categoria c WHERE c.id = :id AND c.usuario_id = :usuarioId")
    Optional<Categoria> findByIdAndUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Categoria c WHERE c.id = :id AND c.usuario_id = :usuarioId")
    boolean existsByIdAndUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
