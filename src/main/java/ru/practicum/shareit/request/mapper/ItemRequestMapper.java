package ru.practicum.shareit.request.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ItemRequestMapper {
    private final ItemRepository itemRepository;

    public ItemRequest toItemRequest(ItemRequestCreateDto dto, Long requesterId) {
        ItemRequest request = new ItemRequest();
        request.setDescription(dto.getDescription());
        request.setRequesterId(requesterId);
        request.setCreated(LocalDateTime.now());
        return request;
    }

    public ItemRequestDto toItemRequestDto(ItemRequest request) {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(request.getId());
        dto.setDescription(request.getDescription());
        dto.setCreated(request.getCreated());

        List<Item> items = itemRepository.findByRequestId(request.getId());
        List<ItemDto> itemDtos = items.stream()
                .map(ItemMapper::toItemDto)
                .toList();
        dto.setItems(itemDtos);

        return dto;
    }
}