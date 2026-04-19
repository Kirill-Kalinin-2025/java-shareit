package ru.practicum.shareit.item.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoWithBookings;
import ru.practicum.shareit.item.service.ItemService;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    public ItemDto create(@RequestBody ItemDto itemDto,
                          @RequestHeader("X-Sharer-User-Id") Long ownerId) {
        return itemService.create(itemDto, ownerId);
    }

    @PatchMapping("/{itemId}")
    public ItemDto update(@PathVariable Long itemId,
                          @RequestBody ItemDto itemDto,
                          @RequestHeader("X-Sharer-User-Id") Long ownerId) {
        return itemService.update(itemId, itemDto, ownerId);
    }

    @GetMapping("/{itemId}")
    public ItemDtoWithBookings getById(@PathVariable Long itemId,
                                       @RequestHeader("X-Sharer-User-Id") Long userId) {
        return itemService.getById(itemId, userId);
    }

    @GetMapping
    public List<ItemDtoWithBookings> getByOwner(@RequestHeader("X-Sharer-User-Id") Long ownerId) {
        return itemService.getByOwnerId(ownerId);
    }

    @GetMapping("/search")
    public List<ItemDto> search(@RequestParam String text) {
        return itemService.search(text);
    }

    @PostMapping("/{itemId}/comment")
    public CommentDto addComment(@PathVariable Long itemId,
                                 @RequestBody CommentDto commentDto,
                                 @RequestHeader("X-Sharer-User-Id") Long userId) {
        return itemService.addComment(itemId, userId, commentDto);
    }
}