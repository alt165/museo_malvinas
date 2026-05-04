package com.proveedores.exception;

import com.proveedores.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        log.warn("event=exception.resource_not_found status={} path={} exception={}", HttpStatus.NOT_FOUND.value(), request.getRequestURI(), exception.getClass().getSimpleName());
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(
            BusinessException exception,
            HttpServletRequest request
    ) {
        log.warn("event=exception.business status={} path={} exception={}", HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), exception.getClass().getSimpleName());
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("event=exception.validation status={} path={} fields={}", HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), validationErrors.keySet());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene errores de validacion",
                request,
                validationErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> validationErrors = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> getPropertyName(violation),
                        ConstraintViolation::getMessage,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
        log.warn("event=exception.constraint_violation status={} path={} fields={}", HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), validationErrors.keySet());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene errores de validacion",
                request,
                validationErrors
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn("event=exception.data_integrity status={} path={} exception={}", HttpStatus.CONFLICT.value(), request.getRequestURI(), exception.getClass().getSimpleName());
        return buildResponse(
                HttpStatus.CONFLICT,
                "No se pudo completar la operacion porque viola una restriccion de datos",
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        log.warn("event=exception.message_not_readable status={} path={} exception={}", HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), exception.getClass().getSimpleName());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud es invalido o no tiene el formato esperado",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        log.warn("event=exception.type_mismatch status={} path={} parameter={}", HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), exception.getName());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "El parametro '" + exception.getName() + "' no tiene un valor valido",
                request
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        log.warn("event=exception.missing_parameter status={} path={} parameter={}", HttpStatus.BAD_REQUEST.value(), request.getRequestURI(), exception.getParameterName());
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "El parametro '" + exception.getParameterName() + "' es obligatorio",
                request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        log.warn("event=exception.method_not_supported status={} path={} method={}", HttpStatus.METHOD_NOT_ALLOWED.value(), request.getRequestURI(), exception.getMethod());
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP no soportado para este recurso", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        log.warn("event=exception.access_denied status={} path={}", HttpStatus.FORBIDDEN.value(), request.getRequestURI());
        return buildResponse(HttpStatus.FORBIDDEN, "No tenes permisos para acceder a este recurso", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        log.warn("event=exception.authentication status={} path={}", HttpStatus.UNAUTHORIZED.value(), request.getRequestURI());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Se requiere autenticacion para acceder a este recurso", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception exception, HttpServletRequest request) {
        log.error("event=exception.unhandled status={} path={} exception={}", HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getRequestURI(), exception.getClass().getSimpleName(), exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return buildResponse(status, message, request, null);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> validationErrors
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                validationErrors
        ));
    }

    private String getPropertyName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        int lastSeparator = propertyPath.lastIndexOf('.');
        if (lastSeparator == -1) {
            return propertyPath;
        }
        return propertyPath.substring(lastSeparator + 1);
    }
}
