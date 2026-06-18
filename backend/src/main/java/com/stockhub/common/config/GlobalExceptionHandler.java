package com.stockhub.common.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.stockhub.common.dto.ProblemDetail;
import com.stockhub.common.exception.DuplicateStockException;
import com.stockhub.common.exception.ExportException;
import com.stockhub.common.exception.InvalidComparisonException;
import com.stockhub.common.exception.InvalidFilterException;
import com.stockhub.common.exception.ResourceNotFoundException;
import com.stockhub.common.exception.WatchlistLimitExceededException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.builder()
                .type(URI.create("https://api.stockhub.com/errors/resource-not-found").toString())
                .title("Resource Not Found")
                .status(HttpStatus.NOT_FOUND.value())
                .detail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.computeIfAbsent(fieldError.getField(), k -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.builder()
                .type(URI.create("https://api.stockhub.com/errors/validation-error").toString())
                .title("Validation Error")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("One or more fields failed validation")
                .errors(fieldErrors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.builder()
                .type(URI.create("https://api.stockhub.com/errors/forbidden").toString())
                .title("Access Denied")
                .status(HttpStatus.FORBIDDEN.value())
                .detail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(BadCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.builder()
                .type(URI.create("https://api.stockhub.com/errors/unauthorized").toString())
                .title("Authentication Failed")
                .status(HttpStatus.UNAUTHORIZED.value())
                .detail("Invalid credentials provided")
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(ConstraintViolationException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.computeIfAbsent(propertyPath, k -> new ArrayList<>()).add(message);
        }

        ProblemDetail problem = ProblemDetail.builder()
                .type(URI.create("https://api.stockhub.com/errors/validation-error").toString())
                .title("Constraint Violation")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("One or more constraints were violated")
                .errors(errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(DuplicateStockException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateStock(DuplicateStockException ex) {
        ProblemDetail problem = ProblemDetail.builder()
                .type(URI.create("https://api.stockhub.com/errors/conflict").toString())
                .title("Duplicate Stock")
                .status(HttpStatus.CONFLICT.value())
                .detail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(WatchlistLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleWatchlistLimit(WatchlistLimitExceededException ex) {
        ProblemDetail problem = ProblemDetail.builder()
                .type(URI.create("https://api.stockhub.com/errors/payment-required").toString())
                .title("Watchlist Limit Exceeded")
                .status(HttpStatus.PAYMENT_REQUIRED.value())
                .detail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(problem);
    }

    @ExceptionHandler(InvalidComparisonException.class)
    public ResponseEntity<ProblemDetail> handleInvalidComparison(InvalidComparisonException ex) {
        ProblemDetail problem = ProblemDetail.builder()
                .type(URI.create("https://api.stockhub.com/errors/bad-request").toString())
                .title("Invalid Comparison")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(InvalidFilterException.class)
    public ResponseEntity<ProblemDetail> handleInvalidFilter(InvalidFilterException ex) {
        ProblemDetail problem = ProblemDetail.builder()
                .type(URI.create("https://api.stockhub.com/errors/bad-request").toString())
                .title("Invalid Filter")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(ExportException.class)
    public ResponseEntity<ProblemDetail> handleExport(ExportException ex) {
        log.error("Export error: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.builder()
                .type(URI.create("https://api.stockhub.com/errors/internal-error").toString())
                .title("Export Failed")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        log.error("Unexpected error occurred", ex);
        ProblemDetail problem = ProblemDetail.builder()
                .type(URI.create("https://api.stockhub.com/errors/internal-error").toString())
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("An unexpected error occurred. Please try again later.")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
