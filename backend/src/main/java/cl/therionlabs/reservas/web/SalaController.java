package cl.therionlabs.reservas.web;

import cl.therionlabs.reservas.dto.Dtos.SalaRequest;
import cl.therionlabs.reservas.dto.Dtos.SalaResponse;
import cl.therionlabs.reservas.model.TipoSala;
import cl.therionlabs.reservas.service.SalaService;
import cl.therionlabs.reservas.service.SalaService.FiltroSalas;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private final SalaService service;

    public SalaController(SalaService service) {
        this.service = service;
    }

    @GetMapping
    public List<SalaResponse> listar(@RequestParam(required = false) Integer capacidadMin,
                                     @RequestParam(required = false) Boolean proyector,
                                     @RequestParam(required = false) Boolean computadores,
                                     @RequestParam(required = false) Boolean climatizacion,
                                     @RequestParam(required = false) TipoSala tipo,
                                     @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        var filtro = new FiltroSalas(capacidadMin, proyector, computadores, climatizacion, tipo, incluirInactivas);
        return service.buscar(filtro).stream().map(SalaResponse::de).toList();
    }

    @GetMapping("/disponibles")
    public List<SalaResponse> disponibles(@RequestParam LocalDate fecha,
                                          @RequestParam LocalTime inicio,
                                          @RequestParam LocalTime fin,
                                          @RequestParam(required = false) Integer capacidadMin,
                                          @RequestParam(required = false) Boolean proyector,
                                          @RequestParam(required = false) Boolean computadores,
                                          @RequestParam(required = false) Boolean climatizacion,
                                          @RequestParam(required = false) TipoSala tipo) {
        var filtro = new FiltroSalas(capacidadMin, proyector, computadores, climatizacion, tipo, false);
        return service.disponibles(fecha, inicio, fin, filtro).stream().map(SalaResponse::de).toList();
    }

    @GetMapping("/{id}")
    public SalaResponse obtener(@PathVariable Long id) {
        return SalaResponse.de(service.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalaResponse crear(@Valid @RequestBody SalaRequest req) {
        return SalaResponse.de(service.crear(req));
    }

    @PutMapping("/{id}")
    public SalaResponse actualizar(@PathVariable Long id, @Valid @RequestBody SalaRequest req) {
        return SalaResponse.de(service.actualizar(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        service.desactivar(id);
    }
}
