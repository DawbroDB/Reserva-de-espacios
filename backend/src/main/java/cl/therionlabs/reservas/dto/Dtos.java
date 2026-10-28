package cl.therionlabs.reservas.dto;

import cl.therionlabs.reservas.model.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** DTOs de la API agrupados en un solo archivo: son records pequeños sin lógica. */
public final class Dtos {

    private Dtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record UsuarioResponse(Long id, String email, String nombre, Rol rol) {
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getEmail(), u.getNombre(), u.getRol());
        }
    }

    public record SalaRequest(
            @NotBlank @Size(max = 20) String codigo,
            @NotBlank @Size(max = 80) String nombre,
            @NotNull TipoSala tipo,
            @Min(1) @Max(500) int capacidad,
            boolean proyector,
            boolean computadores,
            boolean climatizacion,
            Boolean activa) {
    }

    public record SalaResponse(Long id, String codigo, String nombre, TipoSala tipo, int capacidad,
                               boolean proyector, boolean computadores, boolean climatizacion,
                               boolean activa) {
        public static SalaResponse de(Sala s) {
            return new SalaResponse(s.getId(), s.getCodigo(), s.getNombre(), s.getTipo(), s.getCapacidad(),
                    s.isProyector(), s.isComputadores(), s.isClimatizacion(), s.isActiva());
        }
    }

    public record ReservaRequest(
            @NotNull Long salaId,
            @NotNull Long usuarioId,
            @NotNull LocalDate fecha,
            @NotNull LocalTime horaInicio,
            @NotNull LocalTime horaFin,
            @NotBlank @Size(max = 120) String actividad) {
    }

    public record ReservaResponse(Long id, Long salaId, String salaCodigo, String salaNombre,
                                  Long usuarioId, String relator, LocalDate fecha,
                                  LocalTime horaInicio, LocalTime horaFin, String actividad,
                                  EstadoReserva estado) {
        public static ReservaResponse de(Reserva r) {
            return new ReservaResponse(r.getId(), r.getSala().getId(), r.getSala().getCodigo(),
                    r.getSala().getNombre(), r.getRelator().getId(), r.getRelator().getNombre(),
                    r.getFecha(), r.getHoraInicio(), r.getHoraFin(), r.getActividad(), r.getEstado());
        }
    }

    public record AgendaSalaResponse(SalaResponse sala, List<ReservaResponse> reservas) {
    }

    public record OcupacionResponse(Long salaId, String salaCodigo, String salaNombre,
                                    long minutosReservados, long minutosDisponibles,
                                    double porcentaje, int cantidadReservas) {
    }

    /** Inconsistencia detectada: dos reservas activas que se cruzan, o una reserva en sala desactivada. */
    public record ConflictoResponse(String tipo, String descripcion, ReservaResponse reserva,
                                    ReservaResponse reservaEnConflicto) {
    }

    public record ErrorResponse(String codigo, String mensaje, ReservaResponse reservaEnConflicto) {
        public ErrorResponse(String codigo, String mensaje) {
            this(codigo, mensaje, null);
        }
    }
}
