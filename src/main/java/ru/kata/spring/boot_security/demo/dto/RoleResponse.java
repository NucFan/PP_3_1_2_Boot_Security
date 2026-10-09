package ru.kata.spring.boot_security.demo.dto;

import ru.kata.spring.boot_security.demo.model.Role;

public record RoleResponse(Long id, String name) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getAuthority());
    }
}
