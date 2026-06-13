package com.conectatarot.backend.controller;

import com.conectatarot.backend.entity.Usuario;
import com.conectatarot.backend.repository.UsuarioRepository;
import com.conectatarot.backend.repository.RolRepository;
import com.conectatarot.backend.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class GoogleAuthController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public GoogleAuthController(UsuarioRepository usuarioRepository, RolRepository rolRepository, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.jwtService = jwtService;
    }

	@PostMapping("/google")
	public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
	    String email = body.get("email");
	    String nombre = body.get("nombre");

	    boolean[] esNuevo = {false};

	    Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(() -> {
	        esNuevo[0] = true;
	        Usuario nuevo = new Usuario();
	        nuevo.setNombre(nombre);
	        nuevo.setEmail(email);
	        nuevo.setPassword(encoder.encode("google_" + email));
	        nuevo.setRol(rolRepository.findByNombreRol("CLIENTE").orElseThrow());
	        nuevo.setActivo(true);
	        return usuarioRepository.save(nuevo);
	    });

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
}
