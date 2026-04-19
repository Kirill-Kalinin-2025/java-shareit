package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoWithBookings;

import java.util.List;

public interface ItemService {
    ItemDto create(ItemDto itemDto, Long ownerId);

    ItemDto update(Long itemId, ItemDto itemDto, Long ownerId);

    ItemDtoWithBookings getById(Long itemId, Long userId);

    List<ItemDtoWithBookings> getByOwnerId(Long ownerId);

    List<ItemDto> search(String text);

    CommentDto addComment(Long itemId, Long userId, CommentDto commentDto);
}