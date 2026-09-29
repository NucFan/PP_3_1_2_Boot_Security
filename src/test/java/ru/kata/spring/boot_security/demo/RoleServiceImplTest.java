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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("Должен возвращать Optional.empty(), если роль по имени не найдена")
    void shouldReturnEmptyOptionalWhenRoleNotFound() {
        when(roleDao.findByName("ROLE_UNKNOWN")).thenReturn(Optional.empty());

        Optional<Role> foundRole = roleService.getRoleByName("ROLE_UNKNOWN");

        assertThat(foundRole).isEmpty();
        verify(roleDao, times(1)).findByName("ROLE_UNKNOWN");
    }

    @Test
    @DisplayName("saveRole для новой роли (id == null) → persist, возвращает тот же объект")
    void shouldPersistNewRole() {
        // given
        Role newRole = new Role("ROLE_NEW"); // id == null

        // when
        Role saved = roleService.saveRole(newRole);

        // then
        assertThat(saved).isSameAs(newRole);
        verify(roleDao, times(1)).persist(newRole);
        verify(roleDao, never()).merge(any());
    }

    @Test
    @DisplayName("saveRole для существующей роли (id != null) → merge, возвращает managed-инстанс")
    void shouldMergeExistingRole() {
        // given
        Role existing = new Role("ROLE_ADMIN");
        existing.setId(1L);

        Role merged = new Role("ROLE_ADMIN");
        merged.setId(1L);

        when(roleDao.merge(existing)).thenReturn(merged);

        // when
        Role result = roleService.saveRole(existing);

        // then
        assertThat(result).isSameAs(merged);
        verify(roleDao, times(1)).merge(existing);
        verify(roleDao, never()).persist(any());
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
    @DisplayName("getManagedRoles: role с id == null отбрасывается, в БД не идём")
    void shouldSkipRoleWithoutId() {
        // given
        Role transientRole = new Role("ROLE_NEW"); // id == null

        // when
        Set<Role> managedRoles = roleService.getManagedRoles(Set.of(transientRole));

        // then
        assertThat(managedRoles).isEmpty();
        verify(roleDao, never()).findById(any());
    }

    @Test
    @DisplayName("getManagedRoles: если role не найдена в БД — бросает IllegalArgumentException")
    void shouldThrowWhenRoleNotFoundInDb() {
        // given
        Role detached = new Role("ROLE_GHOST");
        detached.setId(99L);

        when(roleDao.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> roleService.getManagedRoles(Set.of(detached)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");

        verify(roleDao, times(1)).findById(99L);
    }

    @Test
    @DisplayName("getManagedRoles: пустой Set — возвращает пустой Set, DAO не вызывается")
    void shouldReturnEmptySetWhenRolesAreEmpty() {
        // when
        Set<Role> managedRoles = roleService.getManagedRoles(Collections.emptySet());

        // then
        assertThat(managedRoles).isEmpty();
        verifyNoInteractions(roleDao);
    }

    @Test
    @DisplayName("getManagedRoles: null — возвращает пустой Set, DAO не вызывается")
    void shouldReturnEmptySetWhenRolesAreNull() {
        // when
        Set<Role> managedRoles = roleService.getManagedRoles(null);

        // then
        assertThat(managedRoles).isEmpty();
        verifyNoInteractions(roleDao);
    }
}