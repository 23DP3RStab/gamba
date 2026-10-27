package com.gamba.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 30, message = "First name can be at most 30 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 30, message = "Last name can be at most 30 characters")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate birthDate;

    @NotBlank(message = "Email is required")
    @Size(max = 40, message = "Email can be at most 40 characters")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Phone number must be 8 digits")
    @Pattern(regexp = "^[0-9]{8}$", message = "Phone number must be 8 digits")
    private String phone;
}
