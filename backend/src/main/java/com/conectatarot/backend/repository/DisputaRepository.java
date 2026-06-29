package com.conectatarot.backend.repository;

import com.conectatarot.backend.entity.Disputa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputaRepository extends JpaRepository<Disputa, Long> {
    List<Disputa> findAllByOrderByFechaCreacionDesc();
    boolean existsBySesion_IdAndReportadoPor(Integer sesionId, String email);
}
