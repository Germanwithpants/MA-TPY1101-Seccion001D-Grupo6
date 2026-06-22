package com.conectatarot.backend.repository;

import com.conectatarot.backend.entity.ContenidoApp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContenidoAppRepository extends JpaRepository<ContenidoApp, Long> {
    Optional<ContenidoApp> findByClave(String clave);
}
