package com.conectatarot.backend.controller;

import com.conectatarot.backend.dto.ApiResponse;
import com.conectatarot.backend.entity.Sesion;
import com.conectatarot.backend.entity.Tarotista;
import com.conectatarot.backend.entity.Usuario;
import com.conectatarot.backend.repository.SesionRepository;
import com.conectatarot.backend.repository.TarotistaRepository;
import com.conectatarot.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TarotistaRepository tarotistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SesionRepository sesionRepository;

    @GetMapping("/tarotistas/pendientes")
    public ResponseEntity<ApiResponse<List<Tarotista>>> tarotistaPendientes() {
        List<Tarotista> pendientes = tarotistaRepository.findByEstadoIgnoreCase("PENDIENTE");
        return ResponseEntity.ok(ApiResponse.ok("Tarotistas pendientes obtenidos", pendientes));
    }

    @PutMapping("/tarotistas/{id}/aprobar")
    public ResponseEntity<ApiResponse<Tarotista>> aprobarTarotista(@PathVariable Integer id) {
        Tarotista tarotista = tarotistaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarotista no encontrado"));
        tarotista.setEstado("APROBADO");
        return ResponseEntity.ok(ApiResponse.ok("Tarotista aprobado", tarotistaRepository.save(tarotista)));
    }

    @PutMapping("/tarotistas/{id}/rechazar")
    public ResponseEntity<ApiResponse<Tarotista>> rechazarTarotista(@PathVariable Integer id) {
        Tarotista tarotista = tarotistaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarotista no encontrado"));
        tarotista.setEstado("RECHAZADO");
        return ResponseEntity.ok(ApiResponse.ok("Tarotista rechazado", tarotistaRepository.save(tarotista)));
    }

    @PutMapping("/usuarios/{id}/bloquear")
    public ResponseEntity<ApiResponse<String>> bloquearUsuario(@PathVariable Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(ApiResponse.ok("Usuario bloqueado correctamente", null));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<ApiResponse<List<Usuario>>> listarUsuarios() {
        return ResponseEntity.ok(ApiResponse.ok("Usuarios obtenidos", usuarioRepository.findAll()));
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<ApiResponse<Map<String, Object>>> estadisticas() {
        long totalUsuarios     = usuarioRepository.count();
        long totalSesiones     = sesionRepository.count();
        long tarotistaActivos  = tarotistaRepository.findByEstadoIgnoreCase("APROBADO").size();
        BigDecimal ingresoTotal = sesionRepository.sumIngresoTotal().orElse(BigDecimal.ZERO);

        Map<String, Object> stats = Map.of(
                "totalUsuarios",    totalUsuarios,
                "totalSesiones",    totalSesiones,
                "tarotistaActivos", tarotistaActivos,
                "ingresoTotal",     ingresoTotal
        );
        return ResponseEntity.ok(ApiResponse.ok("Estadísticas obtenidas", stats));
    }

    @GetMapping("/reportes/financiero")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reporteFinanciero(
            @RequestParam String desde,
            @RequestParam String hasta
    ) {
        LocalDateTime desdeDate = LocalDate.parse(desde).atStartOfDay();
        LocalDateTime hastaDate = LocalDate.parse(hasta).atTime(23, 59, 59);

        List<Sesion> pagos = sesionRepository
                .findByEstadoPagoAndFechaBetweenOrderByFechaAsc("PAGADO", desdeDate, hastaDate);

        BigDecimal total = pagos.stream()
                .map(Sesion::getPrecioTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> detalle = pagos.stream().map(s -> Map.<String, Object>of(
                "sesionId",  s.getId(),
                "fecha",     s.getFecha().toString(),
                "monto",     s.getPrecioTotal(),
                "tarotista", s.getTarotista().getNombreProfesional(),
                "cliente",   s.getUsuario().getNombre()
        )).toList();

        Map<String, Object> reporte = Map.of(
                "desde",       desde,
                "hasta",       hasta,
                "totalPagos",  pagos.size(),
                "ingresoTotal", total,
                "pagos",       detalle
        );
        return ResponseEntity.ok(ApiResponse.ok("Reporte financiero generado", reporte));
    }
}
