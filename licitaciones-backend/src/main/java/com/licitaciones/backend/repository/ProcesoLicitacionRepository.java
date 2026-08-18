package com.licitaciones.backend.repository;

import com.licitaciones.backend.entity.ProcesoLicitacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcesoLicitacionRepository extends JpaRepository<ProcesoLicitacion, Long> {
}
