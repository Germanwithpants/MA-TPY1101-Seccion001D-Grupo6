package com.conectatarot.backend.service;

import com.conectatarot.backend.dto.SesionRequestDTO;
import com.conectatarot.backend.dto.SesionResponseDTO;
import com.conectatarot.backend.entity.*;
import com.conectatarot.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SesionService {

    private final UsuarioRepository usuarioRepository;
    private final TarotistaRepository tarotistaRepository;
    private final EspecialidadRepository especialidadRepository;
    private final TarotistaEspecialidadRepository tarotistaEspecialidadRepository;
    private final SesionRepository sesionRepository;
    private final DisponibilidadTarotistaRepository disponibilidadRepository;
    private final FcmService fcmService;

    public Sesion agendarSesion(SesionRequestDTO request) {

        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Tarotista tarotista = tarotistaRepository.findById(request.getTarotistaId())
                .orElseThrow(() -> new RuntimeException("Tarotista no encontrado"));

        Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadId())
                .orElseThrow(() -> new RuntimeException("Especialidad no encontrada"));

        if (!"APROBADO".equalsIgnoreCase(tarotista.getEstado())) {
            throw new RuntimeException("El tarotista no está aprobado");
        }

        boolean tieneEspecialidad =
                tarotistaEspecialidadRepository.existsByTarotista_IdAndEspecialidad_Id(
                        tarotista.getId(),
                        especialidad.getId()
                );

        if (!tieneEspecialidad) {
            throw new RuntimeException("El tarotista no tiene esta especialidad");
        }

        if (!estaDentroDeDisponibilidad(
                tarotista.getId(),
                request.getFecha(),
                request.getDuracionMinutos()
        )) {
            throw new RuntimeException("Horario fuera de disponibilidad del tarotista");
        }

        validarSolapamiento(
                tarotista.getId(),
                request.getFecha(),
                request.getDuracionMinutos()
        );

        BigDecimal precioTotal = tarotista.getPrecioBase()
                .multiply(BigDecimal.valueOf(request.getDuracionMinutos()))
                .divide(BigDecimal.valueOf(60));

        Sesion sesion = Sesion.builder()
                .usuario(usuario)
                .tarotista(tarotista)
                .especialidad(especialidad)
                .fecha(request.getFecha())
                .duracionMinutos(request.getDuracionMinutos())
                .precioTotal(precioTotal)
                .estado("CONFIRMADA")
                .build();

        return sesionRepository.save(sesion);
    }

    public List<SesionResponseDTO> obtenerMisSesiones(String email) {
        return sesionRepository.findByUsuario_Email(email)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    public List<SesionResponseDTO> obtenerSesionesTarotista(String email) {
        return sesionRepository.findByTarotista_Usuario_EmailOrderByFechaAsc(email)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    public Page<SesionResponseDTO> obtenerSesionesTarotistaPaginado(
            String email,
            String estado,
            Pageable pageable
    ) {
        Page<Sesion> sesiones;

        if (estado != null && !estado.isBlank()) {
            sesiones = sesionRepository
                    .findByTarotista_Usuario_EmailAndEstadoOrderByFechaAsc(
                            email,
                            estado.toUpperCase(),
                            pageable
                    );
        } else {
            sesiones = sesionRepository
                    .findByTarotista_Usuario_EmailOrderByFechaAsc(
                            email,
                            pageable
                    );
        }

        return sesiones.map(this::convertirADTO);
    }

    public SesionResponseDTO cancelarSesion(Integer sesionId, String email) {

        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        if (!sesion.getUsuario().getEmail().equals(email)) {
            throw new RuntimeException("No tienes permiso para cancelar esta sesión");
        }

        if ("CANCELADA".equalsIgnoreCase(sesion.getEstado())) {
            throw new RuntimeException("La sesión ya está cancelada");
        }

        sesion.setEstado("CANCELADA");
        SesionResponseDTO result = convertirADTO(sesionRepository.save(sesion));

        // Notify tarotista that session was cancelled
        String fcmTarotista = sesion.getTarotista().getUsuario().getFcmToken();
        String nombreCliente = sesion.getUsuario().getNombre();
        fcmService.sendNotification(fcmTarotista,
            "Sesión cancelada",
            nombreCliente + " canceló la sesión del " + sesion.getFecha().toLocalDate(),
            "CANCELADA");

        return result;
    }

    public SesionResponseDTO confirmarSesion(Integer sesionId, String emailTarotista) {

        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        validarTarotistaDueno(sesion, emailTarotista);

        if (!"PENDIENTE".equalsIgnoreCase(sesion.getEstado())) {
            throw new RuntimeException("Solo se pueden confirmar sesiones pendientes");
        }

        sesion.setEstado("CONFIRMADA");
        SesionResponseDTO resultConf = convertirADTO(sesionRepository.save(sesion));

        // Notify client that session was confirmed
        String fcmCliente = sesion.getUsuario().getFcmToken();
        fcmService.sendNotification(fcmCliente,
            "¡Sesión confirmada! ✅",
            sesion.getTarotista().getNombreProfesional() + " confirmó tu sesión del " + sesion.getFecha().toLocalDate(),
            "CONFIRMADA");

        return resultConf;
    }

    public SesionResponseDTO rechazarSesion(Integer sesionId, String emailTarotista) {

        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        validarTarotistaDueno(sesion, emailTarotista);

        if (!"PENDIENTE".equalsIgnoreCase(sesion.getEstado())) {
            throw new RuntimeException("Solo se pueden rechazar sesiones pendientes");
        }

        sesion.setEstado("RECHAZADA");
        SesionResponseDTO resultRech = convertirADTO(sesionRepository.save(sesion));

        // Notify client that session was rejected
        String fcmClienteRech = sesion.getUsuario().getFcmToken();
        fcmService.sendNotification(fcmClienteRech,
            "Sesión no disponible",
            sesion.getTarotista().getNombreProfesional() + " no puede atenderte el " + sesion.getFecha().toLocalDate() + ". Puedes agendar otro horario.",
            "RECHAZADA");

        return resultRech;
    }

    private boolean estaDentroDeDisponibilidad(
            Integer tarotistaId,
            LocalDateTime fecha,
            Integer duracionMinutos
    ) {
        List<DisponibilidadTarotista> disponibilidades =
                disponibilidadRepository.findByTarotistaIdAndActivaTrue(tarotistaId);

        if (disponibilidades.isEmpty()) {
            return false;
        }

        String dia = fecha.getDayOfWeek().toString();

        LocalTime inicio = fecha.toLocalTime();
        LocalTime fin = inicio.plusMinutes(duracionMinutos);

        if (fin.isBefore(inicio) || fin.equals(inicio)) {
            return false;
        }

        return disponibilidades.stream().anyMatch(d ->
                d.getDiaSemana().equalsIgnoreCase(dia)
                        && !inicio.isBefore(d.getHoraInicio())
                        && !fin.isAfter(d.getHoraFin())
        );
    }

    private void validarSolapamiento(
            Integer tarotistaId,
            LocalDateTime inicioNueva,
            Integer duracionNueva
    ) {
        LocalDateTime finNueva = inicioNueva.plusMinutes(duracionNueva);

        List<Sesion> sesiones = sesionRepository.findByTarotista_Id(tarotistaId);

        for (Sesion s : sesiones) {

            if ("CANCELADA".equalsIgnoreCase(s.getEstado())
                    || "RECHAZADA".equalsIgnoreCase(s.getEstado())) {
                continue;
            }

            LocalDateTime inicioExistente = s.getFecha();
            LocalDateTime finExistente = inicioExistente.plusMinutes(s.getDuracionMinutos());

            boolean solapa =
                    inicioNueva.isBefore(finExistente)
                            && finNueva.isAfter(inicioExistente);

            if (solapa) {
                throw new RuntimeException("El horario ya está ocupado");
            }
        }
    }

    private void validarTarotistaDueno(Sesion sesion, String email) {
        if (!sesion.getTarotista().getUsuario().getEmail().equals(email)) {
            throw new RuntimeException("No tienes permiso para modificar esta sesión");
        }
    }

    private SesionResponseDTO convertirADTO(Sesion sesion) {
        return SesionResponseDTO.builder()
                .id(sesion.getId())
                .nombreCliente(sesion.getUsuario().getNombre())
                .emailCliente(sesion.getUsuario().getEmail())
                .nombreTarotista(sesion.getTarotista().getNombreProfesional())
                .especialidad(sesion.getEspecialidad().getNombre())
                .fecha(sesion.getFecha())
                .duracionMinutos(sesion.getDuracionMinutos())
                .precioTotal(sesion.getPrecioTotal())
                .estado(sesion.getEstado())
		.estadoPago(sesion.getEstadoPago())
                .build();
    }
}
