package com.conectatarot.backend.controller;

import com.conectatarot.backend.entity.Disputa;
import com.conectatarot.backend.entity.Sesion;
import com.conectatarot.backend.repository.DisputaRepository;
import com.conectatarot.backend.repository.SesionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DisputaController {

    private final DisputaRepository disputaRepository;
    private final SesionRepository sesionRepository;

    @PostMapping("/api/disputas")
    public ResponseEntity<?> reportar(@RequestBody Map<String, Object> body, Authentication auth) {
        String email = auth.getName();
        Integer sesionId = (Integer) body.get("sesionId");
        String tipo = (String) body.get("tipo");
        String descripcion = (String) body.get("descripcion");

        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        boolean esCliente = sesion.getUsuario().getEmail().equals(email);
        boolean esTarotista = sesion.getTarotista().getUsuario().getEmail().equals(email);
        if (!esCliente && !esTarotista) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "No autorizado"));
        }

        if (disputaRepository.existsBySesion_IdAndReportadoPor(sesionId, email)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Ya reportaste esta sesión"));
        }

        Disputa disputa = Disputa.builder()
                .sesion(sesion)
                .reportadoPor(email)
                .tipo(tipo)
                .descripcion(descripcion)
                .estado("PENDIENTE")
                .build();

        disputaRepository.save(disputa);
        return ResponseEntity.ok(Map.of("success", true, "message", "Reporte enviado al administrador"));
    }

    @GetMapping("/api/admin/disputas")
    public ResponseEntity<?> listar() {
        List<Disputa> disputas = disputaRepository.findAllByOrderByFechaCreacionDesc();
        List<Map<String, Object>> resultado = disputas.stream().map(d -> Map.<String, Object>of(
                "id", d.getId(),
                "sesionId", d.getSesion().getId(),
                "nombreCliente", d.getSesion().getUsuario().getNombre(),
                "nombreTarotista", d.getSesion().getTarotista().getNombreProfesional(),
                "tipo", d.getTipo(),
                "descripcion", d.getDescripcion() != null ? d.getDescripcion() : "",
                "estado", d.getEstado(),
                "reportadoPor", d.getReportadoPor(),
                "fechaCreacion", d.getFechaCreacion() != null ? d.getFechaCreacion().toString() : ""
        )).toList();
        return ResponseEntity.ok(Map.of("success", true, "data", resultado));
    }

    @PutMapping("/api/admin/disputas/{id}/resolver")
    public ResponseEntity<?> resolver(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Disputa disputa = disputaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Disputa no encontrada"));
        disputa.setEstado("RESUELTA");
        disputa.setResolucion(body.getOrDefault("resolucion", ""));
        disputaRepository.save(disputa);
        return ResponseEntity.ok(Map.of("success", true, "message", "Disputa resuelta"));
    }

    @PutMapping("/api/admin/disputas/{id}/en-revision")
    public ResponseEntity<?> enRevision(@PathVariable Long id) {
        Disputa disputa = disputaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Disputa no encontrada"));
        disputa.setEstado("EN_REVISION");
        disputaRepository.save(disputa);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
