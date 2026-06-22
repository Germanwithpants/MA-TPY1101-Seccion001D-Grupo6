package com.conectatarot.backend.config;

import com.conectatarot.backend.entity.ContenidoApp;
import com.conectatarot.backend.entity.Rol;
import com.conectatarot.backend.repository.ContenidoAppRepository;
import com.conectatarot.backend.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final ContenidoAppRepository contenidoAppRepository;

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

        Map<String, String> contenidoDefault = Map.of(
            "app.titulo",       "ConectaTarot",
            "app.bienvenida",   "Conecta con tu tarotista de confianza",
            "app.descripcion",  "Plataforma de sesiones de tarot en línea",
            "app.contacto",     "soporte@conectatarot.com",
            "app.slogan",       "Tu destino, tu elección"
        );

        contenidoDefault.forEach((clave, valor) -> {
            try {
                if (contenidoAppRepository.findByClave(clave).isEmpty()) {
                    ContenidoApp c = new ContenidoApp();
                    c.setClave(clave);
                    c.setValor(valor);
                    c.setDescripcion("Contenido inicial del sistema");
                    c.setModificadoPor("system");
                    contenidoAppRepository.save(c);
                }
            } catch (Exception e) {
                System.err.println("DataInitializer: no se pudo crear contenido '" + clave + "': " + e.getMessage());
            }
        });
    }
}
