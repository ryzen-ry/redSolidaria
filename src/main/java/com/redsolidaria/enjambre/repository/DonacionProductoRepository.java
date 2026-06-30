package com.redsolidaria.enjambre.repository;

import com.redsolidaria.enjambre.model.DonacionProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DonacionProductoRepository extends JpaRepository<DonacionProducto, Long> {
    List<DonacionProducto> findByEstado(String estado);
    void deleteByUsuario_Id(Long usuarioId);
}
