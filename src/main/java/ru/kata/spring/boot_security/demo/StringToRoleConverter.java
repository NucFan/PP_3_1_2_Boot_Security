package ru.kata.spring.boot_security.demo;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ru.kata.spring.boot_security.demo.dao.RoleDaoImpl;
import ru.kata.spring.boot_security.demo.model.Role;

@Component
public class StringToRoleConverter implements Converter<String, Role> {

    private final RoleDaoImpl roleDaoImpl;

    public StringToRoleConverter(RoleDaoImpl roleDaoImpl) {
        this.roleDaoImpl = roleDaoImpl;
    }

    @Override
    public Role convert(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        Long id = Long.parseLong(source);
        return roleDaoImpl.findById(id).orElse(null);
    }
}