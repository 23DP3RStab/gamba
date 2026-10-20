package com.gamba.backend.user.dto;

import com.gamba.backend.user.User;
import java.time.LocalDate;

public record ProfileResponse(String firstName, String lastName, LocalDate birthDate, String email, String phone) {

	public static ProfileResponse from(User user) {
		return new ProfileResponse(user.getFirstName(), user.getLastName(), user.getBirthDate(), user.getEmail(),
				user.getPhone());
	}

}
