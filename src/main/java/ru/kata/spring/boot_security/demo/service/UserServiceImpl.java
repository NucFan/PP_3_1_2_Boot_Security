package ru.kata.spring.boot_security.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.dao.UserDao;
import ru.kata.spring.boot_security.demo.dto.CreateUserRequest;
import ru.kata.spring.boot_security.demo.dto.UpdateUserRequest;
import ru.kata.spring.boot_security.demo.exception.UserNotFoundException;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;


import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService{

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    public UserServiceImpl(UserDao userDao, PasswordEncoder passwordEncoder, RoleService roleService) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
    }


    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    @Override
    @Transactional
    public User saveUser(User user) {

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }


        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            Role userRole = roleService.getRoleByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException("Default role ROLE_USER not found"));
            user.setRoles(new HashSet<>(Set.of(userRole)));
        } else {
            user.setRoles(roleService.getManagedRoles(user.getRoles()));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userDao.persist(user);
        return user;
    }

    @Override
    @Transactional
    public User updateUser(Long id, UpdateUserRequest request){
        User existing = userDao.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.firstName() != null) existing.setFirstName(request.firstName());
        if (request.lastName() != null) existing.setLastName(request.lastName());
        if (request.age() != null) existing.setAge(request.age());
        if (request.email() != null) existing.setEmail(request.email());

        if (request.password() != null && !request.password().isBlank()) {
            existing.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            existing.setRoles(roleService.getManagedRolesByIds(request.roleIds()));
        }

        return userDao.merge(existing);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userDao.findById(id)
                        .orElseThrow(() -> new UserNotFoundException(id));
        userDao.deleteById(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id){
        return userDao.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userDao.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUserById(Long id) {
        return userDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUserByEmail(String email) {
        return userDao.findByEmail(email);
    }

    @Override
    @Transactional
    public User createUser(CreateUserRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Password must not be empty");
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setAge(request.age());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));


        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            user.setRoles(roleService.getManagedRolesByIds(request.roleIds()));
        } else {
            Role userRole = roleService.getRoleByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException("Default role ROLE_USER not found"));
            user.setRoles(new HashSet<>(Set.of(userRole)));
        }

        userDao.persist(user);
        return user;
    }

}
