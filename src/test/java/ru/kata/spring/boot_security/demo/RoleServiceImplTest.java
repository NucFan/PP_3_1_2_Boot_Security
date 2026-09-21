package ru.kata.spring.boot_security.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kata.spring.boot_security.demo.dao.RoleRepository;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.service.RoleServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

    @ExtendWith(MockitoExtension.class)
    class RoleServiceImplTest {

        @Mock
        private RoleRepository roleRepository;

        @InjectMocks
        private RoleServiceImpl roleService;

        @Test
        @DisplayName("Должен возвращать список всех ролей")
        void shouldReturnAllRoles() {
            // given
            Role adminRole = new Role("ROLE_ADMIN");
            Role userRole = new Role("ROLE_USER");
            when(roleRepository.findAll()).thenReturn(List.of(adminRole, userRole));

            // when
            List<Role> roles = roleService.getAllRoles();

            // then
            assertThat(roles).hasSize(2).containsExactlyInAnyOrder(adminRole, userRole);
            verify(roleRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Должен находить роль по имени")
        void shouldFindRoleByName() {
            // given
            Role adminRole = new Role("ROLE_ADMIN");
            when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));

            // when
            Optional<Optional<Role>> foundRole = Optional.ofNullable(roleService.getRoleByName("ROLE_ADMIN"));

            // then
            assertThat(foundRole).isPresent();
            assertThat(foundRole.get()).contains(adminRole);
            verify(roleRepository, times(1)).findByName("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Должен сохранять роль")
        void shouldSaveRole() {
            // given
            Role newRole = new Role("ROLE_NEW");
            when(roleRepository.save(newRole)).thenReturn(newRole);

            // when
            Role savedRole = roleService.saveRole(newRole);

            // then
            assertThat(savedRole).isNotNull().isEqualTo(newRole);
            verify(roleRepository, times(1)).save(newRole);
        }
    }

