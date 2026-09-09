package ru.kata.spring.boot_security.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.kata.spring.boot_security.demo.dao.RoleRepository;
import ru.kata.spring.boot_security.demo.dao.UserRepository;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;

import java.util.Collections;
import java.util.HashSet;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;


    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            roleRepository.save(new Role("ROLE_USER"));
        }

        if (userRepository.findByEmail("admin@mail.com").isEmpty()) {

            Role adminRole = new Role("ROLE_ADMIN");

            adminRole = roleRepository.save(adminRole);

            User admin = new User();
            admin.setFirstName("Главный");
            admin.setLastName("Администратор");
            admin.setEmail("admin@mail.com");

            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setRoles(new HashSet<>(Collections.singletonList(adminRole)));

            userRepository.save(admin);

            System.out.println("====== ТЕСТОВЫЙ АДМИН УСПЕШНО СОЗДАН ======");
            System.out.println("Логин (Email): admin@mail.com");
            System.out.println("Пароль: admin");
            System.out.println("===========================================");
        }
    }
}
