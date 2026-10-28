package cl.therionlabs.reservas.service;

import cl.therionlabs.reservas.model.Usuario;
import cl.therionlabs.reservas.repository.UsuarioRepository;
import cl.therionlabs.reservas.service.Excepciones.CredencialesInvalidasException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    public Usuario login(String email, String password) {
        return usuarios.findByEmailIgnoreCase(email.trim())
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .orElseThrow(CredencialesInvalidasException::new);
    }
}
