package ru.practicum.shareit.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = ru.practicum.shareit.ShareItServer.class)
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userDto = new UserDto(null, "Test User", "test@example.com");
    }

    @Test
    void createUser_shouldReturnUserWithId() {
        UserDto created = userService.create(userDto);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Test User");
        assertThat(created.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void createUser_withDuplicateEmail_shouldThrowConflictException() {
        userService.create(userDto);
        UserDto duplicate = new UserDto(null, "Another", "test@example.com");

        assertThrows(ConflictException.class, () -> userService.create(duplicate));
    }

    @Test
    void getUserById_shouldReturnUser() {
        UserDto created = userService.create(userDto);
        UserDto found = userService.getById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo(created.getName());
        assertThat(found.getEmail()).isEqualTo(created.getEmail());
    }

    @Test
    void getUserById_withInvalidId_shouldThrowNotFoundException() {
        assertThrows(NotFoundException.class, () -> userService.getById(999L));
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        userService.create(userDto);
        userService.create(new UserDto(null, "User2", "user2@example.com"));

        List<UserDto> users = userService.getAll();

        assertThat(users).hasSize(2);
    }

    @Test
    void updateUser_shouldUpdateName() {
        UserDto created = userService.create(userDto);
        UserDto update = new UserDto(null, "Updated Name", null);

        UserDto updated = userService.update(created.getId(), update);

        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getEmail()).isEqualTo(created.getEmail());
    }

    @Test
    void updateUser_shouldUpdateEmail() {
        UserDto created = userService.create(userDto);
        UserDto update = new UserDto(null, null, "updated@example.com");

        UserDto updated = userService.update(created.getId(), update);

        assertThat(updated.getName()).isEqualTo(created.getName());
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void updateUser_withDuplicateEmail_shouldThrowConflictException() {
        userService.create(userDto);
        UserDto second = userService.create(new UserDto(null, "Second", "second@example.com"));
        UserDto update = new UserDto(null, "Updated", "test@example.com");

        assertThrows(ConflictException.class, () -> userService.update(second.getId(), update));
    }

    @Test
    void deleteUser_shouldRemoveUser() {
        UserDto created = userService.create(userDto);
        userService.delete(created.getId());

        assertThrows(NotFoundException.class, () -> userService.getById(created.getId()));
    }
}