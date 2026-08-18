package com.licitaciones.backend.repository;

import com.licitaciones.backend.entity.ResultadoAnalisis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResultadoAnalisisRepository extends JpaRepository<ResultadoAnalisis, Long> {

    List<ResultadoAnalisis> findByProcesoIdOrderByFechaCreacionDesc(Long procesoId);

    Optional<ResultadoAnalisis> findTopByProcesoIdOrderByFechaCreacionDesc(Long procesoId);
}
