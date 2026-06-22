package com.conectatarot.backend.service;

import com.conectatarot.backend.dto.LoginRequestDTO;
import com.conectatarot.backend.dto.LoginResponseDTO;
import com.conectatarot.backend.entity.Usuario;
import com.conectatarot.backend.repository.UsuarioRepository;
import com.conectatarot.backend.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponseDTO login(
            LoginRequestDTO request
    ) {

        Usuario usuario = usuarioRepository
                .findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new RuntimeException("Usuario no encontrado")
                );

        if(!passwordEncoder.matches(
                request.getPassword(),
                usuario.getPassword()
        )){
            throw new RuntimeException("Credenciales inválidas");
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new RuntimeException("Cuenta desactivada");
        }

        String token = jwtService.generateToken(
                usuario.getEmail(),
                usuario.getRol().getNombreRol()
        );

        return new LoginResponseDTO(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol().getNombreRol(),
                usuario.getActivo(),
                token
        );
    }

}