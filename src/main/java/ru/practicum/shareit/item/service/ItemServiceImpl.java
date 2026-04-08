package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.storage.ItemStorage;
import ru.practicum.shareit.user.storage.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemStorage itemStorage;
    private final UserStorage userStorage;

    @Override
    public ItemDto create(ItemDto itemDto, Long ownerId) {
        userStorage.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + ownerId + " не найден"));

        Item item = ItemMapper.toItem(itemDto, ownerId);
        Item saved = itemStorage.save(item);
        return ItemMapper.toItemDto(saved);
    }

    @Override
    public ItemDto update(Long itemId, ItemDto itemDto, Long ownerId) {
        Item existing = itemStorage.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));

        if (!existing.getOwner().equals(ownerId)) {
            throw new ForbiddenException("Редактировать вещь может только владелец");
        }

        if (itemDto.getName() != null) {
            existing.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            existing.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            existing.setAvailable(itemDto.getAvailable());
        }

        Item updated = itemStorage.update(existing);
        return ItemMapper.toItemDto(updated);
    }

    @Override
    public ItemDto getById(Long itemId) {
        Item item = itemStorage.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));
        return ItemMapper.toItemDto(item);
    }

    @Override
    public List<ItemDto> getByOwnerId(Long ownerId) {
        return itemStorage.findByOwnerId(ownerId).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        return itemStorage.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }
}