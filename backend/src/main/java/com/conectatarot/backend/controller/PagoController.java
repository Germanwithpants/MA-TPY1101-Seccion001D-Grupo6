package com.conectatarot.backend.controller;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.common.IntegrationCommerceCodes;
import cl.transbank.common.IntegrationType;
import cl.transbank.webpay.common.WebpayOptions;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCommitResponse;
import com.conectatarot.backend.entity.Sesion;
import com.conectatarot.backend.repository.SesionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final SesionRepository sesionRepository;

    @Value("${app.frontend-url:https://ma-tpy1101-seccion001d-grupo6-production-cec7.up.railway.app}")
    private String backendUrl;

    public PagoController(SesionRepository sesionRepository) {
        this.sesionRepository = sesionRepository;
    }

    private WebpayPlus.Transaction getTransaction() {
	WebpayOptions options = new WebpayOptions(
	      IntegrationCommerceCodes.WEBPAY_PLUS,
	      IntegrationApiKeys.WEBPAY,
	      IntegrationType.TEST
    );
        return new WebpayPlus.Transaction(options);
    }

    @PostMapping("/iniciar/{sesionId}")
    public ResponseEntity<?> iniciarPago(@PathVariable Integer sesionId) {
        try {
            Sesion sesion = sesionRepository.findById(sesionId)
                    .orElseThrow(() -> new RuntimeException("Sesion no encontrada"));

            String buyOrder = "orden" + sesion.getId() + "_" + System.currentTimeMillis() % 100000;
            String sessionId = "sesion" + sesion.getId();
            double amount = sesion.getPrecioTotal().doubleValue();
            String returnUrl = backendUrl + "/api/pagos/confirmar";

            WebpayPlusTransactionCreateResponse response = getTransaction().create(buyOrder, sessionId, amount, returnUrl);

            sesion.setTokenWebpay(response.getToken());
            sesionRepository.save(sesion);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "url", response.getUrl(),
                    "token", response.getToken()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmarPagoPost(@RequestParam(required = false) String token_ws) {
        return procesarConfirmacion(token_ws);
    }

    @GetMapping("/confirmar")
    public ResponseEntity<?> confirmarPagoGet(@RequestParam(required = false) String token_ws) {
        return procesarConfirmacion(token_ws);
    }

    private ResponseEntity<?> procesarConfirmacion(String token_ws) {
        try {
            if (token_ws == null) {
                return ResponseEntity.ok("<html><body><h2>Pago cancelado o anulado</h2></body></html>");
            }

            WebpayPlusTransactionCommitResponse response = getTransaction().commit(token_ws);

            System.out.println("TOKEN RECIBIDO: " + token_ws);
            System.out.println("STATUS WEBPAY: " + response.getStatus());
	    System.out.println("RESPONSE COMPLETO: " + response.toString());

            Sesion sesion = sesionRepository.findAll().stream()
                    .filter(s -> token_ws.equals(s.getTokenWebpay()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Sesion no encontrada para este token"));

            if ("AUTHORIZED".equals(response.getStatus())) {
                sesion.setEstadoPago("PAGADO");
            } else {
                sesion.setEstadoPago("RECHAZADO");
            }
            sesionRepository.save(sesion);

            String html = "<html><body style='font-family:sans-serif;text-align:center;padding:50px;'>" +
                    "<h2>" + (sesion.getEstadoPago().equals("PAGADO") ? "✅ Pago exitoso" : "❌ Pago rechazado") + "</h2>" +
                    "<p>Puedes volver a la aplicación ConectaTarot.</p>" +
                    "</body></html>";

            return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("<html><body><h2>Error: " + e.getMessage() + "</h2></body></html>");
        }
    }

    @GetMapping("/estado/{sesionId}")
    public ResponseEntity<?> estadoPago(@PathVariable Integer sesionId) {
        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new RuntimeException("Sesion no encontrada"));
        return ResponseEntity.ok(Map.of("success", true, "estadoPago", sesion.getEstadoPago()));
    }

    @GetMapping("/mis-pagos")
    public ResponseEntity<?> misPagos() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Sesion> sesiones = sesionRepository
                .findByUsuario_EmailAndTokenWebpayIsNotNullOrderByFechaDesc(email);
        List<Map<String, Object>> resultado = sesiones.stream().map(s -> Map.<String, Object>of(
                "sesionId",    s.getId(),
                "monto",       s.getPrecioTotal(),
                "fecha",       s.getFecha().toString(),
                "estadoPago",  s.getEstadoPago(),
                "tarotista",   s.getTarotista().getNombreProfesional()
        )).toList();
        return ResponseEntity.ok(resultado);
    }
}
