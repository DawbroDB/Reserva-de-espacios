package cl.therionlabs.reservas.service;

import cl.therionlabs.reservas.dto.Dtos.ConflictoResponse;
import cl.therionlabs.reservas.dto.Dtos.OcupacionResponse;
import cl.therionlabs.reservas.dto.Dtos.ReservaRequest;
import cl.therionlabs.reservas.dto.Dtos.ReservaResponse;
import cl.therionlabs.reservas.model.EstadoReserva;
import cl.therionlabs.reservas.model.Reserva;
import cl.therionlabs.reservas.model.Sala;
import cl.therionlabs.reservas.model.Usuario;
import cl.therionlabs.reservas.repository.ReservaRepository;
import cl.therionlabs.reservas.repository.SalaRepository;
import cl.therionlabs.reservas.repository.UsuarioRepository;
import cl.therionlabs.reservas.service.Excepciones.NoEncontradoException;
import cl.therionlabs.reservas.service.Excepciones.ReglaNegocioException;
import cl.therionlabs.reservas.service.Excepciones.TopeHorarioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private final ReservaRepository reservas;
    private final SalaRepository salas;
    private final UsuarioRepository usuarios;
    private final Clock clock;
    private final LocalTime apertura;
    private final LocalTime cierre;

    public ReservaService(ReservaRepository reservas, SalaRepository salas, UsuarioRepository usuarios,
                          Clock clock,
                          @Value("${reservas.horario.apertura:08:00}") LocalTime apertura,
                          @Value("${reservas.horario.cierre:22:00}") LocalTime cierre) {
        this.reservas = reservas;
        this.salas = salas;
        this.usuarios = usuarios;
        this.clock = clock;
        this.apertura = apertura;
        this.cierre = cierre;
    }

    @Transactional(readOnly = true)
    public List<Reserva> buscar(LocalDate fecha, Long salaId, Long usuarioId, LocalDate desde) {
        return reservas.buscar(fecha, salaId, usuarioId, desde);
    }

    @Transactional(readOnly = true)
    public Reserva obtener(Long id) {
        return reservas.findById(id).orElseThrow(() -> new NoEncontradoException("Reserva " + id + " no existe"));
    }

    @Transactional
    public Reserva crear(ReservaRequest req) {
        Sala sala = salaBloqueada(req.salaId());
        Usuario relator = usuarios.findById(req.usuarioId())
                .orElseThrow(() -> new NoEncontradoException("Usuario " + req.usuarioId() + " no existe"));
        validar(sala, req, null);
        return reservas.save(new Reserva(sala, relator, req.fecha(), req.horaInicio(), req.horaFin(),
                req.actividad().trim()));
    }

    @Transactional
    public Reserva actualizar(Long id, ReservaRequest req) {
        Reserva reserva = obtener(id);
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new ReglaNegocioException("No se puede modificar una reserva cancelada");
        }
        Sala sala = salaBloqueada(req.salaId());
        validar(sala, req, id);
        reserva.setSala(sala);
        reserva.setFecha(req.fecha());
        reserva.setHoraInicio(req.horaInicio());
        reserva.setHoraFin(req.horaFin());
        reserva.setActividad(req.actividad().trim());
        return reserva;
    }

    @Transactional
    public Reserva cancelar(Long id) {
        Reserva reserva = obtener(id);
        reserva.setEstado(EstadoReserva.CANCELADA);
        return reserva;
    }

    private Sala salaBloqueada(Long salaId) {
        return salas.findByIdParaReservar(salaId)
                .orElseThrow(() -> new NoEncontradoException("Sala " + salaId + " no existe"));
    }

    /** Reglas de negocio de una reserva. La sala ya viene bloqueada, así que el chequeo de solapes es atómico. */
    private void validar(Sala sala, ReservaRequest req, Long excluirId) {
        if (!sala.isActiva()) {
            throw new ReglaNegocioException("La sala " + sala.getCodigo() + " está fuera de servicio");
        }
        if (!req.horaInicio().isBefore(req.horaFin())) {
            throw new ReglaNegocioException("La hora de inicio debe ser anterior a la de término");
        }
        if (req.horaInicio().isBefore(apertura) || req.horaFin().isAfter(cierre)) {
            throw new ReglaNegocioException("El horario debe estar entre " + apertura + " y " + cierre);
        }
        if (LocalDateTime.of(req.fecha(), req.horaInicio()).isBefore(LocalDateTime.now(clock))) {
            throw new ReglaNegocioException("No se puede reservar en una fecha u hora pasada");
        }
        List<Reserva> solapes = reservas.buscarSolapes(sala.getId(), req.fecha(), req.horaInicio(),
                req.horaFin(), excluirId);
        if (!solapes.isEmpty()) {
            throw new TopeHorarioException(solapes.getFirst());
        }
    }

    @Transactional(readOnly = true)
    public List<OcupacionResponse> ocupacion(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new ReglaNegocioException("El rango de fechas es inválido");
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        long minutosPorSala = dias * Duration.between(apertura, cierre).toMinutes();
        Map<Long, List<Reserva>> porSala = reservas
                .findByEstadoAndFechaBetweenOrderBySalaIdAscFechaAscHoraInicioAsc(EstadoReserva.ACTIVA, desde, hasta)
                .stream().collect(Collectors.groupingBy(r -> r.getSala().getId()));

        return salas.findAllByOrderByCodigoAsc().stream()
                .filter(Sala::isActiva)
                .map(s -> {
                    List<Reserva> rs = porSala.getOrDefault(s.getId(), List.of());
                    long minutos = rs.stream()
                            .mapToLong(r -> Duration.between(r.getHoraInicio(), r.getHoraFin()).toMinutes())
                            .sum();
                    double pct = minutosPorSala == 0 ? 0 : Math.round(minutos * 1000.0 / minutosPorSala) / 10.0;
                    return new OcupacionResponse(s.getId(), s.getCodigo(), s.getNombre(), minutos,
                            minutosPorSala, pct, rs.size());
                })
                .toList();
    }

    /**
     * Inconsistencias que el coordinador debe resolver. La API impide crear topes, pero pueden
     * existir por cargas de datos históricas o por salas desactivadas con reservas futuras.
     */
    @Transactional(readOnly = true)
    public List<ConflictoResponse> conflictos() {
        LocalDate hoy = LocalDate.now(clock);
        List<Reserva> activas = reservas
                .findByEstadoAndFechaGreaterThanEqualOrderBySalaIdAscFechaAscHoraInicioAsc(EstadoReserva.ACTIVA, hoy);
        List<ConflictoResponse> resultado = new ArrayList<>();

        Map<String, List<Reserva>> porSalaYFecha = activas.stream()
                .collect(Collectors.groupingBy(r -> r.getSala().getId() + "|" + r.getFecha()));
        for (List<Reserva> grupo : porSalaYFecha.values()) {
            for (int i = 0; i < grupo.size(); i++) {
                for (int j = i + 1; j < grupo.size(); j++) {
                    Reserva a = grupo.get(i);
                    Reserva b = grupo.get(j);
                    if (a.seSolapaCon(b.getHoraInicio(), b.getHoraFin())) {
                        resultado.add(new ConflictoResponse("TOPE_HORARIO",
                                "Dos reservas activas se cruzan en " + a.getSala().getCodigo() + " el " + a.getFecha(),
                                ReservaResponse.de(a), ReservaResponse.de(b)));
                    }
                }
            }
        }
        activas.stream()
                .filter(r -> !r.getSala().isActiva())
                .forEach(r -> resultado.add(new ConflictoResponse("SALA_INACTIVA",
                        "La sala " + r.getSala().getCodigo() + " está fuera de servicio y tiene reservas futuras",
                        ReservaResponse.de(r), null)));
        return resultado;
    }
}
