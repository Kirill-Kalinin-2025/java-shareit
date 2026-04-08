package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.storage.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserStorage userStorage;

    @Override
    public UserDto create(UserDto userDto) {
        if (userStorage.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("Пользователь с email " + userDto.getEmail() + " уже существует");
        }
        User user = UserMapper.toUser(userDto);
        User saved = userStorage.save(user);
        return UserMapper.toUserDto(saved);
    }

    @Override
    public UserDto update(Long id, UserDto userDto) {
        User existing = userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));

        if (userDto.getName() != null) {
            existing.setName(userDto.getName());
        }
        if (userDto.getEmail() != null && !userDto.getEmail().equals(existing.getEmail())) {
            if (userStorage.existsByEmail(userDto.getEmail())) {
                throw new RuntimeException("Пользователь с email " + userDto.getEmail() + " уже существует");
            }
            existing.setEmail(userDto.getEmail());
        }

        User updated = userStorage.update(existing);
        return UserMapper.toUserDto(updated);
    }

    @Override
    public UserDto getById(Long id) {
        User user = userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAll() {
        return userStorage.findAll().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    public void delete(Long id) {
        userStorage.deleteById(id);
    }
}