package cl.therionlabs.reservas.repository;

import cl.therionlabs.reservas.model.Sala;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    List<Sala> findAllByOrderByCodigoAsc();

    boolean existsByCodigoIgnoreCase(String codigo);

    /**
     * SELECT ... FOR UPDATE sobre la sala: dos reservas simultáneas para la misma sala
     * quedan en fila, así la segunda ya ve a la primera al revisar solapes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sala s where s.id = :id")
    Optional<Sala> findByIdParaReservar(@Param("id") Long id);
}
