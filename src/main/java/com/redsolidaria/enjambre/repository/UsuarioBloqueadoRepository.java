package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.UsuarioBloqueado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioBloqueadoRepository extends JpaRepository<UsuarioBloqueado, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCodigoEstudianteIgnoreCase(String codigoEstudiante);

    boolean existsByDni(String dni);

    boolean existsByCertificadoDiscapacidad(String certificadoDiscapacidad);

    java.util.List<UsuarioBloqueado> findByRol(String rol);
}
