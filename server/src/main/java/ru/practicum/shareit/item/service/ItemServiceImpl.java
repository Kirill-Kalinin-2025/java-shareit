package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoWithBookings;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.CommentRepository;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.request.storage.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemRequestRepository requestRepository;
    private final BookingMapper bookingMapper;


    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));
    }

    @Override
    @Transactional
    public ItemDto create(ItemDto itemDto, Long ownerId) {
        getUserOrThrow(ownerId);

        if (itemDto.getRequestId() != null) {
            requestRepository.findById(itemDto.getRequestId())
                    .orElseThrow(() -> new NotFoundException("Запрос с id " + itemDto.getRequestId() + " не найден"));
        }

        Item item = ItemMapper.toItem(itemDto, ownerId);
        item.setRequestId(itemDto.getRequestId());
        Item saved = itemRepository.save(item);
        log.info("Created item with id: {} for owner: {}", saved.getId(), ownerId);
        return ItemMapper.toItemDto(saved);
    }

    @Override
    @Transactional
    public ItemDto update(Long itemId, ItemDto itemDto, Long ownerId) {
        Item existing = getItemOrThrow(itemId);

        if (!existing.getOwner().equals(ownerId)) {
            throw new ForbiddenException("Редактировать вещь может только владелец");
        }

        if (itemDto.getName() != null && !itemDto.getName().isBlank()) {
            existing.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) {
            existing.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            existing.setAvailable(itemDto.getAvailable());
        }

        Item updated = itemRepository.save(existing);
        log.info("Updated item with id: {}", itemId);
        return ItemMapper.toItemDto(updated);
    }

    @Override
    public ItemDtoWithBookings getById(Long itemId, Long userId) {
        Item item = getItemOrThrow(itemId);

        List<Comment> comments = commentRepository.findByItemIdOrderByCreatedDesc(itemId);
        List<CommentDto> commentDtos = getCommentDtosWithAuthorNames(comments);

        ItemDtoWithBookings itemDto = new ItemDtoWithBookings(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getAvailable()
        );
        itemDto.setComments(commentDtos);

        if (item.getOwner().equals(userId)) {
            addBookingInfoToItem(itemDto, itemId);
        }

        return itemDto;
    }

    @Override
    public List<ItemDtoWithBookings> getByOwnerId(Long ownerId) {
        getUserOrThrow(ownerId);

        List<Item> items = itemRepository.findByOwnerOrderById(ownerId);
        List<Long> itemIds = items.stream().map(Item::getId).toList();

        List<Comment> allComments = commentRepository.findByItemIdIn(itemIds);
        Map<Long, List<CommentDto>> commentsByItemId = allComments.stream()
                .collect(Collectors.groupingBy(
                        Comment::getItemId,
                        Collectors.mapping(comment ->
                                        CommentMapper.toCommentDto(comment, getUserName(comment.getAuthorId())),
                                Collectors.toList())
                ));

        List<Booking> allBookings = bookingRepository.findByItemIdInOrderByStartDesc(itemIds);
        Map<Long, List<Booking>> bookingsByItemId = allBookings.stream()
                .collect(Collectors.groupingBy(Booking::getItemId));

        return items.stream()
                .map(item -> {
                    ItemDtoWithBookings dto = new ItemDtoWithBookings(
                            item.getId(),
                            item.getName(),
                            item.getDescription(),
                            item.getAvailable()
                    );

                    dto.setComments(commentsByItemId.getOrDefault(item.getId(), Collections.emptyList()));

                    List<Booking> itemBookings = bookingsByItemId.getOrDefault(item.getId(), Collections.emptyList());
                    addBookingInfoFromList(dto, itemBookings);

                    return dto;
                })
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        // Проверка text.isBlank() перенесена в gateway
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(Long itemId, Long userId, CommentDto commentDto) {
        User user = getUserOrThrow(userId);
        Item item = getItemOrThrow(itemId);

        boolean hasBooked = bookingRepository.existsByItemIdAndBookerIdAndEndBefore(
                itemId, userId, LocalDateTime.now());

        if (!hasBooked) {
            throw new ValidationException("Пользователь не брал эту вещь в аренду");
        }

        Comment comment = CommentMapper.toComment(commentDto, itemId, userId);
        Comment saved = commentRepository.save(comment);
        log.info("Added comment to item {} from user {}", itemId, userId);

        return CommentMapper.toCommentDto(saved, user.getName());
    }

    private void addBookingInfoToItem(ItemDtoWithBookings itemDto, Long itemId) {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> lastBookings = bookingRepository.findByItemIdAndStatusOrderByStartAsc(
                itemId, BookingStatus.APPROVED);

        Booking lastBooking = lastBookings.stream()
                .filter(b -> b.getEnd().isBefore(now))
                .reduce((first, second) -> second)
                .orElse(null);

        Booking nextBooking = lastBookings.stream()
                .filter(b -> b.getStart().isAfter(now))
                .findFirst()
                .orElse(null);

        if (lastBooking != null) {
            itemDto.setLastBooking(bookingMapper.toBookingShortDto(lastBooking));
        }
        if (nextBooking != null) {
            itemDto.setNextBooking(bookingMapper.toBookingShortDto(nextBooking));
        }
    }

    private void addBookingInfoFromList(ItemDtoWithBookings itemDto, List<Booking> bookings) {
        LocalDateTime now = LocalDateTime.now();

        Booking lastBooking = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED)
                .filter(b -> b.getEnd().isBefore(now))
                .findFirst()
                .orElse(null);

        Booking nextBooking = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED)
                .filter(b -> b.getStart().isAfter(now))
                .reduce((first, second) -> second)
                .orElse(null);

        if (lastBooking != null) {
            itemDto.setLastBooking(bookingMapper.toBookingShortDto(lastBooking));
        }
        if (nextBooking != null) {
            itemDto.setNextBooking(bookingMapper.toBookingShortDto(nextBooking));
        }
    }

    private List<CommentDto> getCommentDtosWithAuthorNames(List<Comment> comments) {
        return comments.stream()
                .map(comment -> {
                    String authorName = getUserName(comment.getAuthorId());
                    return CommentMapper.toCommentDto(comment, authorName);
                })
                .toList();
    }

    private String getUserName(Long userId) {
        return userRepository.findById(userId)
                .map(User::getName)
                .orElse("Unknown User");
    }
}