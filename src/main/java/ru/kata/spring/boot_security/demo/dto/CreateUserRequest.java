package ru.kata.spring.boot_security.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(

        @NotBlank(message = "First name must not be blank")
        @Size(max = 50)
        String firstName,

        @NotBlank(message = "Last name must not be blank")
        @Size(max = 50)
        String lastName,

        @Min(0) @Max(150)
        Integer age,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 4, message = "Password must be at least 4 characters")
        String password,

        Set<Long> roleIds
) {
}
