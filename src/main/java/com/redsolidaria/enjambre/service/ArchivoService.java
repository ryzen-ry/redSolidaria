package com.redsolidaria.enjambre.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ArchivoService {

    /**
     * Guarda la evidencia de una incidencia en la ruta local y de target.
     * Estructura: uploads/incidentes/YYYY/MM/DD/
     * Nombre único: YYYYMMDD_HHMMSS_UUID.ext
     * 
     * @param archivo el archivo enviado por el usuario
     * @return la ruta relativa web del archivo guardado (ej. /uploads/incidentes/2026/06/25/archivo.jpg)
     */
    public String guardarEvidencia(MultipartFile archivo) throws IOException {
        validarArchivo(archivo);

        LocalDateTime now = LocalDateTime.now();
        String year = String.format("%04d", now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String day = String.format("%02d", now.getDayOfMonth());

        // Generar ruta relativa y nombre de archivo único
        String relPath = "uploads/incidentes/" + year + "/" + month + "/" + day + "/";
        
        String originalFilename = archivo.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = now.format(formatter);
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String nombreArchivo = timestamp + "_" + uuid + extension;

        // Directorios físicos en src y target
        String srcDir = "src/main/resources/static/" + relPath;
        String targetDir = "target/classes/static/" + relPath;

        // Crear directorios si no existen
        new File(srcDir).mkdirs();
        new File(targetDir).mkdirs();

        byte[] bytes = archivo.getBytes();

        // Guardar en src/main/resources/static/uploads/incidentes/YYYY/MM/DD/
        Path pathSrc = Paths.get(srcDir + nombreArchivo);
        Files.write(pathSrc, bytes);

        // Guardar en target/classes/static/uploads/incidentes/YYYY/MM/DD/ (para recarga en caliente)
        Path pathTarget = Paths.get(targetDir + nombreArchivo);
        Files.write(pathTarget, bytes);

        // Devolver la ruta web relativa
        return "/" + relPath + nombreArchivo;
    }

    /**
     * Elimina el archivo físico de evidencia si existe en el disco.
     * 
     * @param ruta la ruta relativa web del archivo (ej. /uploads/incidentes/2026/06/25/archivo.jpg)
     */
    public void eliminarEvidencia(String ruta) {
        if (ruta == null || ruta.isEmpty()) {
            return;
        }

        // Quitar la barra diagonal inicial si está presente
        String cleanRuta = ruta.startsWith("/") ? ruta.substring(1) : ruta;

        String srcFile = "src/main/resources/static/" + cleanRuta;
        String targetFile = "target/classes/static/" + cleanRuta;

        try {
            Files.deleteIfExists(Paths.get(srcFile));
        } catch (IOException e) {
            System.err.println("[ArchivoService] Error al eliminar evidencia en src: " + e.getMessage());
        }

        try {
            Files.deleteIfExists(Paths.get(targetFile));
        } catch (IOException e) {
            System.err.println("[ArchivoService] Error al eliminar evidencia en target: " + e.getMessage());
        }
    }

    /**
     * Valida el tamaño, el tipo MIME y la extensión del archivo.
     */
    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío o no fue enviado.");
        }

        // 1. Validar tamaño (máximo 10MB)
        long maxBytes = 10 * 1024 * 1024; // 10MB
        if (archivo.getSize() > maxBytes) {
            throw new IllegalArgumentException("El tamaño del archivo excede el límite permitido de 10MB.");
        }

        // 2. Validar tipo MIME real
        String contentType = archivo.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("No se pudo determinar el tipo MIME del archivo.");
        }

        boolean isValidMime = contentType.equals("image/jpeg") ||
                              contentType.equals("image/png") ||
                              contentType.equals("image/jpg") ||
                              contentType.equals("video/mp4") ||
                              contentType.equals("video/quicktime") ||
                              contentType.equals("video/x-msvideo") ||
                              contentType.equals("video/webm");

        if (!isValidMime) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Solo se aceptan imágenes (JPG, JPEG, PNG) y videos (MP4, MOV, AVI, WEBM).");
        }

        // 3. Validar extensión
        String originalFilename = archivo.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("El nombre de archivo no es válido.");
        }
        
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        boolean isValidExt = ext.equals(".jpg") ||
                             ext.equals(".jpeg") ||
                             ext.equals(".png") ||
                             ext.equals(".mp4") ||
                             ext.equals(".mov") ||
                             ext.equals(".avi") ||
                             ext.equals(".webm");

        if (!isValidExt) {
            throw new IllegalArgumentException("La extensión del archivo no está permitida.");
        }
    }
}
