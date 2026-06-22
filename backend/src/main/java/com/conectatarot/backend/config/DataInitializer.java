package com.conectatarot.backend.config;

import com.conectatarot.backend.entity.Rol;
import com.conectatarot.backend.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;

    @Override
    public void run(String... args) {
        for (String nombre : List.of("CLIENTE", "TAROTISTA", "ADMIN")) {
            try {
                if (rolRepository.findByNombreRol(nombre).isEmpty()) {
                    Rol rol = new Rol();
                    rol.setNombreRol(nombre);
                    rolRepository.save(rol);
                    System.out.println("DataInitializer: rol '" + nombre + "' creado.");
                }
            } catch (Exception e) {
                System.err.println("DataInitializer: no se pudo crear rol '" + nombre + "': " + e.getMessage());
            }
        }
    }
}
