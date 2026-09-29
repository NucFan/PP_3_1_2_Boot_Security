package ru.kata.spring.boot_security.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.dao.RoleDao;
import ru.kata.spring.boot_security.demo.model.Role;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleDao roleDao;

    public RoleServiceImpl(RoleDao roleDao) {
        this.roleDao = roleDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> getRoleByName(String name) {
        return roleDao.findByName(name);
    }

    @Override
    @Transactional
    public Role saveRole(Role role) {
        if (role.getId() == null){
            roleDao.persist(role);
            return role;
        }
        return roleDao.merge(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> getRoleById(Long id) {
        return roleDao.findById(id);
    }

    @Override
    @Transactional
    public Set<Role> getManagedRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return new HashSet<>();
        }

        return roles.stream()
                .filter(r -> r.getId() != null)
                .map(r -> roleDao.findById(r.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: id=" + r.getId())))
                .collect(Collectors.toSet());
    }
}