package com.conectatarot.backend.service;

import com.conectatarot.backend.entity.Sesion;
import com.conectatarot.backend.repository.SesionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SesionSchedulerService {

    private final SesionRepository sesionRepository;

    @Scheduled(fixedDelay = 60000)
    public void marcarSesionesCompletadas() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Sesion> candidatas = sesionRepository.findByEstadoIn(List.of("CONFIRMADA", "PENDIENTE"));
        for (Sesion s : candidatas) {
            if (s.getFecha() != null && s.getDuracionMinutos() != null
                    && s.getFecha().plusMinutes(s.getDuracionMinutos()).isBefore(ahora)) {
                s.setEstado("COMPLETADA");
                sesionRepository.save(s);
            }
        }
    }
}
