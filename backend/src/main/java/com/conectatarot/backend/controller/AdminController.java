package com.conectatarot.backend.controller;

import com.conectatarot.backend.dto.ApiResponse;
import com.conectatarot.backend.entity.*;
import com.conectatarot.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ResenaRepository resenaRepository;
    private final AuditLogRepository auditLogRepository;
    private final ContenidoAppRepository contenidoAppRepository;

    private static final BigDecimal COMISION_RATE = new BigDecimal("0.10");

    // ── Tarotistas ────────────────────────────────────────────────────────────

    @GetMapping("/tarotistas/pendientes")
    public ResponseEntity<ApiResponse<List<Tarotista>>> tarotistaPendientes() {
        return ResponseEntity.ok(ApiResponse.ok("Tarotistas pendientes obtenidos",
                tarotistaRepository.findByEstadoIgnoreCase("PENDIENTE")));
    }

    @PutMapping("/tarotistas/{id}/aprobar")
    public ResponseEntity<ApiResponse<Tarotista>> aprobarTarotista(@PathVariable Integer id,
                                                                    Authentication auth) {
        Tarotista tarotista = tarotistaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarotista no encontrado"));
        tarotista.setEstado("APROBADO");
        Tarotista saved = tarotistaRepository.save(tarotista);
        registrarAudit("APROBAR_TAROTISTA", "Tarotista", id.toString(),
                auth != null ? auth.getName() : "system",
                "Tarotista aprobado: " + tarotista.getNombreProfesional());
        return ResponseEntity.ok(ApiResponse.ok("Tarotista aprobado", saved));
    }

    @PutMapping("/tarotistas/{id}/rechazar")
    public ResponseEntity<ApiResponse<Tarotista>> rechazarTarotista(@PathVariable Integer id,
                                                                     Authentication auth) {
        Tarotista tarotista = tarotistaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarotista no encontrado"));
        tarotista.setEstado("RECHAZADO");
        Tarotista saved = tarotistaRepository.save(tarotista);
        registrarAudit("RECHAZAR_TAROTISTA", "Tarotista", id.toString(),
                auth != null ? auth.getName() : "system",
                "Tarotista rechazado: " + tarotista.getNombreProfesional());
        return ResponseEntity.ok(ApiResponse.ok("Tarotista rechazado", saved));
    }

    // ── Usuarios ──────────────────────────────────────────────────────────────

    @GetMapping("/usuarios")
    public ResponseEntity<ApiResponse<List<Usuario>>> listarUsuarios() {
        return ResponseEntity.ok(ApiResponse.ok("Usuarios obtenidos", usuarioRepository.findAll()));
    }

    @GetMapping("/usuarios/activos")
    public ResponseEntity<ApiResponse<Map<String, Object>>> usuariosActivos() {
        long activos = usuarioRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getActivo())).count();
        long total = usuarioRepository.count();
        return ResponseEntity.ok(ApiResponse.ok("Conteo de usuarios activos",
                Map.of("activos", activos, "total", total)));
    }

    @PutMapping("/usuarios/{id}/bloquear")
    public ResponseEntity<ApiResponse<String>> bloquearUsuario(@PathVariable Integer id,
                                                               Authentication auth) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (auth != null && usuario.getEmail().equals(auth.getName())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No puedes bloquearte a ti mismo"));
        }
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        registrarAudit("BLOQUEAR_USUARIO", "Usuario", id.toString(),
                auth != null ? auth.getName() : "system",
                "Usuario bloqueado: " + usuario.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Usuario bloqueado correctamente", null));
    }

    @PutMapping("/usuarios/{id}/desbloquear")
    public ResponseEntity<ApiResponse<String>> desbloquearUsuario(@PathVariable Integer id,
                                                                  Authentication auth) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        registrarAudit("DESBLOQUEAR_USUARIO", "Usuario", id.toString(),
                auth != null ? auth.getName() : "system",
                "Usuario desbloqueado: " + usuario.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Usuario desbloqueado correctamente", null));
    }

    // ── Reseñas ───────────────────────────────────────────────────────────────

    @DeleteMapping("/resenas/{id}")
    public ResponseEntity<ApiResponse<String>> eliminarResena(@PathVariable Integer id,
                                                              Authentication auth) {
        Resena resena = resenaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada"));
        resenaRepository.delete(resena);
        registrarAudit("ELIMINAR_RESENA", "Resena", id.toString(),
                auth != null ? auth.getName() : "system",
                "Reseña eliminada (tarotistaId=" + resena.getTarotistaId() + ")");
        return ResponseEntity.ok(ApiResponse.ok("Reseña eliminada y acción registrada", null));
    }

    // ── Pagos ─────────────────────────────────────────────────────────────────

    @GetMapping("/pagos")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listarPagos() {
        List<Sesion> pagadas = sesionRepository.findAll().stream()
                .filter(s -> s.getTokenWebpay() != null)
                .toList();
        List<Map<String, Object>> resultado = pagadas.stream().map(s -> Map.<String, Object>of(
                "sesionId",    s.getId(),
                "monto",       s.getPrecioTotal(),
                "fecha",       s.getFecha().toString(),
                "estadoPago",  s.getEstadoPago() != null ? s.getEstadoPago() : "PENDIENTE",
                "tarotista",   s.getTarotista().getNombreProfesional(),
                "cliente",     s.getUsuario().getNombre()
        )).toList();
        return ResponseEntity.ok(ApiResponse.ok("Pagos obtenidos", resultado));
    }

    // ── Comisiones ────────────────────────────────────────────────────────────

    @GetMapping("/comisiones")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listarComisiones(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {

        List<Sesion> pagadas;
        if (desde != null && hasta != null) {
            LocalDateTime d = LocalDate.parse(desde).atStartOfDay();
            LocalDateTime h = LocalDate.parse(hasta).atTime(23, 59, 59);
            pagadas = sesionRepository.findByEstadoPagoAndFechaBetweenOrderByFechaAsc("PAGADO", d, h);
        } else {
            pagadas = sesionRepository.findAll().stream()
                    .filter(s -> "PAGADO".equals(s.getEstadoPago()))
                    .toList();
        }

        List<Map<String, Object>> detalle = pagadas.stream().map(s -> {
            BigDecimal comision = s.getComisionAdmin() != null
                    ? s.getComisionAdmin()
                    : s.getPrecioTotal().multiply(COMISION_RATE).setScale(2, RoundingMode.HALF_UP);
            return Map.<String, Object>of(
                    "sesionId",  s.getId(),
                    "fecha",     s.getFecha().toString(),
                    "monto",     s.getPrecioTotal(),
                    "comision",  comision,
                    "tarotista", s.getTarotista().getNombreProfesional()
            );
        }).toList();

        BigDecimal totalComisiones = detalle.stream()
                .map(m -> (BigDecimal) m.get("comision"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(ApiResponse.ok("Comisiones obtenidas", Map.of(
                "totalComisiones", totalComisiones,
                "cantidad", detalle.size(),
                "detalle", detalle
        )));
    }

    // ── Estadísticas ──────────────────────────────────────────────────────────

    @GetMapping("/estadisticas")
    public ResponseEntity<ApiResponse<Map<String, Object>>> estadisticas() {
        long totalUsuarios    = usuarioRepository.count();
        long totalSesiones    = sesionRepository.count();
        long tarotistaActivos = tarotistaRepository.findByEstadoIgnoreCase("APROBADO").size();
        BigDecimal ingresoTotal = sesionRepository.sumIngresoTotal().orElse(BigDecimal.ZERO);
        long usuariosActivos  = usuarioRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getActivo())).count();

        return ResponseEntity.ok(ApiResponse.ok("Estadísticas obtenidas", Map.of(
                "totalUsuarios",    totalUsuarios,
                "usuariosActivos",  usuariosActivos,
                "totalSesiones",    totalSesiones,
                "tarotistaActivos", tarotistaActivos,
                "ingresoTotal",     ingresoTotal
        )));
    }

    // ── Reporte financiero ────────────────────────────────────────────────────

    @GetMapping("/reportes/financiero")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reporteFinanciero(
            @RequestParam String desde,
            @RequestParam String hasta) {

        LocalDateTime desdeDate = LocalDate.parse(desde).atStartOfDay();
        LocalDateTime hastaDate = LocalDate.parse(hasta).atTime(23, 59, 59);
        List<Sesion> pagos = sesionRepository
                .findByEstadoPagoAndFechaBetweenOrderByFechaAsc("PAGADO", desdeDate, hastaDate);

        BigDecimal total = pagos.stream()
                .map(Sesion::getPrecioTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal comisiones = total.multiply(COMISION_RATE).setScale(2, RoundingMode.HALF_UP);

        List<Map<String, Object>> detalle = pagos.stream().map(s -> Map.<String, Object>of(
                "sesionId",  s.getId(),
                "fecha",     s.getFecha().toString(),
                "monto",     s.getPrecioTotal(),
                "tarotista", s.getTarotista().getNombreProfesional(),
                "cliente",   s.getUsuario().getNombre()
        )).toList();

        return ResponseEntity.ok(ApiResponse.ok("Reporte financiero generado", Map.of(
                "desde",        desde,
                "hasta",        hasta,
                "totalPagos",   pagos.size(),
                "ingresoTotal", total,
                "comisiones",   comisiones,
                "pagos",        detalle
        )));
    }

    // ── Logs de auditoría ─────────────────────────────────────────────────────

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<AuditLog>>> obtenerLogs(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {

        List<AuditLog> logs;
        if (desde != null && hasta != null) {
            LocalDateTime d = LocalDate.parse(desde).atStartOfDay();
            LocalDateTime h = LocalDate.parse(hasta).atTime(23, 59, 59);
            logs = auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(d, h);
        } else {
            logs = auditLogRepository.findAllByOrderByTimestampDesc();
        }
        return ResponseEntity.ok(ApiResponse.ok("Logs obtenidos", logs));
    }

    // ── CMS Contenido ─────────────────────────────────────────────────────────

    @GetMapping("/contenido")
    public ResponseEntity<ApiResponse<List<ContenidoApp>>> listarContenido() {
        return ResponseEntity.ok(ApiResponse.ok("Contenido obtenido", contenidoAppRepository.findAll()));
    }

    @PutMapping("/contenido/{clave}")
    public ResponseEntity<ApiResponse<ContenidoApp>> actualizarContenido(
            @PathVariable String clave,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        ContenidoApp contenido = contenidoAppRepository.findByClave(clave)
                .orElseGet(() -> {
                    ContenidoApp nuevo = new ContenidoApp();
                    nuevo.setClave(clave);
                    nuevo.setDescripcion("Creado vía admin");
                    return nuevo;
                });

        contenido.setValor(body.get("valor"));
        contenido.setModificadoPor(auth != null ? auth.getName() : "system");
        ContenidoApp saved = contenidoAppRepository.save(contenido);

        registrarAudit("EDITAR_CONTENIDO", "ContenidoApp", clave,
                auth != null ? auth.getName() : "system",
                "Clave editada: " + clave);

        return ResponseEntity.ok(ApiResponse.ok("Contenido actualizado sin redeploy", saved));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void registrarAudit(String accion, String entidad, String entidadId,
                                 String adminEmail, String detalle) {
        try {
            AuditLog log = new AuditLog();
            log.setAccion(accion);
            log.setEntidad(entidad);
            log.setEntidadId(entidadId);
            log.setAdminEmail(adminEmail);
            log.setDetalle(detalle);
            auditLogRepository.save(log);
        } catch (Exception ignored) {}
    }
}
