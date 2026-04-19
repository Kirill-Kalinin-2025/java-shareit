package shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemDtoWithBookings;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = ru.practicum.shareit.ShareItServer.class)
@ActiveProfiles("test")
@Transactional
class ItemServiceIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ItemRequestService requestService;

    private UserDto owner;
    private UserDto booker;
    private ItemDto itemDto;

    @BeforeEach
    void setUp() {
        owner = userService.create(new UserDto(null, "Owner", "owner@example.com"));
        booker = userService.create(new UserDto(null, "Booker", "booker@example.com"));
        itemDto = new ItemDto(null, "Drill", "Power drill", true, null);
    }

    @Test
    void createItem_shouldReturnItemWithId() {
        ItemDto created = itemService.create(itemDto, owner.getId());

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Drill");
        assertThat(created.getDescription()).isEqualTo("Power drill");
        assertThat(created.getAvailable()).isTrue();
    }

    @Test
    void createItem_withRequestId_shouldCreateItemLinkedToRequest() {
        ItemRequestDto request = requestService.create(booker.getId(), new ItemRequestCreateDto("Need a drill"));
        ItemDto itemWithRequest = new ItemDto(null, "Drill", "Power drill", true, request.getId());

        ItemDto created = itemService.create(itemWithRequest, owner.getId());

        assertThat(created.getRequestId()).isEqualTo(request.getId());
    }

    @Test
    void createItem_withInvalidRequestId_shouldThrowNotFoundException() {
        ItemDto itemWithInvalidRequest = new ItemDto(null, "Drill", "Power drill", true, 999L);

        assertThrows(NotFoundException.class, () -> itemService.create(itemWithInvalidRequest, owner.getId()));
    }

    @Test
    void getItemById_shouldReturnItem() {
        ItemDto created = itemService.create(itemDto, owner.getId());
        ItemDtoWithBookings found = itemService.getById(created.getId(), owner.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo("Drill");
    }

    @Test
    void getItemById_withInvalidId_shouldThrowNotFoundException() {
        assertThrows(NotFoundException.class, () -> itemService.getById(999L, owner.getId()));
    }

    @Test
    void getByOwnerId_shouldReturnOwnerItems() {
        itemService.create(itemDto, owner.getId());
        itemService.create(new ItemDto(null, "Hammer", "Big hammer", true, null), owner.getId());

        List<ItemDtoWithBookings> items = itemService.getByOwnerId(owner.getId());

        assertThat(items).hasSize(2);
    }

    @Test
    void updateItem_shouldUpdateFields() {
        ItemDto created = itemService.create(itemDto, owner.getId());
        ItemDto update = new ItemDto(null, "Updated Drill", "Updated description", false, null);

        ItemDto updated = itemService.update(created.getId(), update, owner.getId());

        assertThat(updated.getName()).isEqualTo("Updated Drill");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getAvailable()).isFalse();
    }

    @Test
    void updateItem_byNonOwner_shouldThrowForbiddenException() {
        ItemDto created = itemService.create(itemDto, owner.getId());
        ItemDto update = new ItemDto(null, "Updated", null, null, null);

        assertThrows(ForbiddenException.class, () -> itemService.update(created.getId(), update, booker.getId()));
    }

    @Test
    void search_shouldReturnAvailableItems() {
        itemService.create(itemDto, owner.getId());
        itemService.create(new ItemDto(null, "Hammer", "Big hammer", false, null), owner.getId());

        List<ItemDto> found = itemService.search("drill");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Drill");
    }

    @Test
    void search_withEmptyText_shouldReturnEmptyList() {
        List<ItemDto> found = itemService.search("");

        assertThat(found).isEmpty();
    }

    @Test
    void addComment_shouldCreateComment() {
        ItemDto created = itemService.create(itemDto, owner.getId());

        BookingCreateDto bookingDto = new BookingCreateDto();
        bookingDto.setItemId(created.getId());
        // Устанавливаем даты в прошлом, чтобы бронирование уже завершилось
        bookingDto.setStart(LocalDateTime.now().minusSeconds(2));
        bookingDto.setEnd(LocalDateTime.now().minusSeconds(1));

        BookingDto booking = bookingService.create(booker.getId(), bookingDto);
        bookingService.approve(booking.getId(), owner.getId(), true);

        CommentDto commentDto = new CommentDto(null, "Great drill!", null, null);
        CommentDto added = itemService.addComment(created.getId(), booker.getId(), commentDto);

        assertThat(added.getId()).isNotNull();
        assertThat(added.getText()).isEqualTo("Great drill!");
        assertThat(added.getAuthorName()).isEqualTo(booker.getName());
    }

    @Test
    void addComment_withoutBooking_shouldThrowValidationException() {
        ItemDto created = itemService.create(itemDto, owner.getId());
        CommentDto commentDto = new CommentDto(null, "Great!", null, null);

        assertThrows(ValidationException.class, () -> itemService.addComment(created.getId(), booker.getId(), commentDto));
    }
}