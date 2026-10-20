package ru.kata.spring.boot_security.demo.service;

import ru.kata.spring.boot_security.demo.dto.CreateUserRequest;
import ru.kata.spring.boot_security.demo.dto.UpdateUserRequest;
import ru.kata.spring.boot_security.demo.model.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> getAllUsers();

    User saveUser(User user);

    User updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);

    User getUserById(Long id);

    User getUserByEmail(String email);

    Optional<User> findUserById(Long id);

    Optional<User> findUserByEmail(String email);

    User createUser(CreateUserRequest request);


}
