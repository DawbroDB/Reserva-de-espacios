package cl.therionlabs.reservas.web;

import cl.therionlabs.reservas.dto.Dtos.*;
import cl.therionlabs.reservas.model.EstadoReserva;
import cl.therionlabs.reservas.service.ReservaService;
import cl.therionlabs.reservas.service.SalaService;
import cl.therionlabs.reservas.service.SalaService.FiltroSalas;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ReservaController {

    private final ReservaService reservas;
    private final SalaService salas;

    public ReservaController(ReservaService reservas, SalaService salas) {
        this.reservas = reservas;
        this.salas = salas;
    }

    @GetMapping("/reservas")
    public List<ReservaResponse> listar(@RequestParam(required = false) LocalDate fecha,
                                        @RequestParam(required = false) Long salaId,
                                        @RequestParam(required = false) Long usuarioId,
                                        @RequestParam(required = false) LocalDate desde) {
        return reservas.buscar(fecha, salaId, usuarioId, desde).stream().map(ReservaResponse::de).toList();
    }

    @GetMapping("/reservas/{id}")
    public ReservaResponse obtener(@PathVariable Long id) {
        return ReservaResponse.de(reservas.obtener(id));
    }

    @PostMapping("/reservas")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@Valid @RequestBody ReservaRequest req) {
        return ReservaResponse.de(reservas.crear(req));
    }

    @PutMapping("/reservas/{id}")
    public ReservaResponse actualizar(@PathVariable Long id, @Valid @RequestBody ReservaRequest req) {
        return ReservaResponse.de(reservas.actualizar(id, req));
    }

    @DeleteMapping("/reservas/{id}")
    public ReservaResponse cancelar(@PathVariable Long id) {
        return ReservaResponse.de(reservas.cancelar(id));
    }

    @GetMapping("/reservas/conflictos")
    public List<ConflictoResponse> conflictos() {
        return reservas.conflictos();
    }

    /** Agenda del día: cada sala activa con sus reservas activas, lista para pintar la grilla de bloques. */
    @GetMapping("/agenda")
    public List<AgendaSalaResponse> agenda(@RequestParam LocalDate fecha) {
        Map<Long, List<ReservaResponse>> porSala = reservas.buscar(fecha, null, null, null).stream()
                .filter(r -> r.getEstado() == EstadoReserva.ACTIVA)
                .map(ReservaResponse::de)
                .collect(Collectors.groupingBy(ReservaResponse::salaId));
        return salas.buscar(new FiltroSalas(null, null, null, null, null, false)).stream()
                .map(s -> new AgendaSalaResponse(SalaResponse.de(s), porSala.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    @GetMapping("/ocupacion")
    public List<OcupacionResponse> ocupacion(@RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        return reservas.ocupacion(desde, hasta);
    }
}
