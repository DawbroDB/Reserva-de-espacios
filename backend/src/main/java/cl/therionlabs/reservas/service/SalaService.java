package cl.therionlabs.reservas.service;

import cl.therionlabs.reservas.dto.Dtos.SalaRequest;
import cl.therionlabs.reservas.model.Sala;
import cl.therionlabs.reservas.model.TipoSala;
import cl.therionlabs.reservas.repository.ReservaRepository;
import cl.therionlabs.reservas.repository.SalaRepository;
import cl.therionlabs.reservas.service.Excepciones.NoEncontradoException;
import cl.therionlabs.reservas.service.Excepciones.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class SalaService {

    /** Criterios de búsqueda; los null no filtran. */
    public record FiltroSalas(Integer capacidadMin, Boolean proyector, Boolean computadores,
                              Boolean climatizacion, TipoSala tipo, boolean incluirInactivas) {

        boolean cumple(Sala s) {
            return (incluirInactivas || s.isActiva())
                    && (capacidadMin == null || s.getCapacidad() >= capacidadMin)
                    && (!Boolean.TRUE.equals(proyector) || s.isProyector())
                    && (!Boolean.TRUE.equals(computadores) || s.isComputadores())
                    && (!Boolean.TRUE.equals(climatizacion) || s.isClimatizacion())
                    && (tipo == null || s.getTipo() == tipo);
        }
    }

    private final SalaRepository salas;
    private final ReservaRepository reservas;

    public SalaService(SalaRepository salas, ReservaRepository reservas) {
        this.salas = salas;
        this.reservas = reservas;
    }

    @Transactional(readOnly = true)
    public List<Sala> buscar(FiltroSalas filtro) {
        // El catálogo de una sede es pequeño: filtrar en memoria es más simple que armar queries dinámicas.
        return salas.findAllByOrderByCodigoAsc().stream().filter(filtro::cumple).toList();
    }

    @Transactional(readOnly = true)
    public List<Sala> disponibles(LocalDate fecha, LocalTime inicio, LocalTime fin, FiltroSalas filtro) {
        if (!inicio.isBefore(fin)) {
            throw new ReglaNegocioException("La hora de inicio debe ser anterior a la de término");
        }
        return buscar(filtro).stream()
                .filter(s -> reservas.buscarSolapes(s.getId(), fecha, inicio, fin, null).isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public Sala obtener(Long id) {
        return salas.findById(id).orElseThrow(() -> new NoEncontradoException("Sala " + id + " no existe"));
    }

    @Transactional
    public Sala crear(SalaRequest req) {
        if (salas.existsByCodigoIgnoreCase(req.codigo().trim())) {
            throw new ReglaNegocioException("Ya existe una sala con código " + req.codigo());
        }
        Sala sala = new Sala(req.codigo().trim().toUpperCase(), req.nombre().trim(), req.tipo(),
                req.capacidad(), req.proyector(), req.computadores(), req.climatizacion());
        if (req.activa() != null) {
            sala.setActiva(req.activa());
        }
        return salas.save(sala);
    }

    @Transactional
    public Sala actualizar(Long id, SalaRequest req) {
        Sala sala = obtener(id);
        String codigo = req.codigo().trim().toUpperCase();
        if (!sala.getCodigo().equalsIgnoreCase(codigo) && salas.existsByCodigoIgnoreCase(codigo)) {
            throw new ReglaNegocioException("Ya existe una sala con código " + codigo);
        }
        sala.setCodigo(codigo);
        sala.setNombre(req.nombre().trim());
        sala.setTipo(req.tipo());
        sala.setCapacidad(req.capacidad());
        sala.setProyector(req.proyector());
        sala.setComputadores(req.computadores());
        sala.setClimatizacion(req.climatizacion());
        if (req.activa() != null) {
            sala.setActiva(req.activa());
        }
        return sala;
    }

    /** No se borra físicamente: se desactiva para no perder el historial de reservas. */
    @Transactional
    public void desactivar(Long id) {
        obtener(id).setActiva(false);
    }
}
