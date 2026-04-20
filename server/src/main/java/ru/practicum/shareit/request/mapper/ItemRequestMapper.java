package ru.practicum.shareit.request.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;

import java.time.LocalDateTime;
import java.util.List;

@UtilityClass
public class ItemRequestMapper {

    public ItemRequest toItemRequest(ItemRequestCreateDto dto, Long requesterId) {
        if (dto == null) {
            return null;
        }
        ItemRequest request = new ItemRequest();
        request.setDescription(dto.getDescription());
        request.setRequesterId(requesterId);
        request.setCreated(LocalDateTime.now());
        return request;
    }

    public ItemRequestDto toItemRequestDto(ItemRequest request, List<ItemDto> items) {
        if (request == null) {
            return null;
        }
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(request.getId());
        dto.setDescription(request.getDescription());
        dto.setCreated(request.getCreated());
        dto.setItems(items);
        return dto;
    }
}