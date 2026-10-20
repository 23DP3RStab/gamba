package com.gamba.backend.profile;

import com.gamba.backend.user.UserService;
import com.gamba.backend.user.dto.PasswordRequest;
import com.gamba.backend.user.dto.ProfileRequest;
import com.gamba.backend.user.dto.ProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// The logged-in user's own profile; who that is comes from their token
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

	private final UserService userService;

	@GetMapping
	public ProfileResponse get(@AuthenticationPrincipal Jwt token) {
		return ProfileResponse.from(userService.get(userId(token)));
	}

	@PutMapping
	public ProfileResponse update(@AuthenticationPrincipal Jwt token, @Valid @RequestBody ProfileRequest request) {
		return ProfileResponse.from(userService.updateProfile(userId(token), request));
	}

	@PutMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@AuthenticationPrincipal Jwt token, @Valid @RequestBody PasswordRequest request) {
		userService.changePassword(userId(token), request);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt token) {
		userService.delete(userId(token));
	}

	private static Long userId(Jwt token) {
		return Long.valueOf(token.getSubject());
	}

}
