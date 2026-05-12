package com.redsolidaria.enjambre.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void enviarCodigoVerificacion(String emailDestino, String codigo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(emailDestino);
        mensaje.setSubject("🔐 Código de verificación - Red Solidaria UTP");
        mensaje.setText("Hola,\n\nTu código de verificación es: " + codigo +
                        "\n\nEste código expira en 10 minutos.\n\n" +
                        "Si no solicitaste este código, ignora este mensaje.\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP");

        mailSender.send(mensaje);
        System.out.println("✓ Correo enviado a: " + emailDestino + " | Código: " + codigo);
    }
}