package com.redsolidaria.enjambre.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class FileController {

    private final Path uploadDir = Paths.get("uploads/documentos/");

    @GetMapping("/uploads/documentos/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> servirDocumento(@PathVariable String filename) {
        try {
            Path filePath = uploadDir.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                String contentType = determinarContentType(filename);
                if ("application/pdf".equals(contentType)) {
                    return pdfPlaceholder(filename);
                } else {
                    return imagenPlaceholder("Documento no disponible",
                            "Este archivo (" + filename + ") no esta disponible en el servidor.\n" +
                            "Los documentos se almacenan localmente y no se migraron a este entorno.\n" +
                            "Para ver los documentos, revisa tu servidor de desarrollo local.");
                }
            }

            String contentType = determinarContentType(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private ResponseEntity<Resource> pdfPlaceholder(String filename) {
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                "<style>" +
                "body{margin:0;font-family:'Inter',sans-serif;background:#F8FAFC;display:flex;align-items:center;justify-content:center;min-height:100vh}" +
                ".card{background:white;border-radius:16px;padding:2rem;box-shadow:0 2px 8px rgba(0,0,0,0.08);text-align:center;max-width:400px}" +
                ".icon{font-size:3rem;margin-bottom:1rem}" +
                "h3{color:#1E293B;margin:0 0 0.5rem}" +
                "p{color:#64748B;font-size:0.9rem;line-height:1.5;margin:0}" +
                ".filename{background:#F1F5F9;border-radius:8px;padding:0.5rem;margin:1rem 0;font-size:0.8rem;color:#475569;word-break:break-all}" +
                "</style></head><body>" +
                "<div class='card'>" +
                "<div class='icon'>" + (char)0x1F4C4 + "</div>" +
                "<h3>Documento no disponible</h3>" +
                "<p>Este archivo no se encuentra en el servidor actual.<br>Los documentos subidos se almacenan localmente y no se migran automaticamente a entornos como Railway.</p>" +
                "<div class='filename'>" + escapeHtml(filename) + "</div>" +
                "</div></body></html>";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8)));
    }

    private ResponseEntity<Resource> imagenPlaceholder(String titulo, String mensaje) {
        String safeTitulo = escapeXml(titulo);
        String safeMensaje = escapeXml(mensaje).replace("\n", "&#10;");

        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='400' height='300' viewBox='0 0 400 300'>" +
                "<rect width='400' height='300' fill='#F8FAFC' rx='12'/>" +
                "<rect x='50' y='40' width='300' height='220' fill='#FFFFFF' rx='8' stroke='#E2E8F0' stroke-width='1'/>" +
                "<text x='200' y='100' text-anchor='middle' font-size='48'>" + (char)0x1F4C4 + "</text>" +
                "<text x='200' y='140' text-anchor='middle' font-size='14' font-weight='600' fill='#1E293B' font-family='Inter,sans-serif'>" + safeTitulo + "</text>" +
                "<foreignObject x='65' y='155' width='270' height='100'>" +
                "<div xmlns='http://www.w3.org/1999/xhtml' style='font-family:Inter,sans-serif;font-size:11px;color:#64748B;text-align:center;line-height:1.5'>" +
                "<p style='margin:4px 0'>" + safeMensaje.replace("&#10;", "<br/>") + "</p>" +
                "</div></foreignObject></svg>";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .body(new ByteArrayResource(svg.getBytes(StandardCharsets.UTF_8)));
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':  sb.append('&').append("amp;"); break;
                case '<':  sb.append('&').append("lt;"); break;
                case '>':  sb.append('&').append("gt;"); break;
                case '"':  sb.append('&').append("quot;"); break;
                case '\'': sb.append('&').append("#039;"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':  sb.append('&').append("amp;"); break;
                case '<':  sb.append('&').append("lt;"); break;
                case '>':  sb.append('&').append("gt;"); break;
                case '"':  sb.append('&').append("quot;"); break;
                case '\'': sb.append('&').append("apos;"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    private String determinarContentType(String filename) {
        if (filename == null) return "application/octet-stream";

        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "application/msword";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "application/vnd.ms-excel";

        return "application/octet-stream";
    }
}