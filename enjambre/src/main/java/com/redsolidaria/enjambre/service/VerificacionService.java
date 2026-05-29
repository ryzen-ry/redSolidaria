package com.redsolidaria.enjambre.service;

import com.redsolidaria.enjambre.model.CodigoVerificacion;
import com.redsolidaria.enjambre.repository.CodigoVerificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class VerificacionService {

    @Autowired
    private CodigoVerificacionRepository codigoRepository;

    @Autowired
    private EmailService emailService;

    public String generarCodigo() {
        Random random = new Random();
        int codigo = 100000 + random.nextInt(900000);
        return String.valueOf(codigo);
    }

    public void enviarCodigo(String email) {
        String codigo = generarCodigo();
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(10);

        CodigoVerificacion cv = new CodigoVerificacion(email, codigo, expiracion);
        codigoRepository.save(cv);

        emailService.enviarCodigoVerificacion(email, codigo);
    }

    public boolean verificarCodigo(String email, String codigoIngresado) {
        var optionalCodigo = codigoRepository.findByEmailAndCodigoAndUsadoFalse(email, codigoIngresado);

        if (optionalCodigo.isEmpty()) {
            return false;
        }

        CodigoVerificacion cv = optionalCodigo.get();

        if (cv.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            return false;
        }

        cv.setUsado(true);
        codigoRepository.save(cv);
        
        // ❌ ELIMINADO: Ya no se marca como verificado aquí
        // El guardado en BD y marcado como verificado ahora se hace en AuthController
        // después de que el código es válido
        
        return true;
    }
}