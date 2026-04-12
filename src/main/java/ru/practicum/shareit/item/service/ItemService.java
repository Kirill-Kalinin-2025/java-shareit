package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

public interface ItemService {
    ItemDto create(ItemDto itemDto, Long ownerId);

    ItemDto update(Long itemId, ItemDto itemDto, Long ownerId);

    ItemDto getById(Long itemId);

    List<ItemDto> getByOwnerId(Long ownerId);

    List<ItemDto> search(String text);
}