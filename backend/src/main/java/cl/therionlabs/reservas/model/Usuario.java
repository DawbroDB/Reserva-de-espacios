package cl.therionlabs.reservas.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private String passwordHash;

    protected Usuario() {
    }

    public Usuario(String email, String nombre, Rol rol, String passwordHash) {
        this.email = email;
        this.nombre = nombre;
        this.rol = rol;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNombre() { return nombre; }
    public Rol getRol() { return rol; }
    public String getPasswordHash() { return passwordHash; }
}
