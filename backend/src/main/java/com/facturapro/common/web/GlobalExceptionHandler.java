package com.facturapro.common.web;

import com.facturapro.common.error.BusinessException;
import com.facturapro.common.error.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> business(BusinessException ex, HttpServletRequest req) {
        HttpStatus status = "CONFLICT".equals(ex.getCode()) ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return build(status, ex.getCode(), ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Los datos enviados no son validos.", req, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> constraint(ConstraintViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), req, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> missingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Parametro invalido: " + ex.getName(), req, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "El cuerpo de la solicitud tiene un formato invalido.", req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violacion de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "CONFLICT",
                "La operacion entra en conflicto con datos existentes (posible duplicado o referencia invalida).", req, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> maxUpload(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "El archivo supera el tamanio maximo permitido.", req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "No tiene permisos para realizar esta operacion.", req, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> authentication(AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Debe iniciar sesion para acceder a este recurso.", req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> noResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Recurso no encontrado.", req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> methodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Metodo HTTP no soportado.", req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrio un error inesperado. Intente nuevamente o contacte al administrador.", req, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           HttpServletRequest req, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(java.time.Instant.now(), status.value(), status.getReasonPhrase(),
                code, message, req.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
