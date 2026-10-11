package com.mocktestapp.controller;

import com.mocktestapp.dto.TestDto;
import com.mocktestapp.dto.QuestionDto;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Hidden
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "Invalid"));
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation failed",
                "details", errors,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(Exception ex) {
        return ResponseEntity.status(403).body(Map.of(
                "error", "Access denied. Required role not present.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
