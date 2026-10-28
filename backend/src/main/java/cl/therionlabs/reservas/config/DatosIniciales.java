package cl.therionlabs.reservas.config;

import cl.therionlabs.reservas.model.*;
import cl.therionlabs.reservas.repository.ReservaRepository;
import cl.therionlabs.reservas.repository.SalaRepository;
import cl.therionlabs.reservas.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Puebla H2 con datos 100% ficticios (Ley 19.628 / 21.719): catálogo de salas, usuarios demo y
 * una semana de reservas a partir de hoy. Incluye a propósito un tope y una reserva en sala
 * desactivada para que el coordinador tenga inconsistencias que resolver.
 */
@Component
@Profile("!test")
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);
    public static final String PASSWORD_DEMO = "demo1234";

    private static final String[] ACTIVIDADES = {
            "Excel Avanzado", "Python para Análisis de Datos", "Seguridad Industrial",
            "Liderazgo de Equipos", "Redes CCNA Módulo 1", "Primeros Auxilios",
            "Power BI para Gestión", "Metodologías Ágiles"
    };
    private static final int[][] BLOQUES = {{9, 11}, {11, 13}, {14, 16}, {16, 18}, {18, 20}};

    private final SalaRepository salas;
    private final UsuarioRepository usuarios;
    private final ReservaRepository reservas;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public DatosIniciales(SalaRepository salas, UsuarioRepository usuarios, ReservaRepository reservas,
                          PasswordEncoder encoder, Clock clock) {
        this.salas = salas;
        this.usuarios = usuarios;
        this.reservas = reservas;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<Sala> catalogo = salas.saveAll(List.of(
                new Sala("SC-101", "Sala de Clases 101", TipoSala.SALA, 30, true, false, true),
                new Sala("SC-102", "Sala de Clases 102", TipoSala.SALA, 25, true, false, false),
                new Sala("SC-103", "Sala de Reuniones 103", TipoSala.SALA, 12, true, false, true),
                new Sala("SC-201", "Sala de Clases 201", TipoSala.SALA, 40, true, false, true),
                new Sala("LAB-01", "Laboratorio de Computación 1", TipoSala.LABORATORIO, 24, true, true, true),
                new Sala("LAB-02", "Laboratorio de Redes", TipoSala.LABORATORIO, 20, true, true, false),
                new Sala("LAB-03", "Laboratorio de Electrónica", TipoSala.LABORATORIO, 16, false, true, true),
                new Sala("MP-01", "Auditorio Multipropósito", TipoSala.MULTIPROPOSITO, 80, true, false, true),
                new Sala("MP-02", "Sala Multipropósito 2", TipoSala.MULTIPROPOSITO, 35, false, false, false),
                new Sala("LAB-04", "Laboratorio de Automatización", TipoSala.LABORATORIO, 18, true, true, true)
        ));

        String hash = encoder.encode(PASSWORD_DEMO);
        usuarios.saveAll(List.of(
                new Usuario("coordinador@therionlabs.cl", "Coordinación Infraestructura", Rol.COORDINADOR, hash),
                new Usuario("coordinador2@therionlabs.cl", "Coordinación Sede Norte", Rol.COORDINADOR, hash)
        ));
        List<Usuario> relatores = usuarios.saveAll(List.of(
                new Usuario("relator1@therionlabs.cl", "Relator Demo 1", Rol.RELATOR, hash),
                new Usuario("relator2@therionlabs.cl", "Relator Demo 2", Rol.RELATOR, hash),
                new Usuario("relator3@therionlabs.cl", "Relator Demo 3", Rol.RELATOR, hash),
                new Usuario("relator4@therionlabs.cl", "Relator Demo 4", Rol.RELATOR, hash),
                new Usuario("relator5@therionlabs.cl", "Relator Demo 5", Rol.RELATOR, hash),
                new Usuario("relator6@therionlabs.cl", "Relator Demo 6", Rol.RELATOR, hash)
        ));

        LocalDate hoy = LocalDate.now(clock);
        int total = 0;
        for (int d = 0; d < 7; d++) {
            LocalDate fecha = hoy.plusDays(d);
            if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            for (int s = 0; s < catalogo.size(); s++) {
                for (int b = 0; b < BLOQUES.length; b++) {
                    // Patrón determinista que deja ~1/3 de los bloques ocupados.
                    if ((d * 7 + s * 3 + b) % 3 != 0) {
                        continue;
                    }
                    Usuario relator = relatores.get((s + b + d) % relatores.size());
                    String actividad = ACTIVIDADES[(s + b + d) % ACTIVIDADES.length];
                    reservas.save(new Reserva(catalogo.get(s), relator, fecha,
                            LocalTime.of(BLOQUES[b][0], 0), LocalTime.of(BLOQUES[b][1], 0), actividad));
                    total++;
                }
            }
        }

        // Inconsistencias heredadas de las "planillas locales" que el coordinador debe resolver.
        LocalDate manana = hoy.plusDays(1);
        Sala sc101 = catalogo.getFirst();
        reservas.save(new Reserva(sc101, relatores.get(0), manana, LocalTime.of(10, 0), LocalTime.of(11, 30),
                "Taller importado de planilla"));
        reservas.save(new Reserva(sc101, relatores.get(1), manana, LocalTime.of(11, 0), LocalTime.of(12, 0),
                "Charla importada de planilla"));
        Sala lab04 = catalogo.getLast();
        lab04.setActiva(false);
        total += 2;

        log.info("Datos ficticios cargados: {} salas, {} usuarios, {} reservas (password demo: {})",
                catalogo.size(), usuarios.count(), total, PASSWORD_DEMO);
    }
}
