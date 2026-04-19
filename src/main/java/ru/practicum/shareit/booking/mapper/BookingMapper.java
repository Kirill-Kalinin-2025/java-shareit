package ru.practicum.shareit.booking.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.booking.dto.BookItemDto;
import ru.practicum.shareit.booking.dto.BookerDto;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.user.storage.UserRepository;

@Component
@RequiredArgsConstructor
public class BookingMapper {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public BookingDto toBookingDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setStart(booking.getStart());
        dto.setEnd(booking.getEnd());
        dto.setStatus(booking.getStatus());

        itemRepository.findById(booking.getItemId()).ifPresent(item -> {
            BookItemDto itemDto = new BookItemDto(item.getId(), item.getName());
            dto.setItem(itemDto);
        });

        userRepository.findById(booking.getBookerId()).ifPresent(user -> {
            BookerDto bookerDto = new BookerDto(user.getId());
            dto.setBooker(bookerDto);
        });

        return dto;
    }

    public Booking toBooking(BookingCreateDto bookingCreateDto) {
        if (bookingCreateDto == null) {
            return null;
        }
        Booking booking = new Booking();
        booking.setStart(bookingCreateDto.getStart());
        booking.setEnd(bookingCreateDto.getEnd());
        booking.setItemId(bookingCreateDto.getItemId());
        return booking;
    }

    public static BookingShortDto toBookingShortDto(Booking booking) {
        if (booking == null) {
            return null;
        }
        return new BookingShortDto(
                booking.getId(),
                booking.getBookerId()
        );
    }
}