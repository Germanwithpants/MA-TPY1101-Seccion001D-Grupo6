package com.conectatarot.backend.controller;

import com.conectatarot.backend.entity.Resena;
import com.conectatarot.backend.repository.ResenaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resenas")
public class ResenaController {

    private final ResenaRepository resenaRepository;

    public ResenaController(ResenaRepository resenaRepository) {
        this.resenaRepository = resenaRepository;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            Resena resena = new Resena();
            resena.setSesionId((Integer) body.get("sesionId"));
            resena.setTarotistaId((Integer) body.get("tarotistaId"));
            resena.setUsuarioId((Integer) body.get("usuarioId"));
            resena.setCalificacion((Integer) body.get("calificacion"));
            resena.setComentario((String) body.get("comentario"));
            resena.setFecha(LocalDateTime.now());
            resenaRepository.save(resena);
            return ResponseEntity.ok(Map.of("success", true, "message", "Reseña guardada"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/tarotista/{id}")
    public ResponseEntity<?> porTarotista(@PathVariable Integer id) {
        List<Resena> resenas = resenaRepository.findByTarotistaId(id);
        Double promedio = resenaRepository.promedioByTarotistaId(id);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", resenas,
            "promedio", promedio != null ? promedio : 0.0
        ));
    }
}
