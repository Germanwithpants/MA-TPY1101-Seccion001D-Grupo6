package com.conectatarot.backend.controller;

import com.conectatarot.backend.dto.RegistroUsuarioRequest;
import com.conectatarot.backend.dto.UsuarioResponse;
import com.conectatarot.backend.entity.Usuario;
import com.conectatarot.backend.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<?> registrarUsuario(@RequestBody RegistroUsuarioRequest request) {
        try {

            // Crear entidad Usuario desde el DTO
            Usuario usuario = new Usuario();
            usuario.setNombre(request.getNombre());
            usuario.setEmail(request.getEmail());
            usuario.setPassword(request.getPassword());

            // Guardar usuario
            Usuario usuarioGuardado = usuarioService.registrarUsuario(usuario);

            // Crear response limpio (SIN PASSWORD)
            UsuarioResponse response = new UsuarioResponse();
            response.setIdUsuario(usuarioGuardado.getIdUsuario());
            response.setNombre(usuarioGuardado.getNombre());
            response.setEmail(usuarioGuardado.getEmail());
            response.setRol(usuarioGuardado.getRol().getNombreRol());
            response.setActivo(usuarioGuardado.getActivo());
            response.setFechaRegistro(
                    usuarioGuardado.getFechaRegistro() != null
                            ? usuarioGuardado.getFechaRegistro().toString()
                            : null
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Integer id, @RequestBody RegistroUsuarioRequest request) {
        try {
            Usuario usuario = usuarioService.actualizarUsuario(id, request.getNombre(), request.getEmail());
            UsuarioResponse response = new UsuarioResponse();
            response.setIdUsuario(usuario.getIdUsuario());
            response.setNombre(usuario.getNombre());
            response.setEmail(usuario.getEmail());
            response.setRol(usuario.getRol().getNombreRol());
            response.setActivo(usuario.getActivo());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Integer id) {
        try {
            usuarioService.desactivarUsuario(id);
            return ResponseEntity.ok(Map.of("mensaje", "Usuario desactivado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/fcm-token")
    public ResponseEntity<?> saveFcmToken(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        try {
            String fcmToken = body.get("fcmToken");
            if (fcmToken == null || fcmToken.isBlank()) {
                return ResponseEntity.badRequest().body("fcmToken requerido");
            }
            usuarioService.saveFcmToken(id, fcmToken);
            return ResponseEntity.ok(Map.of("mensaje", "Token FCM guardado"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
