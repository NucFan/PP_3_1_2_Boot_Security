package ru.kata.spring.boot_security.demo;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ru.kata.spring.boot_security.demo.dao.RoleRepository;
import ru.kata.spring.boot_security.demo.model.Role;

@Component
public class StringToRoleConverter implements Converter<String, Role> {

    private final RoleRepository roleRepository;

    public StringToRoleConverter(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Role convert(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        Long id = Long.parseLong(source);
        return roleRepository.findById(id).orElse(null);
    }
}