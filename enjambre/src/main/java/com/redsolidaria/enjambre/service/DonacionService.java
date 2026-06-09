package com.redsolidaria.enjambre.service;

import com.redsolidaria.enjambre.dto.DonacionMonetariaDTO;
import com.redsolidaria.enjambre.dto.DonacionProductoDTO;
import com.redsolidaria.enjambre.model.DonacionMonetaria;
import com.redsolidaria.enjambre.model.DonacionProducto;
import com.redsolidaria.enjambre.repository.DonacionMonetariaRepository;
import com.redsolidaria.enjambre.repository.DonacionProductoRepository;
import com.redsolidaria.enjambre.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonacionService {

    private final DonacionMonetariaRepository monetariaRepository;
    private final DonacionProductoRepository productoRepository;
    private final UsuarioService usuarioService;

    @Transactional
    public Long guardarDonacionTemporal(DonacionMonetariaDTO dto, Long usuarioId) {
        DonacionMonetaria donacion = new DonacionMonetaria();
        donacion.setMonto(dto.getMonto());
        donacion.setNombreCompleto(dto.getNombreCompleto());
        donacion.setCelular(dto.getCelular());
        donacion.setEmail(dto.getEmail());
        donacion.setEstado("PENDIENTE");
        donacion.setFechaDonacion(LocalDateTime.now());
        var usuario = usuarioService.buscarPorId(usuarioId);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId);
        }
        donacion.setUsuario(usuario);

        donacion = monetariaRepository.save(donacion);
        return donacion.getId();
    }

    @Transactional
    public void confirmarCodigoYape(Long donacionId, String codigoYape) {
        DonacionMonetaria donacion = monetariaRepository.findById(donacionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la donación con ID: " + donacionId));
        donacion.setCodigoYape(codigoYape);
        donacion.setEstado("VERIFICANDO");
        monetariaRepository.save(donacion);
    }

    @Transactional
    public void guardarDonacionProducto(DonacionProductoDTO dto, Long usuarioId) {
        DonacionProducto donacion = new DonacionProducto();
        donacion.setTipoProducto(dto.getTipoProducto());
        donacion.setEstadoProducto(dto.getEstadoProducto());
        donacion.setNombreCompleto(dto.getNombreCompleto());
        donacion.setEmail(dto.getEmail());
        donacion.setTelefono(dto.getTelefono());
        donacion.setOpcionEntrega(dto.getOpcionEntrega());
        
        if ("recoger".equalsIgnoreCase(dto.getOpcionEntrega())) {
            donacion.setDireccion(dto.getDireccion());
            donacion.setHorario(dto.getHorario());
        } else {
            donacion.setDireccion(null);
            donacion.setHorario(null);
        }
        
        donacion.setComentarios(dto.getComentarios());
        donacion.setEstado("PENDIENTE");
        donacion.setFechaDonacion(LocalDateTime.now());
        var usuario = usuarioService.buscarPorId(usuarioId);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId);
        }
        donacion.setUsuario(usuario);

        productoRepository.save(donacion);
    }

    @Transactional
    public DonacionMonetaria confirmarDonacionMonetaria(Long id) {
        DonacionMonetaria donacion = monetariaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la donación monetaria con ID: " + id));
        donacion.setEstado("CONFIRMADO");
        return monetariaRepository.save(donacion);
    }

    @Transactional
    public void rechazarDonacionMonetaria(Long id) {
        DonacionMonetaria donacion = monetariaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la donación monetaria con ID: " + id));
        donacion.setEstado("RECHAZADO");
        monetariaRepository.save(donacion);
    }

    @Transactional
    public DonacionProducto confirmarDonacionProducto(Long id) {
        DonacionProducto donacion = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la donación de producto con ID: " + id));
        donacion.setEstado("CONFIRMADO");
        return productoRepository.save(donacion);
    }

    @Transactional
    public void rechazarDonacionProducto(Long id) {
        DonacionProducto donacion = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la donación de producto con ID: " + id));
        donacion.setEstado("RECHAZADO");
        productoRepository.save(donacion);
    }

    public List<DonacionMonetaria> obtenerTodasMonetarias() {
        return monetariaRepository.findAll();
    }

    public List<DonacionProducto> obtenerTodasProductos() {
        return productoRepository.findAll();
    }

    public List<DonacionMonetaria> obtenerMonetariasPorEstado(String estado) {
        return monetariaRepository.findByEstado(estado);
    }

    public List<DonacionProducto> obtenerProductosPorEstado(String estado) {
        return productoRepository.findByEstado(estado);
    }
}
