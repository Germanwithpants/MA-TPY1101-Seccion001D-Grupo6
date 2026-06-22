package com.conectatarot.backend.controller;

import com.conectatarot.backend.entity.Usuario;
import com.conectatarot.backend.repository.UsuarioRepository;
import com.conectatarot.backend.repository.RolRepository;
import com.conectatarot.backend.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.google.client-id}")
    private String googleClientId;

    public GoogleAuthController(UsuarioRepository usuarioRepository, RolRepository rolRepository, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {

        String idTokenString = body.get("idToken");
        if (idTokenString == null || idTokenString.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Falta idToken"));
        }

        GoogleIdToken.Payload payload;
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idTokenString);

            if (googleIdToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Token de Google invalido"));
            }

            payload = googleIdToken.getPayload();

        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No se pudo verificar el token de Google"));
        }

        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "El email de Google no esta verificado"));
        }

        String email = payload.getEmail();
        Object nameClaim = payload.get("name");
        String nombre = (nameClaim != null) ? nameClaim.toString() : email;

        boolean[] esNuevo = {false};
        final String nombreFinal = nombre;

        Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(() -> {
            esNuevo[0] = true;
            Usuario nuevo = new Usuario();
            nuevo.setNombre(nombreFinal);
            nuevo.setEmail(email);
            nuevo.setPassword(encoder.encode(generarPasswordAleatorio()));
            nuevo.setRol(rolRepository.findByNombreRol("CLIENTE").orElseThrow());
            nuevo.setActivo(true);
            return usuarioRepository.save(nuevo);
        });

        if (!esNuevo[0] && !Boolean.TRUE.equals(usuario.getActivo())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Cuenta desactivada"));
        }

        String token = jwtService.generateToken(usuario.getEmail(), usuario.getRol().getNombreRol());

        return ResponseEntity.ok(Map.of(
                "idUsuario", usuario.getIdUsuario(),
                "nombre", usuario.getNombre(),
                "email", usuario.getEmail(),
                "rol", usuario.getRol().getNombreRol(),
                "activo", usuario.getActivo(),
                "token", token,
                "esNuevo", esNuevo[0]
        ));
    }

    private String generarPasswordAleatorio() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
