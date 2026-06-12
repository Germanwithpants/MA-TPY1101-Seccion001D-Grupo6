package com.conectatarot.backend.repository;

import com.conectatarot.backend.entity.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena, Integer> {
    List<Resena> findByTarotistaId(Integer tarotistaId);

    @Query("SELECT AVG(r.calificacion) FROM Resena r WHERE r.tarotistaId = :tarotistaId")
    Double promedioByTarotistaId(Integer tarotistaId);
}
