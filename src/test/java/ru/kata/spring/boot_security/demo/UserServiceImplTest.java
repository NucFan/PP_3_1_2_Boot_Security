package ru.kata.spring.boot_security.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.kata.spring.boot_security.demo.dao.UserDao;
import ru.kata.spring.boot_security.demo.dto.UpdateUserRequest;
import ru.kata.spring.boot_security.demo.exception.UserNotFoundException;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.RoleService;
import ru.kata.spring.boot_security.demo.service.UserServiceImpl;

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

    // ═══════════════════════════════════════════════════════════
    //  getAllUsers
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("getAllUsers: возвращает список из DAO")
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

    // ═══════════════════════════════════════════════════════════
    //  getUserById — User (не Optional!), бросает UserNotFoundException
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("getUserById: найден → возвращает User")
    void shouldReturnUserById() {
        User user = new User();
        user.setId(1L);
        when(userDao.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertThat(result).isSameAs(user);
        verify(userDao, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getUserById: не найден → UserNotFoundException")
    void shouldThrowNotFoundOnGetById() {
        when(userDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userDao, times(1)).findById(99L);
    }

    // ═══════════════════════════════════════════════════════════
    //  getUserByEmail — User (не Optional!), бросает UserNotFoundException
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("getUserByEmail: найден → возвращает User")
    void shouldReturnUserByEmail() {
        User user = new User();
        user.setEmail("test@mail.com");
        when(userDao.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        User result = userService.getUserByEmail("test@mail.com");

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("getUserByEmail: не найден → UserNotFoundException")
    void shouldThrowNotFoundOnGetByEmail() {
        when(userDao.findByEmail("ghost@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("ghost@mail.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ═══════════════════════════════════════════════════════════
    //  findUserById / findUserByEmail — Optional (для MVC и Security)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("findUserById: найден → Optional с пользователем")
    void shouldFindUserById() {
        User user = new User();
        user.setId(1L);
        when(userDao.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findUserById(1L);

        assertThat(result).isPresent().contains(user);
    }

    @Test
    @DisplayName("findUserById: не найден → Optional.empty()")
    void shouldReturnEmptyOnFindById() {
        when(userDao.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.findUserById(99L)).isEmpty();
    }

    @Test
    @DisplayName("findUserByEmail: не найден → Optional.empty()")
    void shouldReturnEmptyOnFindByEmail() {
        when(userDao.findByEmail("ghost@mail.com")).thenReturn(Optional.empty());

        assertThat(userService.findUserByEmail("ghost@mail.com")).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════
    //  saveUser (для MVC + REST create)
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("saveUser: назначает ROLE_USER по умолчанию, хеширует пароль, persist")
    void shouldSaveUserWithDefaultRoleAndHashedPassword() {
        User newUser = new User();
        newUser.setEmail("new@mail.com");
        newUser.setPassword("rawPassword");

        Role userRole = new Role("ROLE_USER");
        userRole.setId(1L);

        when(roleService.getRoleByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("rawPassword")).thenReturn("$2a$hashed");

        userService.saveUser(newUser);

        assertThat(newUser.getPassword()).isEqualTo("$2a$hashed");
        assertThat(newUser.getRoles()).containsExactly(userRole);
        verify(passwordEncoder, times(1)).encode("rawPassword");
        verify(roleService, times(1)).getRoleByName("ROLE_USER");
        verify(roleService, never()).getManagedRoles(any());
        verify(userDao, times(1)).persist(newUser);
    }

    @Test
    @DisplayName("saveUser: роли заданы → getManagedRoles, без ROLE_USER")
    void shouldUseManagedRolesWhenProvided() {
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
    @DisplayName("saveUser: пустой password → IllegalArgumentException")
    void shouldThrowWhenPasswordEmpty() {
        User newUser = new User();
        newUser.setEmail("x@mail.com");
        newUser.setPassword("");

        assertThatThrownBy(() -> userService.saveUser(newUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password");

        verify(userDao, never()).persist(any());
    }

    @Test
    @DisplayName("saveUser: ROLE_USER нет в БД → IllegalStateException")
    void shouldThrowWhenDefaultRoleMissing() {
        User newUser = new User();
        newUser.setEmail("orphan@mail.com");
        newUser.setPassword("raw");

        when(roleService.getRoleByName("ROLE_USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.saveUser(newUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ROLE_USER");

        verify(passwordEncoder, never()).encode(any());
        verify(userDao, never()).persist(any());
    }

    // ═══════════════════════════════════════════════════════════
    //  updateUser(Long, UpdateUserRequest) — новая сигнатура
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("updateUser(Long): не найден → UserNotFoundException, merge не вызывается")
    void shouldThrowWhenUserNotFoundOnUpdate() {
        UpdateUserRequest request = new UpdateUserRequest(
                "New", null, null, null, null, null);

        when(userDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, request))
                .isInstanceOf(UserNotFoundException.class);

        verify(userDao, never()).merge(any());
    }

    @Test
    @DisplayName("updateUser(Long): обновляет только переданные поля")
    void shouldUpdateOnlyProvidedFields() {
        User existing = new User();
        existing.setId(1L);
        existing.setFirstName("Old");
        existing.setLastName("OldLast");
        existing.setPassword("oldHash");

        UpdateUserRequest request = new UpdateUserRequest(
                "New", null, null, null, null, null);

        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(userDao.merge(existing)).thenReturn(existing);

        userService.updateUser(1L, request);

        assertThat(existing.getFirstName()).isEqualTo("New");
        assertThat(existing.getLastName()).isEqualTo("OldLast");   // не менялось
        assertThat(existing.getPassword()).isEqualTo("oldHash");   // не менялось
        verify(passwordEncoder, never()).encode(any());
        verify(userDao, times(1)).merge(existing);
    }

    @Test
    @DisplayName("updateUser(Long): новый пароль хешируется")
    void shouldHashNewPassword() {
        User existing = new User();
        existing.setId(1L);
        existing.setPassword("oldHash");

        UpdateUserRequest request = new UpdateUserRequest(
                null, null, null, null, "newRaw", null);

        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newRaw")).thenReturn("newHash");
        when(userDao.merge(existing)).thenReturn(existing);

        userService.updateUser(1L, request);

        assertThat(existing.getPassword()).isEqualTo("newHash");
        verify(passwordEncoder, times(1)).encode("newRaw");
    }

    @Test
    @DisplayName("updateUser(Long): пустой пароль → пароль не меняется")
    void shouldNotTouchPasswordWhenEmpty() {
        User existing = new User();
        existing.setId(1L);
        existing.setPassword("oldHash");

        UpdateUserRequest request = new UpdateUserRequest(
                null, null, null, null, "", null);

        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(userDao.merge(existing)).thenReturn(existing);

        userService.updateUser(1L, request);

        assertThat(existing.getPassword()).isEqualTo("oldHash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("updateUser(Long): roleIds переданы → getManagedRolesByIds")
    void shouldUpdateRolesWhenIdsProvided() {
        User existing = new User();
        existing.setId(1L);

        Set<Long> roleIds = Set.of(2L);
        Role admin = new Role("ROLE_ADMIN");
        admin.setId(2L);

        UpdateUserRequest request = new UpdateUserRequest(
                null, null, null, null, null, roleIds);

        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(roleService.getManagedRolesByIds(roleIds)).thenReturn(Set.of(admin));
        when(userDao.merge(existing)).thenReturn(existing);

        userService.updateUser(1L, request);

        assertThat(existing.getRoles()).containsExactly(admin);
        verify(roleService, times(1)).getManagedRolesByIds(roleIds);
    }

    @Test
    @DisplayName("updateUser(Long): roleIds null → роли не трогаются")
    void shouldNotTouchRolesWhenIdsNull() {
        User existing = new User();
        existing.setId(1L);
        Role oldRole = new Role("ROLE_USER");
        existing.setRoles(Set.of(oldRole));

        UpdateUserRequest request = new UpdateUserRequest(
                null, null, null, null, null, null);

        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(userDao.merge(existing)).thenReturn(existing);

        userService.updateUser(1L, request);

        assertThat(existing.getRoles()).containsExactly(oldRole);
        verify(roleService, never()).getManagedRolesByIds(any());
    }

    @Test
    @DisplayName("updateUser(Long): roleIds пусты → роли не трогаются")
    void shouldNotTouchRolesWhenIdsEmpty() {
        User existing = new User();
        existing.setId(1L);
        Role oldRole = new Role("ROLE_USER");
        existing.setRoles(Set.of(oldRole));

        UpdateUserRequest request = new UpdateUserRequest(
                null, null, null, null, null, Set.of());

        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(userDao.merge(existing)).thenReturn(existing);

        userService.updateUser(1L, request);

        assertThat(existing.getRoles()).containsExactly(oldRole);
        verify(roleService, never()).getManagedRolesByIds(any());
    }

    // ═══════════════════════════════════════════════════════════
    //  deleteUser — теперь проверяет существование
    // ═══════════════════════════════════════════════════════════

    @Test
    @DisplayName("deleteUser: пользователь есть → deleteById")
    void shouldDeleteExistingUser() {
        User user = new User();
        user.setId(5L);

        when(userDao.findById(5L)).thenReturn(Optional.of(user));

        userService.deleteUser(5L);

        verify(userDao, times(1)).deleteById(5L);
    }

    @Test
    @DisplayName("deleteUser: пользователя нет → UserNotFoundException, deleteById не вызывается")
    void shouldThrowWhenDeletingMissingUser() {
        when(userDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(UserNotFoundException.class);

        verify(userDao, never()).deleteById(any());
    }
}