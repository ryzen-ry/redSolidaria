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

    public void enviarConfirmacionMonetaria(String emailDestino, String nombre, Double monto) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(emailDestino);
        mensaje.setSubject("💖 ¡Tu donación monetaria ha sido confirmada! - Red Solidaria UTP");
        mensaje.setText("Hola " + nombre + ",\n\n" +
                        "Queremos agradecerte de todo corazón por tu generosa donación monetaria de S/. " + String.format("%.2f", monto) + ".\n" +
                        "Tu contribución ha sido verificada y confirmada con éxito. Gracias a ti, podremos seguir brindando apoyo y adquiriendo productos de primera necesidad para quienes más lo necesitan.\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP");

        mailSender.send(mensaje);
        System.out.println("✓ Correo de confirmación monetaria enviado a: " + emailDestino + " | Monto: S/. " + monto);
    }

    public void enviarRechazoMonetaria(String emailDestino, String nombre) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(emailDestino);
        mensaje.setSubject("⚠️ Actualización sobre tu donación monetaria - Red Solidaria UTP");
        mensaje.setText("Hola " + nombre + ",\n\n" +
                        "Lamentamos informarte que no hemos podido verificar el código de tu donación monetaria.\n" +
                        "Por este motivo, la donación ha sido marcada como rechazada. Si crees que se trata de un error, por favor ponte en contacto con nosotros o intenta registrarla nuevamente.\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP");

        mailSender.send(mensaje);
        System.out.println("✓ Correo de rechazo monetaria enviado a: " + emailDestino);
    }

    public void enviarConfirmacionProductoRecoger(String emailDestino, String nombre, String producto, String horario) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(emailDestino);
        mensaje.setSubject("📦 ¡Tu donación de producto ha sido aprobada! (Recojo en domicilio) - Red Solidaria UTP");
        mensaje.setText("Hola " + nombre + ",\n\n" +
                        "Nos alegra informarte que tu donación de producto (" + producto + ") ha sido aprobada.\n" +
                        "Hemos coordinado la entrega bajo la opción de: Recoger en domicilio.\n" +
                        "Un miembro de nuestro equipo se acercará a la dirección proporcionada dentro del horario seleccionado:\n" +
                        "⏰ Horario de recojo: " + horario + "\n\n" +
                        "Por favor, ten el producto listo. ¡Muchas gracias por tu valioso apoyo!\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP");

        mailSender.send(mensaje);
        System.out.println("✓ Correo de recojo de producto enviado a: " + emailDestino + " | Producto: " + producto);
    }

    public void enviarConfirmacionProductoLlevar(String emailDestino, String nombre, String producto, String direccionSede, String horarioAtencion) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(emailDestino);
        mensaje.setSubject("📦 ¡Tu donación de producto ha sido aprobada! (Llevar a sede) - Red Solidaria UTP");
        mensaje.setText("Hola " + nombre + ",\n\n" +
                        "Nos alegra informarte que tu donación de producto (" + producto + ") ha sido aprobada.\n" +
                        "Puedes acercarte a nuestra sede para realizar la entrega:\n" +
                        "📍 Dirección de la sede: " + direccionSede + "\n" +
                        "⏰ Horario de atención: " + horarioAtencion + "\n\n" +
                        "¡Muchas gracias por tu valioso apoyo para nuestra comunidad!\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP");

        mailSender.send(mensaje);
        System.out.println("✓ Correo de entrega de producto en sede enviado a: " + emailDestino + " | Producto: " + producto);
    }

    public void enviarRechazoProducto(String emailDestino, String nombre) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(emailDestino);
        mensaje.setSubject("⚠️ Actualización sobre tu donación de producto - Red Solidaria UTP");
        mensaje.setText("Hola " + nombre + ",\n\n" +
                        "Agradecemos enormemente tu intención de donar.\n" +
                        "Lamentablemente, en esta ocasión no podemos recibir el producto propuesto debido a políticas internas o falta de capacidad de almacenamiento para este tipo de implemento.\n" +
                        "Esperamos poder contar con tu ayuda en futuras oportunidades.\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP");

        mailSender.send(mensaje);
        System.out.println("✓ Correo de rechazo producto enviado a: " + emailDestino);
    }
}