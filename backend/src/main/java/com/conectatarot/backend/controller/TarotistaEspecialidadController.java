package com.conectatarot.backend.controller;

import com.conectatarot.backend.dto.ApiResponse;
import com.conectatarot.backend.dto.TarotistaEspecialidadResponseDTO;
import com.conectatarot.backend.entity.DisponibilidadTarotista;
import com.conectatarot.backend.entity.Tarotista;
import com.conectatarot.backend.repository.DisponibilidadTarotistaRepository;
import com.conectatarot.backend.repository.TarotistaRepository;
import com.conectatarot.backend.service.TarotistaEspecialidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/tarotistas")
@RequiredArgsConstructor
public class TarotistaEspecialidadController {

    private final TarotistaEspecialidadService service;
    private final DisponibilidadTarotistaRepository disponibilidadRepository;
    private final TarotistaRepository tarotistaRepository;

    @PostMapping("/{tarotistaId}/especialidades")
    public ResponseEntity<ApiResponse<TarotistaEspecialidadResponseDTO>> agregarEspecialidad(
            @PathVariable Integer tarotistaId,
            @RequestBody EspecialidadRequest request
    ) {
        TarotistaEspecialidadResponseDTO especialidad =
                service.agregarEspecialidad(tarotistaId, request.getEspecialidadId());

        return ResponseEntity.ok(
                ApiResponse.ok("Especialidad agregada correctamente", especialidad)
        );
    }

    @DeleteMapping("/{tarotistaId}/especialidades/{especialidadId}")
    public ResponseEntity<ApiResponse<String>> eliminarEspecialidad(
            @PathVariable Integer tarotistaId,
            @PathVariable Integer especialidadId
    ) {
        service.eliminarEspecialidad(tarotistaId, especialidadId);

        return ResponseEntity.ok(
                ApiResponse.ok("Especialidad eliminada correctamente", null)
        );
    }

    @GetMapping("/{tarotistaId}/especialidades")
    public ResponseEntity<ApiResponse<List<TarotistaEspecialidadResponseDTO>>> listarEspecialidades(
            @PathVariable Integer tarotistaId
    ) {
        List<TarotistaEspecialidadResponseDTO> especialidades =
                service.listarEspecialidades(tarotistaId);

        return ResponseEntity.ok(
                ApiResponse.ok("Especialidades obtenidas correctamente", especialidades)
        );
    }

    @PostMapping("/{tarotistaId}/disponibilidad")
    public ResponseEntity<ApiResponse<?>> agregarDisponibilidad(
            @PathVariable Integer tarotistaId,
            @RequestBody DisponibilidadRequest request
    ) {
        Tarotista tarotista = tarotistaRepository.findById(tarotistaId)
                .orElseThrow(() -> new RuntimeException("Tarotista no encontrado"));

        DisponibilidadTarotista slot = DisponibilidadTarotista.builder()
                .tarotista(tarotista)
                .diaSemana(request.getDiaSemana().toUpperCase())
                .horaInicio(LocalTime.parse(request.getHoraInicio()))
                .horaFin(LocalTime.parse(request.getHoraFin()))
                .activa(true)
                .build();

        return ResponseEntity.ok(
                ApiResponse.ok("Disponibilidad agregada correctamente", disponibilidadRepository.save(slot))
        );
    }

    @GetMapping("/{tarotistaId}/disponibilidad")
    public ResponseEntity<ApiResponse<List<DisponibilidadTarotista>>> listarDisponibilidad(
            @PathVariable Integer tarotistaId
    ) {
        List<DisponibilidadTarotista> slots =
                disponibilidadRepository.findByTarotistaIdAndActivaTrue(tarotistaId);

        return ResponseEntity.ok(
                ApiResponse.ok("Disponibilidad obtenida correctamente", slots)
        );
    }

    @DeleteMapping("/{tarotistaId}/disponibilidad/{disponibilidadId}")
    public ResponseEntity<ApiResponse<String>> eliminarDisponibilidad(
            @PathVariable Integer tarotistaId,
            @PathVariable Integer disponibilidadId
    ) {
        DisponibilidadTarotista slot = disponibilidadRepository.findById(disponibilidadId)
                .orElseThrow(() -> new RuntimeException("Disponibilidad no encontrada"));

        if (!slot.getTarotista().getId().equals(tarotistaId)) {
            throw new RuntimeException("Esta disponibilidad no pertenece al tarotista indicado");
        }

        slot.setActiva(false);
        disponibilidadRepository.save(slot);

        return ResponseEntity.ok(
                ApiResponse.ok("Disponibilidad eliminada correctamente", null)
        );
    }

    public static class EspecialidadRequest {
        private Integer especialidadId;

        public Integer getEspecialidadId() {
            return especialidadId;
        }

        public void setEspecialidadId(Integer especialidadId) {
            this.especialidadId = especialidadId;
        }
    }

    public static class DisponibilidadRequest {
        private String diaSemana;
        private String horaInicio;
        private String horaFin;

        public String getDiaSemana() { return diaSemana; }
        public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }
        public String getHoraInicio() { return horaInicio; }
        public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }
        public String getHoraFin() { return horaFin; }
        public void setHoraFin(String horaFin) { this.horaFin = horaFin; }
    }
}