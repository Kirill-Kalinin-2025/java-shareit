package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingDto create(Long userId, BookingCreateDto bookingCreateDto) {
        User user = getUserOrThrow(userId);
        Item item = getItemOrThrow(bookingCreateDto.getItemId());

        if (!item.getAvailable()) {
            throw new ValidationException("Вещь с id " + item.getId() + " недоступна для бронирования");
        }

        if (item.getOwner().equals(userId)) {
            throw new NotFoundException("Владелец не может бронировать свою вещь с id " + item.getId());
        }

        if (bookingCreateDto.getEnd().isBefore(bookingCreateDto.getStart())) {
            throw new ValidationException("Дата окончания не может быть раньше даты начала");
        }

        if (bookingCreateDto.getStart().equals(bookingCreateDto.getEnd())) {
            throw new ValidationException("Даты начала и окончания не могут совпадать");
        }

        Booking booking = bookingMapper.toBooking(bookingCreateDto);
        booking.setBookerId(userId);
        booking.setStatus(BookingStatus.WAITING);

        Booking saved = bookingRepository.save(booking);
        log.info("Created booking with id: {} for user: {} and item: {}",
                saved.getId(), userId, item.getId());

        return bookingMapper.toBookingDto(saved);
    }

    @Override
    @Transactional
    public BookingDto approve(Long bookingId, Long ownerId, boolean approved) {
        Booking booking = getBookingOrThrow(bookingId);
        Item item = getItemOrThrow(booking.getItemId());

        if (!item.getOwner().equals(ownerId)) {
            throw new ForbiddenException("Пользователь с id " + ownerId +
                    " не является владельцем вещи с id " + item.getId());
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Бронирование с id " + bookingId + " уже обработано");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        Booking saved = bookingRepository.save(booking);

        log.info("Booking {} was {} by owner {}", bookingId, approved ? "approved" : "rejected", ownerId);
        return bookingMapper.toBookingDto(saved);
    }

    @Override
    public BookingDto getById(Long bookingId, Long userId) {
        Booking booking = getBookingOrThrow(bookingId);
        Item item = getItemOrThrow(booking.getItemId());

        if (!booking.getBookerId().equals(userId) && !item.getOwner().equals(userId)) {
            throw new ForbiddenException("Пользователь с id " + userId +
                    " не имеет доступа к бронированию с id " + bookingId);
        }

        return bookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getAllByUser(Long userId, String state) {
        getUserOrThrow(userId);

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;

        switch (state.toUpperCase()) {
            case "CURRENT":
                bookings = bookingRepository.findCurrentBookingsByBookerId(userId, now);
                break;
            case "PAST":
                bookings = bookingRepository.findPastBookingsByBookerId(userId, now);
                break;
            case "FUTURE":
                bookings = bookingRepository.findFutureBookingsByBookerId(userId, now);
                break;
            case "WAITING":
                bookings = bookingRepository.findByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
                break;
            case "REJECTED":
                bookings = bookingRepository.findByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);
                break;
            default:
                bookings = bookingRepository.findByBookerIdOrderByStartDesc(userId);
        }

        return bookings.stream()
                .map(bookingMapper::toBookingDto)
                .toList();
    }

    @Override
    public List<BookingDto> getAllByOwner(Long ownerId, String state) {
        getUserOrThrow(ownerId);

        // Получаем все вещи владельца
        List<Item> ownerItems = itemRepository.findByOwnerOrderById(ownerId);
        List<Long> itemIds = ownerItems.stream().map(Item::getId).toList();

        if (itemIds.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;

        switch (state.toUpperCase()) {
            case "CURRENT":
                bookings = bookingRepository.findCurrentBookingsByOwnerId(itemIds, now);
                break;
            case "PAST":
                bookings = bookingRepository.findPastBookingsByOwnerId(itemIds, now);
                break;
            case "FUTURE":
                bookings = bookingRepository.findFutureBookingsByOwnerId(itemIds, now);
                break;
            case "WAITING":
                bookings = bookingRepository.findByItemOwnerIdAndStatusOrderByStartDesc(itemIds, BookingStatus.WAITING);
                break;
            case "REJECTED":
                bookings = bookingRepository.findByItemOwnerIdAndStatusOrderByStartDesc(itemIds, BookingStatus.REJECTED);
                break;
            default:
                bookings = bookingRepository.findByItemOwnerIdOrderByStartDesc(itemIds);
        }

        return bookings.stream()
                .map(bookingMapper::toBookingDto)
                .toList();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private Item getItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));
    }

    private Booking getBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование с id " + bookingId + " не найдено"));
    }
}