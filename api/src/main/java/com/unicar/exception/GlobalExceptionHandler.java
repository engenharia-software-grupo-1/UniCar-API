package com.unicar.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<Map<String, String>> handleAcessoNegado(AcessoNegadoException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN) // HTTP 403
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({
            CaronaNaoEncontradaException.class,
            VeiculoNaoEncontradoException.class,
            ReservaNaoEncontradaException.class,
            InteresseNaoEncontrado.class,
            TrajetoRecorrenteNaoEncontradoException.class
    })
    public ResponseEntity<Map<String, String>> handleNotFoundExceptions(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND) // HTTP 404
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({
            RegraDeNegocioException.class,
            EstadoInvalidoException.class,
            UsuarioNaoEncontradoException.class
    })
    public ResponseEntity<Map<String, String>> handleBadRequestExceptions(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // HTTP 400
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity
            .status(ex.getStatusCode())
            .body(Map.of("message", ex.getReason() != null ? ex.getReason() : ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String erros = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", "Dados inválidos.", "detalhes", erros));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(HttpMessageNotReadableException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", "Dados inválidos."));
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, String>> handleMissingHeader(org.springframework.web.bind.MissingRequestHeaderException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "O cabeçalho obrigatório '" + ex.getHeaderName() + "' está ausente."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("message", "Erro interno no servidor."));
    }
}