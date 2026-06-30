package com.conectatarot.backend.controller;

import com.conectatarot.backend.entity.Resena;
import com.conectatarot.backend.repository.ResenaRepository;
import com.conectatarot.backend.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resenas")
public class ResenaController {

    private final ResenaRepository resenaRepository;
    private final UsuarioRepository usuarioRepository;

    public ResenaController(ResenaRepository resenaRepository, UsuarioRepository usuarioRepository) {
        this.resenaRepository = resenaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Integer sesionId = (Integer) body.get("sesionId");
            Integer usuarioId = (Integer) body.get("usuarioId");

            if (resenaRepository.existsBySesionIdAndUsuarioId(sesionId, usuarioId)) {
                return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "Ya calificaste esta sesión")
                );
            }

            Resena resena = new Resena();
            resena.setSesionId(sesionId);
            resena.setTarotistaId((Integer) body.get("tarotistaId"));
            resena.setUsuarioId(usuarioId);
            resena.setCalificacion((Integer) body.get("calificacion"));
            resena.setComentario((String) body.get("comentario"));
            resena.setTags(body.getOrDefault("tags", "") != null ? (String) body.get("tags") : "");
            resena.setFecha(LocalDateTime.now());
            resenaRepository.save(resena);
            return ResponseEntity.ok(Map.of("success", true, "message", "Reseña guardada"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/sesion/{sesionId}/existe")
    public ResponseEntity<?> existe(@PathVariable Integer sesionId, Authentication auth) {
        var usuario = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        if (usuario == null) return ResponseEntity.status(401).body(Map.of("existe", false));
        boolean existe = resenaRepository.existsBySesionIdAndUsuarioId(sesionId, usuario.getIdUsuario());
        return ResponseEntity.ok(Map.of("existe", existe));
    }

    @GetMapping("/tarotista/{id}")
    public ResponseEntity<?> porTarotista(@PathVariable Integer id) {
        List<Resena> resenas = resenaRepository.findByTarotistaId(id);
        Double promedio = resenaRepository.promedioByTarotistaId(id);
        List<Map<String, Object>> data = resenas.stream().map(r -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("calificacion", r.getCalificacion());
            m.put("comentario", r.getComentario() != null ? r.getComentario() : "");
            m.put("tags", r.getTags() != null ? r.getTags() : "");
            m.put("fecha", r.getFecha() != null ? r.getFecha().toString() : "");
            return m;
        }).toList();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", data,
            "promedio", promedio != null ? Math.round(promedio * 10.0) / 10.0 : 0.0,
            "total", resenas.size()
        ));
    }
}
