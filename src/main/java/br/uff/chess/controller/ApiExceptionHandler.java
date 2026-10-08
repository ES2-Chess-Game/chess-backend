package br.uff.chess.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.uff.chess.service.exceptions.AvatarNotFoundException;
import br.uff.chess.service.exceptions.InvalidCredentialsException;
import br.uff.chess.service.exceptions.UserAlreadyExistsException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleConflict(UserAlreadyExistsException e) {
        return erro(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleCredentials(InvalidCredentialsException e) {
        return erro(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AvatarNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleAvatarNotFound(AvatarNotFoundException e) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst().orElse("Dados inválidos");
        return erro(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseEntity<Map<String, String>> erro(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body(Map.of("erro", msg));
    }
}
