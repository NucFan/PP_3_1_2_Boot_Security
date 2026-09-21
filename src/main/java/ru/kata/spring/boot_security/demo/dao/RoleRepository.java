package ru.kata.spring.boot_security.demo.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.model.Role;

import java.util.List;
import java.util.Optional;

@Repository
public class RoleRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Role> findByName(String name) {
        try {
            Role role = entityManager.createQuery(
                            "SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", name)
                    .getSingleResult();
            return Optional.of(role);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<Role> findAll() {
        return entityManager.createQuery("SELECT r FROM Role r", Role.class)
                .getResultList();
    }


    public Role save(Role role) {
        if (role.getId() == null) {
            entityManager.persist(role);
            return role;
        } else {
            return entityManager.merge(role);
        }
    }

    public Optional<Role> findById(Long id) {
        Role role = entityManager.find(Role.class, id);
        return Optional.ofNullable(role);
    }
}