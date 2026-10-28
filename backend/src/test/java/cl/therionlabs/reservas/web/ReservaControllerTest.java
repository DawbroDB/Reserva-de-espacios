package cl.therionlabs.reservas.web;

import cl.therionlabs.reservas.RelojFijoConfig;
import cl.therionlabs.reservas.model.Rol;
import cl.therionlabs.reservas.model.Sala;
import cl.therionlabs.reservas.model.TipoSala;
import cl.therionlabs.reservas.model.Usuario;
import cl.therionlabs.reservas.repository.SalaRepository;
import cl.therionlabs.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelojFijoConfig.class)
@Transactional
class ReservaControllerTest {

    @Autowired MockMvc mvc;
    @Autowired SalaRepository salas;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;

    Sala sala;
    Usuario relator;

    @BeforeEach
    void preparar() {
        sala = salas.save(new Sala("API-1", "Sala API", TipoSala.LABORATORIO, 24, true, true, true));
        relator = usuarios.save(new Usuario("api@x.cl", "Relator API", Rol.RELATOR, encoder.encode("clave")));
    }

    private String cuerpo(String inicio, String fin) {
        return """
                {"salaId": %d, "usuarioId": %d, "fecha": "2030-01-08",
                 "horaInicio": "%s", "horaFin": "%s", "actividad": "Taller API"}
                """.formatted(sala.getId(), relator.getId(), inicio, fin);
    }

    @Test
    void segundaReservaSolapadaResponde409ConLaReservaQueChoca() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(cuerpo("09:00", "11:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.salaCodigo").value("API-1"));

        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(cuerpo("10:00", "12:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TOPE_HORARIO"))
                .andExpect(jsonPath("$.reservaEnConflicto.actividad").value("Taller API"));
    }

    @Test
    void salaOcupadaNoApareceEnDisponibles() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(cuerpo("09:00", "11:00")))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/salas/disponibles?fecha=2030-01-08&inicio=10:00&fin=10:30&computadores=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo == 'API-1')]").isEmpty());
        mvc.perform(get("/api/salas/disponibles?fecha=2030-01-08&inicio=11:00&fin=12:00&computadores=true"))
                .andExpect(jsonPath("$[?(@.codigo == 'API-1')]").isNotEmpty());
    }

    @Test
    void validacionDeCamposResponde400() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content("{\"actividad\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void loginCorrectoEIncorrecto() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"api@x.cl\", \"password\": \"clave\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("RELATOR"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"api@x.cl\", \"password\": \"mala\"}"))
                .andExpect(status().isUnauthorized());
    }
}
