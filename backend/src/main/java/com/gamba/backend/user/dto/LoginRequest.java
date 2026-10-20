package com.gamba.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank(message = "Enter your e-mail and password")
		String email,

		@NotBlank(message = "Enter your e-mail and password")
		String password) {
}
