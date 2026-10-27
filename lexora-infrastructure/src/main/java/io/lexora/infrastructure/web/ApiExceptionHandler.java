package io.lexora.infrastructure.web;

import io.lexora.application.common.NotFoundException;
import io.lexora.domain.shared.DomainException;
import io.lexora.domain.shared.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates use case failures into RFC 9457 problem details. Besides the English {@code detail}, each problem
 * carries a stable {@code code} ({@link ErrorCode}) and its {@code params}, which the clients translate.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("entity", e.what());
        params.put("id", String.valueOf(e.id()));
        return problem(HttpStatus.NOT_FOUND, "Not found", e.getMessage(), ErrorCode.NOT_FOUND, params);
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail businessRule(DomainException e) {
        return problem(HttpStatusCode.valueOf(422), "Business rule violated", e.getMessage(), e.code(), e.params());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden(AccessDeniedException e) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", "The user lacks the grant of this operation",
                ErrorCode.FORBIDDEN, Map.of());
    }

    /** Constraints of the contract checked on parameters (the generated interfaces are {@code @Validated}). */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail constraintViolation(ConstraintViolationException e) {
        return invalid(e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ProblemDetail malformed(Exception e) {
        return invalid(e instanceof MethodArgumentNotValidException m
                ? m.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage()).reduce((a, b) -> a + "; " + b).orElse("Invalid request")
                : e.getMessage());
    }

    @ExceptionHandler({NullPointerException.class, IllegalArgumentException.class, DateTimeParseException.class})
    ProblemDetail invalidInput(RuntimeException e) {
        return invalid(e.getMessage());
    }

    private static ProblemDetail invalid(String detail) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail == null ? "Invalid request" : detail,
                ErrorCode.INVALID_REQUEST, Map.of());
    }

    private static ProblemDetail problem(HttpStatusCode status, String title, String detail, ErrorCode code,
                                         Map<String, Object> params) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code.name());
        problem.setProperty("params", params);
        return problem;
    }
}
