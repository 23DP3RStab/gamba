package com.gamba.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PasswordRequest(
		@NotBlank(message = "Enter your current password")
		String current,

		@NotNull(message = Rules.PASSWORD_MESSAGE)
		@Pattern(regexp = Rules.PASSWORD, message = Rules.PASSWORD_MESSAGE)
		String next) {
}
