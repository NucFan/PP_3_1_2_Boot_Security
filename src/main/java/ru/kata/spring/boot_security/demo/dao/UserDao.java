package ru.kata.spring.boot_security.demo.dao;

import ru.kata.spring.boot_security.demo.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {

    Optional<User> findByEmail(String email);
    List<User> findAll();
    Optional<User> findById(Long id);
    void deleteById(Long id);
    void persist(User user);
    User merge(User user);
}
