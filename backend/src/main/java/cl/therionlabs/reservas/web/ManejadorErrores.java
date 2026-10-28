package cl.therionlabs.reservas.web;

import cl.therionlabs.reservas.dto.Dtos.ErrorResponse;
import cl.therionlabs.reservas.dto.Dtos.ReservaResponse;
import cl.therionlabs.reservas.service.Excepciones.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/** Todas las respuestas de error tienen la forma {codigo, mensaje} para que la app las muestre tal cual. */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(TopeHorarioException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse tope(TopeHorarioException e) {
        return new ErrorResponse("TOPE_HORARIO", e.getMessage(), ReservaResponse.de(e.getReservaExistente()));
    }

    @ExceptionHandler(NoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse noEncontrado(NoEncontradoException e) {
        return new ErrorResponse("NO_ENCONTRADO", e.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse regla(ReglaNegocioException e) {
        return new ErrorResponse("REGLA_NEGOCIO", e.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse credenciales(CredencialesInvalidasException e) {
        return new ErrorResponse("CREDENCIALES", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ErrorResponse("VALIDACION", detalle);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse formato(Exception e) {
        return new ErrorResponse("FORMATO", "Solicitud con formato inválido");
    }
}
