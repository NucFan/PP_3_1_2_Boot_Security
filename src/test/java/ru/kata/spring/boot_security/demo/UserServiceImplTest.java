package ru.kata.spring.boot_security.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.kata.spring.boot_security.demo.dao.UserDao; // Исправлено на UserDao
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.RoleService; // Добавлен мок сервиса ролей
import ru.kata.spring.boot_security.demo.service.UserServiceImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private UserServiceImpl userService;

    // ─────────────────────────────────────────────────────────
    // getAllUsers
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllUsers: возвращает всех пользователей из DAO")
    void shouldReturnAllUsers() {
        User u1 = new User();
        u1.setEmail("a@mail.com");
        User u2 = new User();
        u2.setEmail("b@mail.com");

        when(userDao.findAll()).thenReturn(List.of(u1, u2));

        List<User> result = userService.getAllUsers();

        assertThat(result).hasSize(2).containsExactly(u1, u2);
        verify(userDao, times(1)).findAll();
    }

    // ─────────────────────────────────────────────────────────
    // getUserById
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserById: возвращает Optional с пользователем, если найден")
    void shouldReturnUserById() {
        // given
        User user = new User();
        user.setId(1L);
        when(userDao.findById(1L)).thenReturn(Optional.of(user));

        // when
        Optional<User> result = userService.getUserById(1L);

        // then
        assertThat(result).isPresent().contains(user);
        verify(userDao, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getUserById: возвращает Optional.empty(), если пользователь не найден")
    void shouldReturnEmptyOptionalWhenUserNotFound() {
        // given
        when(userDao.findById(99L)).thenReturn(Optional.empty());

        // when
        Optional<User> result = userService.getUserById(99L);

        // then
        assertThat(result).isEmpty();
        verify(userDao, times(1)).findById(99L);
    }

    // ─────────────────────────────────────────────────────────
    // saveUser
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveUser: хеширует пароль, назначает ROLE_USER по умолчанию и делает persist")
    void shouldSaveUserWithDefaultRoleAndHashedPassword() {
        // given
        User newUser = new User();
        newUser.setEmail("new@mail.com");
        newUser.setPassword("rawPassword");
        // roles == null → должен быть назначен ROLE_USER

        Role userRole = new Role("ROLE_USER");
        userRole.setId(1L);


        when(roleService.getRoleByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("rawPassword")).thenReturn("$2a$hashed");


        // when
        userService.saveUser(newUser);

        // then


        assertThat(newUser.getPassword()).isEqualTo("$2a$hashed");
        assertThat(newUser.getRoles()).containsExactly(userRole);

        verify(passwordEncoder, times(1)).encode("rawPassword");
        verify(roleService, times(1)).getRoleByName("ROLE_USER");
        verify(roleService, never()).getManagedRoles(any());
        verify(userDao, times(1)).persist(newUser);

    }

    @Test
    @DisplayName("saveUser: если роли заданы явно — не назначает ROLE_USER по умолчанию")
    void shouldNotAssignDefaultRoleWhenRolesProvided() {
        User newUser = new User();
        newUser.setEmail("admin@mail.com");
        newUser.setPassword("raw");
        Role adminRole = new Role("ROLE_ADMIN");
        newUser.setRoles(new HashSet<>(Set.of(adminRole)));

        when(passwordEncoder.encode("raw")).thenReturn("hashed");
        when(roleService.getManagedRoles(any())).thenReturn(Set.of(adminRole));

        userService.saveUser(newUser);

        verify(roleService, never()).getRoleByName(any());
        verify(userDao, times(1)).persist(newUser);
        assertThat(newUser.getRoles()).containsExactly(adminRole);
    }

    @Test
    @DisplayName("saveUser: если роли пусты — тоже назначает ROLE_USER")
    void shouldAssignDefaultRoleWhenRolesEmpty() {
        User newUser = new User();
        newUser.setEmail("empty@mail.com");
        newUser.setPassword("raw");
        newUser.setRoles(new HashSet<>()); // пустой Set

        Role userRole = new Role("ROLE_USER");
        when(roleService.getRoleByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("raw")).thenReturn("hashed");


        userService.saveUser(newUser);

        verify(roleService, never()).getManagedRoles(any());
        assertThat(newUser.getRoles()).containsExactly(userRole);
    }

    @Test
    @DisplayName("saveUser: если ROLE_USER нет в БД — бросает IllegalStateException, persist не вызывается")
    void shouldThrowWhenDefaultRoleMissing() {
        // given
        User newUser = new User();
        newUser.setEmail("orphan@mail.com");
        newUser.setPassword("raw");
        // roles == null → сервис должен попытаться назначить ROLE_USER и упасть

        when(roleService.getRoleByName("ROLE_USER")).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> userService.saveUser(newUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ROLE_USER");

        verify(roleService, times(1)).getRoleByName("ROLE_USER");
        verify(passwordEncoder, never()).encode(any());
        verify(userDao, never()).persist(any());
    }

    // ─────────────────────────────────────────────────────────
    // updateUser
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUser: пустой пароль → сохраняется старый хеш")
    void shouldKeepOldPasswordWhenNewPasswordEmpty() {
        // given
        User existing = new User();
        existing.setId(1L);
        existing.setPassword("$2a$oldHash");

        User incoming = new User();
        incoming.setId(1L);
        incoming.setEmail("user@mail.com");
        incoming.setPassword(""); // пустой пароль из формы
        incoming.setRoles(new HashSet<>(Set.of(new Role("ROLE_USER"))));

        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(roleService.getManagedRoles(any())).thenReturn(existing.getRoles());

        // when
        userService.updateUser(incoming);

        // then
        assertThat(incoming.getPassword()).isEqualTo("$2a$oldHash");
        verify(passwordEncoder, never()).encode(any());
        verify(userDao, times(1)).merge(incoming);
    }

    @Test
    @DisplayName("updateUser: null-пароль → тоже сохраняется старый хеш")
    void shouldKeepOldPasswordWhenNewPasswordNull() {
        User existing = new User();
        existing.setId(1L);
        existing.setPassword("$2a$oldHash");

        User incoming = new User();
        incoming.setId(1L);
        incoming.setPassword(null);
        incoming.setRoles(new HashSet<>(Set.of(new Role("ROLE_USER"))));

        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(roleService.getManagedRoles(any())).thenReturn(existing.getRoles());

        userService.updateUser(incoming);

        assertThat(incoming.getPassword()).isEqualTo("$2a$oldHash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("updateUser: новый пароль → хешируется и заменяет старый")
    void shouldHashNewPasswordOnUpdate() {
        User existing = new User();
        existing.setId(1L);
        existing.setPassword("$2a$oldHash");

        User incoming = new User();
        incoming.setId(1L);
        incoming.setPassword("newRawPassword");
        incoming.setRoles(new HashSet<>(Set.of(new Role("ROLE_USER"))));

        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newRawPassword")).thenReturn("$2a$newHash");
        when(roleService.getManagedRoles(any())).thenReturn(existing.getRoles());

        userService.updateUser(incoming);

        assertThat(incoming.getPassword()).isEqualTo("$2a$newHash");
        verify(passwordEncoder, times(1)).encode("newRawPassword");
        verify(userDao, times(1)).merge(incoming);
    }


    @Test
    @DisplayName("updateUser: бросает IllegalArgumentException, если пользователя нет")
    void shouldThrowWhenUserNotFoundOnUpdate() {
        User incoming = new User();
        incoming.setId(99L);

        when(userDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(incoming))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(userDao, never()).merge(any());
    }

    @Test
    @DisplayName("updateUser: роли из формы превращаются в managed через RoleService")
    void shouldUseManagedRolesOnUpdate() {
        // given
        User existing = new User();
        existing.setId(1L);
        existing.setPassword("hash");

        Role adminRole = new Role("ROLE_ADMIN");
        adminRole.setId(2L);
        Set<Role> managedRoles = Set.of(adminRole);

        User incoming = new User();
        incoming.setId(1L);
        incoming.setPassword("");
        // имитируем "detached"-роли из формы
        Set<Role> formRoles = new HashSet<>();
        Role formAdmin = new Role("ROLE_ADMIN");
        formAdmin.setId(2L);
        formRoles.add(formAdmin);
        incoming.setRoles(formRoles);

        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(roleService.getManagedRoles(formRoles)).thenReturn(managedRoles);

        // when
        userService.updateUser(incoming);

        // then
        assertThat(incoming.getRoles()).containsExactly(adminRole);
        verify(roleService, times(1)).getManagedRoles(formRoles);
        verify(userDao, times(1)).merge(incoming);
    }

    // ─────────────────────────────────────────────────────────
    // deleteUser
    // ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteUser: вызывает deleteById у DAO")
    void shouldDeleteUserById() {
        userService.deleteUser(5L);

        verify(userDao, times(1)).deleteById(5L);
    }
}