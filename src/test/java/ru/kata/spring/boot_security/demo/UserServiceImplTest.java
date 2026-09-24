package ru.kata.spring.boot_security.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private UserDao userDao; // Исправлено под ваш DAO

    @Mock
    private RoleService roleService; // Добавлен обязательный мок для бизнес-логики ролей

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private Role defaultRole;

    @BeforeEach
    void setUp() {
        defaultRole = new Role("ROLE_USER");
        defaultRole.setId(2L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Иван");
        testUser.setLastName("Иванов");
        testUser.setEmail("ivan@mail.com");
        testUser.setPassword("rawPassword");
        testUser.setRoles(new HashSet<>(Collections.singletonList(defaultRole)));
    }

    @Test
    @DisplayName("Должен возвращать список всех пользователей")
    void shouldReturnAllUsers() {
        // given
        when(userDao.findAll()).thenReturn(List.of(testUser));

        // when
        List<User> users = userService.getAllUsers();

        // then
        assertThat(users).hasSize(1).containsExactly(testUser);
        verify(userDao, times(1)).findAll();
    }

    @Test
    @DisplayName("Должен шифровать пароль, готовить управляемые роли и сохранять нового пользователя")
    void shouldEncryptPasswordAndSaveUser() {
        // given
        when(passwordEncoder.encode("rawPassword")).thenReturn("encryptedPassword");
        when(roleService.getManagedRoles(anySet())).thenReturn(Set.of(defaultRole));

        // when
        userService.saveUser(testUser);

        // then
        assertThat(testUser.getPassword()).isEqualTo("encryptedPassword");
        verify(passwordEncoder, times(1)).encode("rawPassword");
        verify(roleService, times(1)).getManagedRoles(anySet());
        verify(userDao, times(1)).persist(testUser); // Проверяем вызов метода persist
    }

    @Test
    @DisplayName("Должен автоматически назначать роль ROLE_USER, если при создании пользователя роли пустые")
    void shouldAssignDefaultRoleWhenRolesAreEmptyOnSave() {
        // given
        testUser.setRoles(null); // Имитируем пустые роли с фронтенда
        when(passwordEncoder.encode("rawPassword")).thenReturn("encryptedPassword");
        when(roleService.getRoleByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(roleService.getManagedRoles(anySet())).thenReturn(Set.of(defaultRole));

        // when
        userService.saveUser(testUser);

        // then
        assertThat(testUser.getRoles()).contains(defaultRole);
        verify(roleService, times(1)).getRoleByName("ROLE_USER");
        verify(userDao, times(1)).persist(testUser);
    }

    @Test
    @DisplayName("Должен шифровать пароль при обновлении, если передан новый пароль")
    void shouldEncryptNewPasswordOnUpdate() {
        // given
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPassword("oldEncryptedPassword");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setPassword("newRawPassword");
        updatedUser.setRoles(Set.of(defaultRole));

        when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newRawPassword")).thenReturn("newEncryptedPassword");
        when(roleService.getManagedRoles(anySet())).thenReturn(Set.of(defaultRole));

        // when
        userService.updateUser(updatedUser);

        // then
        assertThat(updatedUser.getPassword()).isEqualTo("newEncryptedPassword");
        verify(userDao, times(1)).merge(updatedUser); // Проверяем вызов метода merge
    }

    @Test
    @DisplayName("Не должен шифровать пароль повторно при обновлении, если пароль не изменился")
    void shouldNotEncryptPasswordOnUpdateIfUnchanged() {
        // given
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPassword("encryptedPassword");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setPassword("encryptedPassword");
        updatedUser.setRoles(Set.of(defaultRole));

        when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));
        when(roleService.getManagedRoles(anySet())).thenReturn(Set.of(defaultRole));

        // when
        userService.updateUser(updatedUser);

        // then
        assertThat(updatedUser.getPassword()).isEqualTo("encryptedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userDao, times(1)).merge(updatedUser);
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при обновлении несуществующего пользователя")
    void shouldThrowExceptionWhenUpdatingNonExistingUser() {
        // given
        when(userDao.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateUser(testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");

        verify(userDao, never()).merge(any(User.class));
    }

    @Test
    @DisplayName("Должен удалять пользователя по ID")
    void shouldDeleteUserById() {
        // when
        userService.deleteUser(1L);

        // then
        verify(userDao, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Должен возвращать пользователя по ID")
    void shouldReturnUserById() {
        // given
        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));

        // when
        User foundUser = userService.getUserById(1L);

        // then
        assertThat(foundUser).isNotNull().isEqualTo(testUser);
        verify(userDao, times(1)).findById(1L);
    }
}