package ru.kata.spring.boot_security.demo.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class UserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<User> findByEmail(String email) {
        try {
            User user = entityManager.createQuery(
                            "SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<User> findAll() {
        return entityManager.createQuery(
                        "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles", User.class)
                .getResultList();
    }

    public Optional<User> findById(Long id) {
        try {
            User user = entityManager.createQuery(
                            "SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id", User.class)
                    .setParameter("id", id)
                    .getSingleResult();
            return Optional.of(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }


    public User save(User user) {

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            Set<Role> managedRoles = user.getRoles().stream()
                    .map(role -> role.getId() != null ? entityManager.merge(role) : role)
                    .collect(Collectors.toSet());
            user.setRoles(managedRoles);
        }

        if (user.getId() == null) {
            entityManager.persist(user);
            return user;
        } else {
            return entityManager.merge(user);
        }
    }


    public void deleteById(Long id) {
        User user = entityManager.find(User.class, id);
        if (user != null) {
            entityManager.remove(user);
        }
    }

}
