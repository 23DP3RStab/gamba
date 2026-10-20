package com.gamba.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequest(
		@NotBlank(message = "First name is required")
		@Size(max = 30, message = "First name can be at most 30 characters")
		String firstName,

		@NotBlank(message = "Last name is required")
		@Size(max = 30, message = "Last name can be at most 30 characters")
		String lastName,

		@NotNull(message = "Date of birth is required")
		@PastOrPresent(message = "Date of birth cannot be in the future")
		LocalDate birthDate,

		@NotBlank(message = "E-mail is required")
		@Size(max = 40, message = "E-mail can be at most 40 characters")
		@Pattern(regexp = Rules.EMAIL, message = "Enter a valid e-mail address")
		String email,

		@NotNull(message = Rules.PHONE_MESSAGE)
		@Pattern(regexp = Rules.PHONE, message = Rules.PHONE_MESSAGE)
		String phone,

		@NotNull(message = Rules.PASSWORD_MESSAGE)
		@Pattern(regexp = Rules.PASSWORD, message = Rules.PASSWORD_MESSAGE)
		String password) {
}
