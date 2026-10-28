package cl.therionlabs.reservas.repository;

import cl.therionlabs.reservas.model.EstadoReserva;
import cl.therionlabs.reservas.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Reservas activas de la sala que se cruzan con [inicio, fin), excluyendo la que se está editando. */
    @Query("""
            select r from Reserva r
            where r.sala.id = :salaId
              and r.fecha = :fecha
              and r.estado = cl.therionlabs.reservas.model.EstadoReserva.ACTIVA
              and r.horaInicio < :fin
              and r.horaFin > :inicio
              and (:excluirId is null or r.id <> :excluirId)
            """)
    List<Reserva> buscarSolapes(@Param("salaId") Long salaId,
                                @Param("fecha") LocalDate fecha,
                                @Param("inicio") LocalTime inicio,
                                @Param("fin") LocalTime fin,
                                @Param("excluirId") Long excluirId);

    @Query("""
            select r from Reserva r
            where (:fecha is null or r.fecha = :fecha)
              and (:salaId is null or r.sala.id = :salaId)
              and (:usuarioId is null or r.relator.id = :usuarioId)
              and (:desde is null or r.fecha >= :desde)
            order by r.fecha, r.horaInicio
            """)
    List<Reserva> buscar(@Param("fecha") LocalDate fecha,
                         @Param("salaId") Long salaId,
                         @Param("usuarioId") Long usuarioId,
                         @Param("desde") LocalDate desde);

    List<Reserva> findByEstadoAndFechaBetweenOrderBySalaIdAscFechaAscHoraInicioAsc(
            EstadoReserva estado, LocalDate desde, LocalDate hasta);

    List<Reserva> findByEstadoAndFechaGreaterThanEqualOrderBySalaIdAscFechaAscHoraInicioAsc(
            EstadoReserva estado, LocalDate desde);
}
