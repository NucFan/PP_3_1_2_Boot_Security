package ru.kata.spring.boot_security.demo.rest;


import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.kata.spring.boot_security.demo.dto.CreateUserRequest;
import ru.kata.spring.boot_security.demo.dto.UpdateUserRequest;
import ru.kata.spring.boot_security.demo.dto.UserResponse;
import ru.kata.spring.boot_security.demo.exception.UserNotFoundException;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserService;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserService userService;
    private final RoleService roleService;

    public UserRestController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }


    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAllUsers().stream()
                .map(UserResponse::from)
                .toList();
    }


    @GetMapping("/{id}")
    public UserResponse getOne(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User currentuser) {
        if (currentuser == null) {
            throw new UserNotFoundException("anonymous");
        }
        return UserResponse.from(currentuser);
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody @Valid CreateUserRequest request) {

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setAge(request.age());
        user.setEmail(request.email());
        user.setPassword(request.password());

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            Set<Role> roles = resolveRoles(request.roleIds());
            user.setRoles(roles);
        }

        User saved = userService.saveUser(user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(UserResponse.from(saved));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id,
                               @RequestBody @Valid UpdateUserRequest request) {
        User existing = userService.getUserById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.firstName() != null) existing.setFirstName(request.firstName());
        if (request.lastName() != null) existing.setLastName(request.lastName());
        if (request.age() != null) existing.setAge(request.age());
        if (request.email() != null) existing.setEmail(request.email());
        if (request.password() != null && !request.password().isBlank()) {
            existing.setPassword(request.password());
        }
        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            existing.setRoles(resolveRoles(request.roleIds()));
        }

        User updated = userService.updateUser(existing);
        return UserResponse.from(updated);

    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (userService.getUserById(id).isEmpty()) {
            throw new UserNotFoundException(id);
        }
        userService.deleteUser(id);
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        return roleIds.stream()
                .map(roleId -> roleService.getRoleById(roleId)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: id=" + roleId)))
                .collect(Collectors.toSet());
    }
}
