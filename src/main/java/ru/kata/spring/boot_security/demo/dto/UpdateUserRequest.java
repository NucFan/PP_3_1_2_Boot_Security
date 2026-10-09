package ru.kata.spring.boot_security.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(

        @Size(max = 50)
        String firstName,

        @Size(max = 50)
        String lastName,

        @Min(0) @Max(150)
        Integer age,

        @Email(message = "Email must be valid")
        String email,

        @Size(min = 4, message = "Password must be at least 4 characters")
        String password,

        Set<Long> roleIds
) {
}
