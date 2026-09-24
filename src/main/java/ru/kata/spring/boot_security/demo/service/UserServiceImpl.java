package ru.kata.spring.boot_security.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.dao.UserDao;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;


import java.util.HashSet;
import java.util.List;
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
    public void saveUser(User user) {

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            Set<Role> defaultRoles = new HashSet<>();
            roleService.getRoleByName("ROLE_USER").ifPresent(defaultRoles::add);
            user.setRoles(defaultRoles);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Set<Role> managedRoles = roleService.getManagedRoles(user.getRoles());
        user.setRoles(managedRoles);
        userDao.persist(user);
    }

    @Override
    @Transactional
    public void updateUser(User user){
        User existingUser = userDao.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getPassword() != null && !user.getPassword().isEmpty()
        && !user.getPassword().equals(existingUser.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));

        } else {
            user.setPassword(existingUser.getPassword());
        }

        Set<Role> managedRoles = roleService.getManagedRoles(user.getRoles());
        user.setRoles(managedRoles);

        userDao.merge(user);

    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userDao.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id){
        return userDao.findById(id).orElse(null);
    }

}
