package ru.kata.spring.boot_security.demo.dto;

import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;

import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        Integer age,
        String email,
        Set<String> roles
) {
    public static UserResponse from(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getAuthority)
                .collect(Collectors.toSet());
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getAge(),
                user.getEmail(),
                roleNames
        );
    }
}
