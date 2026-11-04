package com.duoc.cloudsumativa1.repository;

import com.duoc.cloudsumativa1.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Evento e where e.id = :id")
    Optional<Evento> buscarParaReserva(@Param("id") Long id);
}
