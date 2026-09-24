package ru.kata.spring.boot_security.demo.service;

import ru.kata.spring.boot_security.demo.model.Role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleService {
    List<Role> getAllRoles();
    Optional<Role> getRoleByName(String name);
    Role saveRole(Role role);
    Optional<Role> getRoleById(Long id);
    Set<Role> getManagedRoles(Set<Role> roles);
}
