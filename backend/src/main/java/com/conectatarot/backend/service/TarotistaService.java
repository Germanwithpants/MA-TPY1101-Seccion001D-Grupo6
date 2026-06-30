package com.conectatarot.backend.service;

import com.conectatarot.backend.dto.TarotistaResponseDTO;
import com.conectatarot.backend.entity.Tarotista;
import com.conectatarot.backend.entity.Usuario;
import com.conectatarot.backend.repository.ResenaRepository;
import com.conectatarot.backend.repository.TarotistaRepository;
import com.conectatarot.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TarotistaService {

    private final TarotistaRepository tarotistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ResenaRepository resenaRepository;

    public Tarotista crearTarotista(Integer usuarioId, String nombreProfesional) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean yaExiste = tarotistaRepository.existsByUsuario_IdUsuario(usuarioId);
        if (yaExiste) {
            throw new RuntimeException("El usuario ya tiene un perfil de tarotista");
        }

        Tarotista tarotista = Tarotista.builder()
                .usuario(usuario)
                .nombreProfesional(nombreProfesional)
                .estado("PENDIENTE")
                .build();

        return tarotistaRepository.save(tarotista);
    }

    public Tarotista actualizarPerfil(
            Integer tarotistaId,
            String emailUsuarioLogueado,
            String descripcion,
            BigDecimal precioBase
    ) {
        Tarotista tarotista = tarotistaRepository.findById(tarotistaId)
                .orElseThrow(() -> new RuntimeException("Tarotista no encontrado"));

        if (!tarotista.getUsuario().getEmail().equals(emailUsuarioLogueado)) {
            throw new RuntimeException("No tienes permiso para editar este perfil");
        }

        if (descripcion == null || descripcion.length() < 20) {
            throw new RuntimeException("La descripción debe tener al menos 20 caracteres");
        }

        if (precioBase == null || precioBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El precio base debe ser mayor a 0");
        }

        tarotista.setDescripcion(descripcion);
        tarotista.setPrecioBase(precioBase);

        return tarotistaRepository.save(tarotista);
    }

    public List<TarotistaResponseDTO> buscarTarotistas(String especialidad) {

        List<Tarotista> tarotistas;

        if (especialidad != null && !especialidad.isBlank()) {
            tarotistas = tarotistaRepository
                    .findByEstadoIgnoreCaseAndTarotistaEspecialidades_Especialidad_NombreContainingIgnoreCase(
                            "APROBADO",
                            especialidad
                    );
        } else {
            tarotistas = tarotistaRepository.findByEstadoIgnoreCase("APROBADO");
        }

        return tarotistas.stream()
                .map(this::convertirADTO)
                .toList();
    }

    private TarotistaResponseDTO convertirADTO(Tarotista tarotista) {
        Double promedio = resenaRepository.promedioByTarotistaId(tarotista.getId());
        long total = resenaRepository.findByTarotistaId(tarotista.getId()).size();
        return TarotistaResponseDTO.builder()
                .id(tarotista.getId())
                .nombreProfesional(tarotista.getNombreProfesional())
                .descripcion(tarotista.getDescripcion())
                .precioBase(tarotista.getPrecioBase())
                .estado(tarotista.getEstado())
                .especialidades(
                        tarotista.getTarotistaEspecialidades()
                                .stream()
                                .map(relacion -> relacion.getEspecialidad().getNombre())
                                .toList()
                )
                .promedio(promedio != null ? Math.round(promedio * 10.0) / 10.0 : null)
                .totalResenas((int) total)
                .build();
    }
}