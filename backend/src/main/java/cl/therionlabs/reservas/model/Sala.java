package cl.therionlabs.reservas.model;

import jakarta.persistence.*;

@Entity
@Table(name = "salas")
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSala tipo;

    @Column(nullable = false)
    private int capacidad;

    private boolean proyector;
    private boolean computadores;
    private boolean climatizacion;

    /** Una sala desactivada no admite reservas nuevas pero conserva su historial. */
    private boolean activa = true;

    protected Sala() {
    }

    public Sala(String codigo, String nombre, TipoSala tipo, int capacidad,
                boolean proyector, boolean computadores, boolean climatizacion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.capacidad = capacidad;
        this.proyector = proyector;
        this.computadores = computadores;
        this.climatizacion = climatizacion;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public TipoSala getTipo() { return tipo; }
    public void setTipo(TipoSala tipo) { this.tipo = tipo; }
    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }
    public boolean isProyector() { return proyector; }
    public void setProyector(boolean proyector) { this.proyector = proyector; }
    public boolean isComputadores() { return computadores; }
    public void setComputadores(boolean computadores) { this.computadores = computadores; }
    public boolean isClimatizacion() { return climatizacion; }
    public void setClimatizacion(boolean climatizacion) { this.climatizacion = climatizacion; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
