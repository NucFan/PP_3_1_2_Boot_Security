package ru.kata.spring.boot_security.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.dao.RoleDao;
import ru.kata.spring.boot_security.demo.dao.UserDao;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final RoleDao roleDao;


    public DataInitializer(UserDao userDao, PasswordEncoder passwordEncoder, RoleDao roleDao) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.roleDao = roleDao;
    }

    @Override
    @Transactional
    public void run(String... args) {

        Role userRole = getOrCreateRole("ROLE_USER");
        Role adminRole = getOrCreateRole("ROLE_ADMIN");

        if (userDao.findByEmail("admin@mail.com").isEmpty()) {

            User admin = new User();
            admin.setFirstName("Главный");
            admin.setLastName("Администратор");
            admin.setEmail("admin@mail.com");
            admin.setPassword(passwordEncoder.encode("admin"));


            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            roles.add(userRole);
            admin.setRoles(roles);

            userDao.persist(admin);

            System.out.println("====== ТЕСТОВЫЙ АДМИН УСПЕШНО СОЗДАН ======");
            System.out.println("Логин (Email): admin@mail.com");
            System.out.println("Пароль: admin");
            System.out.println("Роли: ROLE_ADMIN, ROLE_USER");
            System.out.println("===========================================");
        }

    }
    private Role getOrCreateRole(String name) {
        return roleDao.findByName(name).orElseGet(() -> {
            Role role = new Role(name);
            roleDao.persist(role);
            return role;
        });
    }
}
