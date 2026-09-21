package ru.kata.spring.boot_security.demo;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.kata.spring.boot_security.demo.dao.UserDaoImpl;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.UserServiceImpl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserDaoImpl userDaoImpl;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Иван");
        testUser.setLastName("Иванов");
        testUser.setEmail("ivan@mail.com");
        testUser.setPassword("rawPassword");
        testUser.setRoles(new HashSet<>(Collections.singletonList(new Role("ROLE_USER"))));
    }

    @Test
    @DisplayName("Должен возвращать список всех пользователей")
    void shouldReturnAllUsers() {
        // given
        when(userDaoImpl.findAll()).thenReturn(List.of(testUser));

        // when
        List<User> users = userService.getAllUsers();

        // then
        assertThat(users).hasSize(1).containsExactly(testUser);
        verify(userDaoImpl, times(1)).findAll();
    }

    @Test
    @DisplayName("Должен шифровать пароль и сохранять нового пользователя")
    void shouldEncryptPasswordAndSaveUser() {
        // given
        when(passwordEncoder.encode("rawPassword")).thenReturn("encryptedPassword");
        when(userDaoImpl.save(any(User.class))).thenReturn(testUser);

        // when
        userService.saveUser(testUser);

        // then
        assertThat(testUser.getPassword()).isEqualTo("encryptedPassword");
        verify(passwordEncoder, times(1)).encode("rawPassword");
        verify(userDaoImpl, times(1)).save(testUser);
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
        updatedUser.setPassword("newRawPassword"); // Передаем новый сырой пароль

        when(userDaoImpl.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newRawPassword")).thenReturn("newEncryptedPassword");

        // when
        userService.updateUser(updatedUser);

        // then
        assertThat(updatedUser.getPassword()).isEqualTo("newEncryptedPassword");
        verify(userDaoImpl, times(1)).save(updatedUser);
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
        updatedUser.setPassword("encryptedPassword"); // Пароль совпадает со старым

        when(userDaoImpl.findById(1L)).thenReturn(Optional.of(existingUser));

        // when
        userService.updateUser(updatedUser);

        // then
        assertThat(updatedUser.getPassword()).isEqualTo("encryptedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userDaoImpl, times(1)).save(updatedUser);
    }

    @Test
    @DisplayName("Должен выбрасывать исключение при обновлении несуществующего пользователя")
    void shouldThrowExceptionWhenUpdatingNonExistingUser() {
        // given
        when(userDaoImpl.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateUser(testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");

        verify(userDaoImpl, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Должен удалять пользователя по ID")
    void shouldDeleteUserById() {
        // when
        userService.deleteUser(1L);

        // then
        verify(userDaoImpl, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Должен возвращать пользователя по ID")
    void shouldReturnUserById() {
        // given
        when(userDaoImpl.findById(1L)).thenReturn(Optional.of(testUser));

        // when
        User foundUser = userService.getUserById(1L);

        // then
        assertThat(foundUser).isNotNull().isEqualTo(testUser);
        verify(userDaoImpl, times(1)).findById(1L);
    }
}