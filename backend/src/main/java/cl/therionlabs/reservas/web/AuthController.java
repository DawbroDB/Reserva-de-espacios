package cl.therionlabs.reservas.web;

import cl.therionlabs.reservas.dto.Dtos.LoginRequest;
import cl.therionlabs.reservas.dto.Dtos.UsuarioResponse;
import cl.therionlabs.reservas.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest req) {
        return UsuarioResponse.de(service.login(req.email(), req.password()));
    }
}
