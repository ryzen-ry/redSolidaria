package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.DonacionMonetaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DonacionMonetariaRepository extends JpaRepository<DonacionMonetaria, Long> {
    List<DonacionMonetaria> findByEstado(String estado);
    void deleteByUsuario_Id(Long usuarioId);
}
