cat > ~/MA-TPY1101-Seccion001D-Grupo6/backend/src/main/java/com/conectatarot/backend/controller/GoogleAuthController.java << 'EOF'
package com.conectatarot.backend.controller;

import com.conectatarot.backend.entity.Usuario;
import com.conectatarot.backend.repository.UsuarioRepository;
import com.conectatarot.backend.repository.RolRepository;
import com.conectatarot.backend.security.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(() -> {
            Usuario nuevo = new Usuario();
