package cl.therionlabs.reservas.service;

import cl.therionlabs.reservas.RelojFijoConfig;
import cl.therionlabs.reservas.dto.Dtos.ReservaRequest;
import cl.therionlabs.reservas.model.*;
import cl.therionlabs.reservas.repository.ReservaRepository;
import cl.therionlabs.reservas.repository.SalaRepository;
import cl.therionlabs.reservas.repository.UsuarioRepository;
import cl.therionlabs.reservas.service.Excepciones.ReglaNegocioException;
import cl.therionlabs.reservas.service.Excepciones.TopeHorarioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(RelojFijoConfig.class)
@Transactional
class ReservaServiceTest {

    private static final LocalDate MANANA = LocalDate.of(2030, 1, 8);

    @Autowired ReservaService service;
    @Autowired SalaRepository salas;
    @Autowired UsuarioRepository usuarios;
    @Autowired ReservaRepository reservas;

    Sala sala;
    Sala otraSala;
    Usuario relator;

    @BeforeEach
    void preparar() {
        sala = salas.save(new Sala("T-1", "Sala Test 1", TipoSala.SALA, 20, true, false, false));
        otraSala = salas.save(new Sala("T-2", "Sala Test 2", TipoSala.SALA, 20, true, false, false));
        relator = usuarios.save(new Usuario("test@x.cl", "Relator Test", Rol.RELATOR, "x"));
        service.crear(req(sala, MANANA, "10:00", "12:00"));
    }

    private ReservaRequest req(Sala s, LocalDate fecha, String inicio, String fin) {
        return new ReservaRequest(s.getId(), relator.getId(), fecha, LocalTime.parse(inicio),
                LocalTime.parse(fin), "Curso de prueba");
    }

    @Test
    void bloquesContiguosNoSonTope() {
        assertDoesNotThrow(() -> service.crear(req(sala, MANANA, "08:00", "10:00")));
        assertDoesNotThrow(() -> service.crear(req(sala, MANANA, "12:00", "13:00")));
    }

    @Test
    void solapeParcialEsTope() {
        assertThrows(TopeHorarioException.class, () -> service.crear(req(sala, MANANA, "11:00", "13:00")));
        assertThrows(TopeHorarioException.class, () -> service.crear(req(sala, MANANA, "09:00", "10:30")));
    }

    @Test
    void reservaContenidaOQueEnvuelveEsTope() {
        assertThrows(TopeHorarioException.class, () -> service.crear(req(sala, MANANA, "10:30", "11:00")));
        assertThrows(TopeHorarioException.class, () -> service.crear(req(sala, MANANA, "09:00", "13:00")));
        assertThrows(TopeHorarioException.class, () -> service.crear(req(sala, MANANA, "10:00", "12:00")));
    }

    @Test
    void mismaHoraEnOtraSalaUOtroDiaEsValida() {
        assertDoesNotThrow(() -> service.crear(req(otraSala, MANANA, "10:00", "12:00")));
        assertDoesNotThrow(() -> service.crear(req(sala, MANANA.plusDays(1), "10:00", "12:00")));
    }

    @Test
    void reservaCanceladaLiberaElBloque() {
        Reserva r = reservas.buscarSolapes(sala.getId(), MANANA, LocalTime.of(10, 0), LocalTime.of(12, 0), null)
                .getFirst();
        service.cancelar(r.getId());
        assertDoesNotThrow(() -> service.crear(req(sala, MANANA, "10:00", "12:00")));
    }

    @Test
    void editarUnaReservaNoChocaConsigoMisma() {
        Reserva r = reservas.buscarSolapes(sala.getId(), MANANA, LocalTime.of(10, 0), LocalTime.of(12, 0), null)
                .getFirst();
        Reserva editada = service.actualizar(r.getId(), req(sala, MANANA, "10:30", "12:30"));
        assertEquals(LocalTime.of(12, 30), editada.getHoraFin());
    }

    @Test
    void validaHorarioYFechaPasada() {
        assertThrows(ReglaNegocioException.class, () -> service.crear(req(sala, MANANA, "12:00", "11:00")));
        assertThrows(ReglaNegocioException.class, () -> service.crear(req(sala, MANANA, "07:00", "09:00")));
        assertThrows(ReglaNegocioException.class, () -> service.crear(req(sala, MANANA, "21:00", "23:00")));
        assertThrows(ReglaNegocioException.class,
                () -> service.crear(req(sala, LocalDate.of(2030, 1, 6), "10:00", "11:00")));
    }

    @Test
    void salaInactivaNoAdmiteReservas() {
        otraSala.setActiva(false);
        assertThrows(ReglaNegocioException.class, () -> service.crear(req(otraSala, MANANA, "14:00", "15:00")));
    }

    @Test
    void conflictosDetectaTopesHeredados() {
        // Simula una carga histórica que se saltó la validación.
        reservas.save(new Reserva(sala, relator, MANANA, LocalTime.of(11, 0), LocalTime.of(11, 30), "Importada"));
        assertEquals(1, service.conflictos().stream().filter(c -> c.tipo().equals("TOPE_HORARIO")).count());
    }
}
