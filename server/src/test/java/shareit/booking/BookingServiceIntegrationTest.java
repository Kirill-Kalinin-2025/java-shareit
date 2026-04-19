package shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = ru.practicum.shareit.ShareItServer.class)
@ActiveProfiles("test")
@Transactional
class BookingServiceIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private ItemService itemService;

    private UserDto owner;
    private UserDto booker;
    private ItemDto item;
    private BookingCreateDto bookingCreateDto;

    @BeforeEach
    void setUp() {
        owner = userService.create(new UserDto(null, "Owner", "owner@example.com"));
        booker = userService.create(new UserDto(null, "Booker", "booker@example.com"));
        item = itemService.create(new ItemDto(null, "Drill", "Power drill", true, null), owner.getId());

        bookingCreateDto = new BookingCreateDto();
        bookingCreateDto.setItemId(item.getId());
        bookingCreateDto.setStart(LocalDateTime.now().plusDays(1));
        bookingCreateDto.setEnd(LocalDateTime.now().plusDays(2));
    }

    @Test
    void createBooking_shouldReturnBookingWithWaitingStatus() {
        BookingDto created = bookingService.create(booker.getId(), bookingCreateDto);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(created.getItem().getId()).isEqualTo(item.getId());
        assertThat(created.getBooker().getId()).isEqualTo(booker.getId());
    }

    @Test
    void createBooking_withUnavailableItem_shouldThrowValidationException() {
        itemService.update(item.getId(),
                new ItemDto(item.getId(), "Drill", "Power drill", false, null),
                owner.getId());

        assertThrows(ValidationException.class,
                () -> bookingService.create(booker.getId(), bookingCreateDto));
    }

    @Test
    void createBooking_byOwner_shouldThrowNotFoundException() {
        assertThrows(NotFoundException.class,
                () -> bookingService.create(owner.getId(), bookingCreateDto));
    }

    @Test
    void createBooking_withEndBeforeStart_shouldThrowValidationException() {
        bookingCreateDto.setEnd(LocalDateTime.now().plusDays(1));
        bookingCreateDto.setStart(LocalDateTime.now().plusDays(2));

        assertThrows(ValidationException.class,
                () -> bookingService.create(booker.getId(), bookingCreateDto));
    }

    @Test
    void createBooking_withStartEqualsEnd_shouldThrowValidationException() {
        LocalDateTime same = LocalDateTime.now().plusDays(1);
        bookingCreateDto.setStart(same);
        bookingCreateDto.setEnd(same);

        assertThrows(ValidationException.class,
                () -> bookingService.create(booker.getId(), bookingCreateDto));
    }

    @Test
    void approveBooking_shouldChangeStatusToApproved() {
        BookingDto created = bookingService.create(booker.getId(), bookingCreateDto);
        BookingDto approved = bookingService.approve(created.getId(), owner.getId(), true);

        assertThat(approved.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void approveBooking_shouldChangeStatusToRejected() {
        BookingDto created = bookingService.create(booker.getId(), bookingCreateDto);
        BookingDto rejected = bookingService.approve(created.getId(), owner.getId(), false);

        assertThat(rejected.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void approveBooking_byNonOwner_shouldThrowForbiddenException() {
        BookingDto created = bookingService.create(booker.getId(), bookingCreateDto);

        assertThrows(ForbiddenException.class,
                () -> bookingService.approve(created.getId(), booker.getId(), true));
    }

    @Test
    void approveBooking_alreadyProcessed_shouldThrowValidationException() {
        BookingDto created = bookingService.create(booker.getId(), bookingCreateDto);
        bookingService.approve(created.getId(), owner.getId(), true);

        assertThrows(ValidationException.class,
                () -> bookingService.approve(created.getId(), owner.getId(), true));
    }

    @Test
    void getBookingById_shouldReturnBooking() {
        BookingDto created = bookingService.create(booker.getId(), bookingCreateDto);
        BookingDto found = bookingService.getById(created.getId(), booker.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
    }

    @Test
    void getBookingById_byOwner_shouldReturnBooking() {
        BookingDto created = bookingService.create(booker.getId(), bookingCreateDto);
        BookingDto found = bookingService.getById(created.getId(), owner.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
    }

    @Test
    void getBookingById_byUnauthorizedUser_shouldThrowForbiddenException() {
        BookingDto created = bookingService.create(booker.getId(), bookingCreateDto);
        UserDto stranger = userService.create(new UserDto(null, "Stranger", "stranger@example.com"));

        assertThrows(ForbiddenException.class,
                () -> bookingService.getById(created.getId(), stranger.getId()));
    }

    @Test
    void getAllByUser_shouldReturnAllBookings() {
        bookingService.create(booker.getId(), bookingCreateDto);
        bookingService.create(booker.getId(), bookingCreateDto);

        List<BookingDto> bookings = bookingService.getAllByUser(booker.getId(), "ALL");

        assertThat(bookings).hasSize(2);
    }

    @Test
    void getAllByUser_withWaitingState_shouldReturnOnlyWaiting() {
        BookingDto created = bookingService.create(booker.getId(), bookingCreateDto);

        List<BookingDto> bookings = bookingService.getAllByUser(booker.getId(), "WAITING");

        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    void getAllByUser_withRejectedState_shouldReturnOnlyRejected() {
        BookingDto created = bookingService.create(booker.getId(), bookingCreateDto);
        bookingService.approve(created.getId(), owner.getId(), false);

        List<BookingDto> bookings = bookingService.getAllByUser(booker.getId(), "REJECTED");

        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void getAllByUser_withFutureState_shouldReturnFutureBookings() {
        bookingService.create(booker.getId(), bookingCreateDto);

        List<BookingDto> bookings = bookingService.getAllByUser(booker.getId(), "FUTURE");

        assertThat(bookings).hasSize(1);
    }

    @Test
    void getAllByUser_withPastState_shouldReturnPastBookings() {
        bookingCreateDto.setStart(LocalDateTime.now().minusDays(2));
        bookingCreateDto.setEnd(LocalDateTime.now().minusDays(1));
        bookingService.create(booker.getId(), bookingCreateDto);

        List<BookingDto> bookings = bookingService.getAllByUser(booker.getId(), "PAST");

        assertThat(bookings).hasSize(1);
    }

    @Test
    void getAllByUser_withCurrentState_shouldReturnCurrentBookings() {
        bookingCreateDto.setStart(LocalDateTime.now().minusDays(1));
        bookingCreateDto.setEnd(LocalDateTime.now().plusDays(1));
        bookingService.create(booker.getId(), bookingCreateDto);

        List<BookingDto> bookings = bookingService.getAllByUser(booker.getId(), "CURRENT");

        assertThat(bookings).hasSize(1);
    }

    @Test
    void getAllByOwner_shouldReturnOwnerBookings() {
        bookingService.create(booker.getId(), bookingCreateDto);

        List<BookingDto> bookings = bookingService.getAllByOwner(owner.getId(), "ALL");

        assertThat(bookings).hasSize(1);
    }

    @Test
    void getAllByOwner_withWaitingState_shouldReturnOnlyWaiting() {
        bookingService.create(booker.getId(), bookingCreateDto);

        List<BookingDto> bookings = bookingService.getAllByOwner(owner.getId(), "WAITING");

        assertThat(bookings).hasSize(1);
    }
}