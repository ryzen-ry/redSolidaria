package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.UbicacionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UbicacionUsuarioRepository extends JpaRepository<UbicacionUsuario, Long> {

    Optional<UbicacionUsuario> findByUsuario_Id(Long usuarioId);

    List<UbicacionUsuario> findByUsuario_RolAndActualizadoEnAfter(String rol, LocalDateTime after);

    void deleteByUsuario_Id(Long usuarioId);
}

