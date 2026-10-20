package com.autograder.appapi;

import com.autograder.submissions.SubmissionException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain errors to safe HTTP responses — never leaks internal details. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SubmissionException.class)
    public ResponseEntity<Map<String, String>> handle(SubmissionException e) {
        HttpStatus status = e.isNotFound() ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handle(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request body"));
    }
}
