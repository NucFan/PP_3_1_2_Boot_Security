package ru.kata.spring.boot_security.demo.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kata.spring.boot_security.demo.dto.RoleResponse;
import ru.kata.spring.boot_security.demo.exception.UserNotFoundException;
import ru.kata.spring.boot_security.demo.service.RoleService;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleRestController {

    private final RoleService roleService;

    public RoleRestController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public List<RoleResponse> getAll() {
        return roleService.getAllRoles().stream()
                .map(RoleResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public RoleResponse getOne(@PathVariable Long id) {
        return roleService.getRoleById(id)
                .map(RoleResponse::from)
                .orElseThrow(() -> new UserNotFoundException("role id=" + id));
    }
}
