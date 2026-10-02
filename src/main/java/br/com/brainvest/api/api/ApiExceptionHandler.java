package br.com.brainvest.api.api;

import br.com.brainvest.api.curriculum.CurriculumNotFoundException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiModels.ErrorResponse> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.status()).body(
                new ApiModels.ErrorResponse(exception.code(), exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(CurriculumNotFoundException.class)
    ResponseEntity<ApiModels.ErrorResponse> handleCurriculumNotFound(CurriculumNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiModels.ErrorResponse("CURRICULUM_NOT_FOUND", exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiModels.ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": valor inválido")
                .orElse("A solicitação contém campos inválidos.");
        return ResponseEntity.badRequest().body(
                new ApiModels.ErrorResponse("VALIDATION_ERROR", message, Instant.now()));
    }
}