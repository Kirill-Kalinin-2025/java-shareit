package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.storage.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemRequestDto create(Long userId, ItemRequestCreateDto dto) {
        User user = getUserOrThrow(userId);

        ItemRequest request = ItemRequestMapper.toItemRequest(dto, userId);
        ItemRequest saved = requestRepository.save(request);
        log.info("Created item request with id: {} for user: {}", saved.getId(), userId);

        return ItemRequestMapper.toItemRequestDto(saved, Collections.emptyList());
    }

    @Override
    public List<ItemRequestDto> getUserRequests(Long userId) {
        getUserOrThrow(userId);

        List<ItemRequest> requests = requestRepository.findByRequesterIdOrderByCreatedDesc(userId);
        return enrichRequestsWithItems(requests);
    }

    @Override
    public List<ItemRequestDto> getAllRequests(Long userId, int from, int size) {
        getUserOrThrow(userId);

        PageRequest pageRequest = PageRequest.of(from / size, size, Sort.by("created").descending());

        List<ItemRequest> requests = requestRepository.findByRequesterIdNotOrderByCreatedDesc(userId, pageRequest)
                .toList();
        return enrichRequestsWithItems(requests);
    }

    @Override
    public ItemRequestDto getById(Long userId, Long requestId) {
        getUserOrThrow(userId);

        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id " + requestId + " не найден"));

        List<Item> items = itemRepository.findByRequestId(requestId);
        List<ItemDto> itemDtos = items.stream()
                .map(ItemMapper::toItemDto)
                .toList();

        return ItemRequestMapper.toItemRequestDto(request, itemDtos);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private List<ItemRequestDto> enrichRequestsWithItems(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        // Собираем все ID запросов
        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .toList();

        // Одним запросом получаем все вещи для этих запросов
        List<Item> items = itemRepository.findByRequestIdIn(requestIds);

        // Группируем вещи по requestId
        Map<Long, List<ItemDto>> itemsByRequestId = items.stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.groupingBy(
                        ItemDto::getRequestId,
                        Collectors.toList()
                ));

        // Обогащаем каждый запрос его вещами
        return requests.stream()
                .map(request -> {
                    List<ItemDto> requestItems = itemsByRequestId.getOrDefault(
                            request.getId(),
                            Collections.emptyList()
                    );
                    return ItemRequestMapper.toItemRequestDto(request, requestItems);
                })
                .toList();
    }
}