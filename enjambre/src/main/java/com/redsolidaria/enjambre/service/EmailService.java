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

    public void enviarCorreoActivacion(String emailDestino) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(emailDestino);
        mensaje.setSubject("🎉 Tu cuenta ha sido activada - Red Solidaria UTP");
        mensaje.setText("Hola,\n\nTu cuenta ha sido activada con éxito. Ya puedes iniciar sesión en la plataforma.\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP");

        mailSender.send(mensaje);
        System.out.println("✓ Correo de activación enviado a: " + emailDestino);
    }

    public void enviarCorreoRechazo(String emailDestino) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(emailDestino);
        mensaje.setSubject("❌ Tu cuenta no fue activada - Red Solidaria UTP");
        mensaje.setText("Hola,\n\nTu cuenta no fue activada porque no cumple los requisitos. Puedes volver a registrarte corrigiendo la información.\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP");

        mailSender.send(mensaje);
        System.out.println("✓ Correo de rechazo enviado a: " + emailDestino);
    }
}