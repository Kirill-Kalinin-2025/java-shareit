package ru.practicum.shareit.item.storage;

import ru.practicum.shareit.item.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemStorage {
    Item save(Item item);

    Item update(Item item);

    Optional<Item> findById(Long id);

    List<Item> findAll();

    List<Item> findByOwnerId(Long ownerId);

    List<Item> search(String text);

    void deleteById(Long id);
}