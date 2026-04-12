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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Item item = itemRepository.findById(bookingCreateDto.getItemId())
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        if (!item.getAvailable()) {
            throw new ValidationException("Вещь недоступна для бронирования");
        }

        if (item.getOwner().equals(userId)) {
            throw new NotFoundException("Владелец не может бронировать свою вещь");
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
        log.info("Created booking with id: {}", saved.getId());

        return bookingMapper.toBookingDto(saved);
    }

    @Override
    @Transactional
    public BookingDto approve(Long bookingId, Long ownerId, boolean approved) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено"));

        Item item = itemRepository.findById(booking.getItemId())
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        if (!item.getOwner().equals(ownerId)) {
            throw new ForbiddenException("Только владелец вещи может подтвердить бронирование");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Бронирование уже обработано");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        Booking saved = bookingRepository.save(booking);

        log.info("Booking {} was {}", bookingId, approved ? "approved" : "rejected");
        return bookingMapper.toBookingDto(saved);
    }

    @Override
    public BookingDto getById(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено"));

        Item item = itemRepository.findById(booking.getItemId())
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        if (!booking.getBookerId().equals(userId) && !item.getOwner().equals(userId)) {
            throw new ForbiddenException("Доступ запрещен");
        }

        return bookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getAllByUser(Long userId, String state) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        List<Booking> bookings = bookingRepository.findByBookerIdOrderByStartDesc(userId);
        return filterBookingsByState(bookings, state);
    }

    @Override
    public List<BookingDto> getAllByOwner(Long ownerId, String state) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        List<Item> ownerItems = itemRepository.findByOwnerOrderById(ownerId);
        List<Long> itemIds = ownerItems.stream().map(Item::getId).toList();

        if (itemIds.isEmpty()) {
            return List.of();
        }

        List<Booking> bookings = bookingRepository.findByItemIdInOrderByStartDesc(itemIds);
        return filterBookingsByState(bookings, state);
    }

    private List<BookingDto> filterBookingsByState(List<Booking> bookings, String state) {
        LocalDateTime now = LocalDateTime.now();

        return bookings.stream()
                .filter(booking -> {
                    return switch (state.toUpperCase()) {
                        case "CURRENT" -> booking.getStart().isBefore(now) && booking.getEnd().isAfter(now);
                        case "PAST" -> booking.getEnd().isBefore(now);
                        case "FUTURE" -> booking.getStart().isAfter(now);
                        case "WAITING" -> booking.getStatus() == BookingStatus.WAITING;
                        case "REJECTED" -> booking.getStatus() == BookingStatus.REJECTED;
                        default -> true;
                    };
                })
                .map(bookingMapper::toBookingDto)
                .toList();
    }
}