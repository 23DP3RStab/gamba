package com.gamba.backend.user;

import com.gamba.backend.user.dto.PasswordRequest;
import com.gamba.backend.user.dto.ProfileRequest;
import com.gamba.backend.user.dto.RegisterRequest;
import com.gamba.backend.web.ApiException;
import java.time.LocalDate;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

	private static final String EMAIL_TAKEN = "This e-mail is already registered";

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;

	public User register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (users.existsByEmail(email)) {
			throw new ApiException(HttpStatus.CONFLICT, "email", EMAIL_TAKEN);
		}
		User user = new User();
		setDetails(user, request.firstName(), request.lastName(), request.birthDate(), email, request.phone());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		return users.save(user);
	}

	@Transactional(readOnly = true)
	public User login(String email, String password) {
		return users.findByEmail(normalizeEmail(email))
				.filter((user) -> passwordEncoder.matches(password, user.getPasswordHash()))
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Wrong e-mail or password"));
	}

	// The logged-in user. Their account can be gone while their token is still valid.
	@Transactional(readOnly = true)
	public User get(Long id) {
		return users.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Your account no longer exists"));
	}

	public User updateProfile(Long id, ProfileRequest request) {
		User user = get(id);
		String email = normalizeEmail(request.email());
		if (users.existsByEmailAndIdNot(email, id)) {
			throw new ApiException(HttpStatus.CONFLICT, "email", EMAIL_TAKEN);
		}
		setDetails(user, request.firstName(), request.lastName(), request.birthDate(), email, request.phone());
		return user;
	}

	public void changePassword(Long id, PasswordRequest request) {
		User user = get(id);
		if (!passwordEncoder.matches(request.current(), user.getPasswordHash())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "current", "Current password is wrong");
		}
		user.setPasswordHash(passwordEncoder.encode(request.next()));
	}

	public void delete(Long id) {
		users.delete(get(id));
	}

	private static void setDetails(User user, String firstName, String lastName, LocalDate birthDate, String email,
			String phone) {
		user.setFirstName(firstName.trim());
		user.setLastName(lastName.trim());
		user.setBirthDate(birthDate);
		user.setEmail(email);
		user.setPhone(phone);
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
