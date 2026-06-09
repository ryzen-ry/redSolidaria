package com.redsolidaria.enjambre.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.email.from}")
    private String emailFrom;

    private final RestTemplate restTemplate = new RestTemplate();

    private void sendEmailViaBrevo(String emailDestino, String subject, String content) {
        if (emailDestino == null || emailDestino.trim().isEmpty()) {
            System.err.println("❌ ERROR: El correo de destino (to) está vacío o es nulo. No se puede enviar el correo.");
            return;
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("❌ ERROR: La API Key de Brevo no está configurada (brevo.api.key).");
            System.err.println("   → Asegúrate de configurar la variable de entorno BREVO_API_KEY en tu servidor/Docker.");
            System.err.println("   → Puedes obtenerla en Brevo: https://app.brevo.com → SMTP & API → API Keys");
            return;
        }

        if (emailFrom == null || emailFrom.trim().isEmpty()) {
            System.err.println("❌ ERROR: El correo remitente no está configurado (brevo.email.from).");
            return;
        }

        String url = "https://api.brevo.com/v3/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);
        headers.set("accept", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of("name", "Red Solidaria UTP", "email", emailFrom));
        body.put("to", List.of(Map.of("email", emailDestino)));
        body.put("subject", subject);
        body.put("textContent", content);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, entity, String.class);
            System.out.println("✓ Correo enviado con éxito por API HTTP Brevo a: " + emailDestino);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("❌ ERROR de API Brevo (" + e.getStatusCode() + ") al enviar correo a " + emailDestino + ": " + e.getResponseBodyAsString());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ ERROR inesperado al enviar correo por API HTTP Brevo a " + emailDestino + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void enviarCodigoVerificacion(String emailDestino, String codigo) {
        String text = "Hola,\n\nTu codigo de verificacion es: " + codigo +
                        "\n\nEste codigo expira en 10 minutos.\n\n" +
                        "Si no solicitaste este codigo, ignora este mensaje.\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP";
        sendEmailViaBrevo(emailDestino, "Codigo de verificacion - Red Solidaria UTP", text);
    }

    @Async
    public void enviarCorreoActivacion(String emailDestino) {
        String text = "Hola,\n\nTu cuenta ha sido activada con exito. Ya puedes iniciar sesion en la plataforma.\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP";
        sendEmailViaBrevo(emailDestino, "Tu cuenta ha sido activada - Red Solidaria UTP", text);
    }

    @Async
    public void enviarCorreoRechazo(String emailDestino) {
        String text = "Hola,\n\nTu cuenta no fue activada porque no cumple los requisitos. Puedes volver a registrarte corrigiendo la información.\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP";
        sendEmailViaBrevo(emailDestino, "❌ Tu cuenta no fue activada - Red Solidaria UTP", text);
    }

    @Async
    public void enviarConfirmacionMonetaria(String emailDestino, String nombre, Double monto) {
        String text = "Hola " + nombre + ",\n\n" +
                        "Queremos agradecerte de todo corazón por tu generosa donación monetaria de S/. " + String.format("%.2f", monto) + ".\n" +
                        "Tu contribución ha sido verificada y confirmada con éxito. Gracias a ti, podremos seguir brindando apoyo y adquiriendo productos de primera necesidad para quienes más lo necesitan.\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP";
        sendEmailViaBrevo(emailDestino, "💖 ¡Tu donación monetaria ha sido confirmada! - Red Solidaria UTP", text);
    }

    @Async
    public void enviarRechazoMonetaria(String emailDestino, String nombre) {
        String text = "Hola " + nombre + ",\n\n" +
                        "Lamentamos informarte que no hemos podido verificar el código de tu donación monetaria.\n" +
                        "Por este motivo, la donación ha sido marcada como rechazada. Si crees que se trata de un error, por favor ponte en contacto con nosotros o intenta registrarla nuevamente.\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP";
        sendEmailViaBrevo(emailDestino, "⚠️ Actualización sobre tu donación monetaria - Red Solidaria UTP", text);
    }

    @Async
    public void enviarConfirmacionProductoRecoger(String emailDestino, String nombre, String producto, String horario) {
        String text = "Hola " + nombre + ",\n\n" +
                        "Nos alegra informarte que tu donación de producto (" + producto + ") ha sido aprobada.\n" +
                        "Hemos coordinado la entrega bajo la opción de: Recoger en domicilio.\n" +
                        "Un miembro de nuestro equipo se acercará a la dirección proporcionada dentro del horario seleccionado:\n" +
                        "⏰ Horario de recojo: " + horario + "\n\n" +
                        "Por favor, ten el producto listo. ¡Muchas gracias por tu valioso apoyo!\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP";
        sendEmailViaBrevo(emailDestino, "📦 ¡Tu donación de producto ha sido aprobada! (Recojo en domicilio) - Red Solidaria UTP", text);
    }

    @Async
    public void enviarConfirmacionProductoLlevar(String emailDestino, String nombre, String producto, String direccionSede, String horarioAtencion) {
        String text = "Hola " + nombre + ",\n\n" +
                        "Nos alegra informarte que tu donación de producto (" + producto + ") ha sido aprobada.\n" +
                        "Puedes acercarte a nuestra sede para realizar la entrega:\n" +
                        "📍 Dirección de la sede: " + direccionSede + "\n" +
                        "⏰ Horario de atención: " + horarioAtencion + "\n\n" +
                        "¡Muchas gracias por tu valioso apoyo para nuestra comunidad!\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP";
        sendEmailViaBrevo(emailDestino, "📦 ¡Tu donación de producto ha sido aprobada! (Llevar a sede) - Red Solidaria UTP", text);
    }

    @Async
    public void enviarRechazoProducto(String emailDestino, String nombre) {
        String text = "Hola " + nombre + ",\n\n" +
                        "Agradecemos enormemente tu intención de donar.\n" +
                        "Lamentablemente, en esta ocasión no podemos recibir el producto propuesto debido a políticas internas o falta de capacidad de almacenamiento para este tipo de implemento.\n" +
                        "Esperamos poder contar con tu ayuda en futuras oportunidades.\n\n" +
                        "Saludos,\nEquipo Red Solidaria UTP";
        sendEmailViaBrevo(emailDestino, "⚠️ Actualización sobre tu donación de producto - Red Solidaria UTP", text);
    }
}