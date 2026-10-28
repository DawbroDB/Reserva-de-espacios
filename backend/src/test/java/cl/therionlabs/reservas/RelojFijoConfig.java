package cl.therionlabs.reservas;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** "Hoy" fijo en los tests: lunes 7 de enero de 2030, 08:00 en Santiago. */
@TestConfiguration
public class RelojFijoConfig {

    public static final ZoneId ZONA = ZoneId.of("America/Santiago");

    @Bean
    @Primary
    public Clock relojFijo() {
        return Clock.fixed(Instant.parse("2030-01-07T11:00:00Z"), ZONA);
    }
}
