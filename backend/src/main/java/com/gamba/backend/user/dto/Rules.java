package com.gamba.backend.user.dto;

// The same field rules as frontend/src/validation.js, so the server accepts exactly what the forms do
final class Rules {

	// An empty e-mail only gets the "required" message
	static final String EMAIL = "^$|[^\\s@]+@[^\\s@]+\\.[^\\s@]+";

	static final String PHONE = "\\d{8}";
	static final String PHONE_MESSAGE = "Phone number must be 8 digits";

	static final String PASSWORD = "(?s)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}";
	static final String PASSWORD_MESSAGE =
			"At least 8 characters with upper and lower case letters, a digit and a symbol";

	private Rules() {
	}

}
