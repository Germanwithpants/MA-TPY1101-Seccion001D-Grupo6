package com.conectatarot.backend.repository;

import com.conectatarot.backend.entity.Sesion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface SesionRepository extends JpaRepository<Sesion, Integer> {

    boolean existsByTarotista_IdAndFecha(
            Integer tarotistaId,
            LocalDateTime fecha
    );

    List<Sesion> findByTarotista_Id(Integer tarotistaId);

    List<Sesion> findByUsuario_Email(String email);

    List<Sesion> findByUsuario_EmailAndTokenWebpayIsNotNullOrderByFechaDesc(String email);

    List<Sesion> findByTarotista_Usuario_EmailOrderByFechaAsc(String email);

    Page<Sesion> findByTarotista_Usuario_EmailOrderByFechaAsc(
            String email,
            Pageable pageable
    );

    Page<Sesion> findByTarotista_Usuario_EmailAndEstadoOrderByFechaAsc(
            String email,
            String estado,
            Pageable pageable
    );

    List<Sesion> findByEstadoPagoAndFechaBetweenOrderByFechaAsc(
            String estadoPago, LocalDateTime desde, LocalDateTime hasta);

    @Query("SELECT SUM(s.precioTotal) FROM Sesion s WHERE s.estadoPago = 'PAGADO'")
    Optional<BigDecimal> sumIngresoTotal();
}