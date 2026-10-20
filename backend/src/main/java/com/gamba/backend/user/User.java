package com.gamba.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String firstName;

	@Column(nullable = false, length = 30)
	private String lastName;

	@Column(nullable = false)
	private LocalDate birthDate;

	// Stored in lower case, so the same address can't be registered twice in different case
	@Column(nullable = false, unique = true, length = 40)
	private String email;

	@Column(nullable = false, length = 8)
	private String phone;

	// BCrypt hash, never the password itself
	@Column(nullable = false)
	private String passwordHash;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

}
