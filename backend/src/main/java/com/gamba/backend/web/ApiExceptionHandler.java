package com.gamba.backend.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Every error reaches the client as { message, errors: { field: message } },
// so the forms can show each message next to its field
@RestControllerAdvice
public class ApiExceptionHandler {

	public record ApiError(String message, Map<String, String> errors) {
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : e.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		String message = errors.isEmpty() ? "Invalid request" : errors.values().iterator().next();
		return ResponseEntity.badRequest().body(new ApiError(message, errors));
	}

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> api(ApiException e) {
		Map<String, String> errors = e.getField() == null ? Map.of() : Map.of(e.getField(), e.getMessage());
		return ResponseEntity.status(e.getStatus()).body(new ApiError(e.getMessage(), errors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError("Invalid request", Map.of()));
	}

}
