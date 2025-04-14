package com.example.gateway.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<Object> handleTimeoutException(TimeoutException ex) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", 408);
        responseBody.put("error", "Request Timeout");
        responseBody.put("message", ex.getMessage());
        responseBody.put("timestamp", LocalDateTime.now());
        
        return new ResponseEntity<>(responseBody, HttpStatus.REQUEST_TIMEOUT);
    }
}