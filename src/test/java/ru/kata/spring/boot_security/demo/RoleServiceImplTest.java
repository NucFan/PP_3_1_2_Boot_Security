package ru.kata.spring.boot_security.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kata.spring.boot_security.demo.dao.RoleDao; // Убедитесь в правильности имени интерфейса (Dao или Repository)
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.service.RoleServiceImpl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleDao roleDao; // Внедряем мок зависимости

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    @DisplayName("Должен возвращать список всех ролей")
    void shouldReturnAllRoles() {
        // given
        Role adminRole = new Role("ROLE_ADMIN");
        Role userRole = new Role("ROLE_USER");
        when(roleDao.findAll()).thenReturn(List.of(adminRole, userRole));

        // when
        List<Role> roles = roleService.getAllRoles();

        // then
        assertThat(roles).hasSize(2).containsExactlyInAnyOrder(adminRole, userRole);
        verify(roleDao, times(1)).findAll();
    }

    @Test
    @DisplayName("Должен находить роль по имени")
    void shouldFindRoleByName() {
        // given
        Role adminRole = new Role("ROLE_ADMIN");
        when(roleDao.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));

        // when
        Optional<Role> foundRole = roleService.getRoleByName("ROLE_ADMIN"); // Исправлено: чистый Optional

        // then
        assertThat(foundRole).isPresent().contains(adminRole);
        verify(roleDao, times(1)).findByName("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Должен сохранять роль")
    void shouldSaveRole() {
        // given
        Role newRole = new Role("ROLE_NEW");
        when(roleDao.save(newRole)).thenReturn(newRole);

        // when
        Role savedRole = roleService.saveRole(newRole);

        // then
        assertThat(savedRole).isNotNull().isEqualTo(newRole);
        verify(roleDao, times(1)).save(newRole);
    }

    @Test
    @DisplayName("Должен возвращать управляемые роли из БД, если у них есть ID")
    void shouldReturnManagedRolesWhenIdsArePresent() {
        // given
        Role detachedAdmin = new Role("ROLE_ADMIN");
        detachedAdmin.setId(1L);
        Role detachedUser = new Role("ROLE_USER");
        detachedUser.setId(2L);

        Set<Role> detachedRoles = Set.of(detachedAdmin, detachedUser);

        when(roleDao.findById(1L)).thenReturn(Optional.of(detachedAdmin));
        when(roleDao.findById(2L)).thenReturn(Optional.of(detachedUser));

        // when
        Set<Role> managedRoles = roleService.getManagedRoles(detachedRoles);

        // then
        assertThat(managedRoles).hasSize(2).containsExactlyInAnyOrder(detachedAdmin, detachedUser);
        verify(roleDao, times(1)).findById(1L);
        verify(roleDao, times(1)).findById(2L);
    }

    @Test
    @DisplayName("Должен возвращать пустой Set, если передан пустой список ролей")
    void shouldReturnEmptySetWhenRolesAreEmpty() {
        // when
        Set<Role> managedRoles = roleService.getManagedRoles(Collections.emptySet());

        // then
        assertThat(managedRoles).isEmpty();
        verifyNoInteractions(roleDao); // Убеждаемся, что в базу запросов не было
    }
}