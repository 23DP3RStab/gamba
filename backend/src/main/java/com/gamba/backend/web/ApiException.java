package com.gamba.backend.web;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// An error the API reports to the client, optionally about one form field
@Getter
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String field; // null when the error is not about a single field

	public ApiException(HttpStatus status, String message) {
		this(status, null, message);
	}

	public ApiException(HttpStatus status, String field, String message) {
		super(message);
		this.status = status;
		this.field = field;
	}

}
