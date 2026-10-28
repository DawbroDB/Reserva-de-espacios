package cl.therionlabs.reservas.service;

import cl.therionlabs.reservas.model.Reserva;

public final class Excepciones {

    private Excepciones() {
    }

    public static class NoEncontradoException extends RuntimeException {
        public NoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    /** Violación de una regla de negocio (horario fuera de rango, fecha pasada, etc.) → 400. */
    public static class ReglaNegocioException extends RuntimeException {
        public ReglaNegocioException(String mensaje) {
            super(mensaje);
        }
    }

    /** Tope de horario con otra reserva activa → 409. */
    public static class TopeHorarioException extends RuntimeException {
        private final Reserva reservaExistente;

        public TopeHorarioException(Reserva reservaExistente) {
            super("La sala %s ya está reservada de %s a %s para \"%s\" (%s)".formatted(
                    reservaExistente.getSala().getCodigo(),
                    reservaExistente.getHoraInicio(),
                    reservaExistente.getHoraFin(),
                    reservaExistente.getActividad(),
                    reservaExistente.getRelator().getNombre()));
            this.reservaExistente = reservaExistente;
        }

        public Reserva getReservaExistente() {
            return reservaExistente;
        }
    }

    public static class CredencialesInvalidasException extends RuntimeException {
        public CredencialesInvalidasException() {
            super("Correo o contraseña incorrectos");
        }
    }
}
