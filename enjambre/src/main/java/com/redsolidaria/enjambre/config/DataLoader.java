package com.redsolidaria.enjambre.config;

import com.redsolidaria.enjambre.model.Administrador;
import com.redsolidaria.enjambre.repository.AdministradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private AdministradorRepository administradorRepository;

    @Override
    public void run(String... args) throws Exception {
        
        // Verificar si ya existe un administrador
        if (!administradorRepository.existsByEmail("admin@redsolidaria.pe")) {
            
            // Crear administrador principal
            Administrador admin = new Administrador(
                "Administrador", 
                "Sistema", 
                "admin@redsolidaria.pe", 
                "admin123"
            );
            
            administradorRepository.save(admin);
            
            System.out.println("========================================");
            System.out.println("✅ CUENTA ADMIN CREADA AUTOMÁTICAMENTE:");
            System.out.println("   Email: admin@redsolidaria.pe");
            System.out.println("   Contraseña: admin123");
            System.out.println("   Rol: ADMIN");
            System.out.println("========================================");
        } else {
            System.out.println("ℹ️ La cuenta admin ya existe en la base de datos");
        }
        
        // Mostrar resumen de usuarios
        System.out.println("\n📊 RESÚMEN DE USUARIOS EN BD:");
        System.out.println("   Administradores: " + administradorRepository.count());
    }
}