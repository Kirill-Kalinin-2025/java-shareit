package shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = ru.practicum.shareit.ShareItServer.class)
@ActiveProfiles("test")
@Transactional
class ItemRequestServiceIntegrationTest {

    @Autowired
    private ItemRequestService requestService;

    @Autowired
    private UserService userService;

    @Autowired
    private ItemService itemService;

    private UserDto requester;
    private UserDto responder;
    private ItemRequestCreateDto requestCreateDto;

    @BeforeEach
    void setUp() {
        requester = userService.create(new UserDto(null, "Requester", "requester@example.com"));
        responder = userService.create(new UserDto(null, "Responder", "responder@example.com"));
        requestCreateDto = new ItemRequestCreateDto("Need a drill");
    }

    @Test
    void createRequest_shouldReturnRequestWithId() {
        ItemRequestDto created = requestService.create(requester.getId(), requestCreateDto);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getDescription()).isEqualTo("Need a drill");
        assertThat(created.getCreated()).isNotNull();
    }

    @Test
    void createRequest_withInvalidUser_shouldThrowNotFoundException() {
        assertThrows(NotFoundException.class,
                () -> requestService.create(999L, requestCreateDto));
    }

    @Test
    void getUserRequests_shouldReturnRequesterRequests() {
        requestService.create(requester.getId(), requestCreateDto);
        requestService.create(requester.getId(), new ItemRequestCreateDto("Need a hammer"));

        List<ItemRequestDto> requests = requestService.getUserRequests(requester.getId());

        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getDescription()).isEqualTo("Need a hammer");
        assertThat(requests.get(1).getDescription()).isEqualTo("Need a drill");
    }

    @Test
    void getUserRequests_shouldReturnEmptyList() {
        List<ItemRequestDto> requests = requestService.getUserRequests(requester.getId());

        assertThat(requests).isEmpty();
    }

    @Test
    void getAllRequests_shouldReturnOtherUsersRequests() {
        requestService.create(requester.getId(), requestCreateDto);
        requestService.create(requester.getId(), new ItemRequestCreateDto("Need a hammer"));

        List<ItemRequestDto> requests = requestService.getAllRequests(responder.getId(), 0, 10);

        assertThat(requests).hasSize(2);
    }

    @Test
    void getAllRequests_withPagination_shouldReturnLimitedResults() {
        requestService.create(requester.getId(), new ItemRequestCreateDto("Request 1"));
        requestService.create(requester.getId(), new ItemRequestCreateDto("Request 2"));
        requestService.create(requester.getId(), new ItemRequestCreateDto("Request 3"));

        List<ItemRequestDto> requests = requestService.getAllRequests(responder.getId(), 0, 2);

        assertThat(requests).hasSize(2);
    }

    @Test
    void getAllRequests_shouldNotReturnOwnRequests() {
        requestService.create(responder.getId(), new ItemRequestCreateDto("My request"));
        requestService.create(requester.getId(), requestCreateDto);

        List<ItemRequestDto> requests = requestService.getAllRequests(responder.getId(), 0, 10);

        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getDescription()).isEqualTo("Need a drill");
    }

    @Test
    void getRequestById_shouldReturnRequestWithItems() {
        ItemRequestDto created = requestService.create(requester.getId(), requestCreateDto);
        itemService.create(new ItemDto(null, "Drill", "Power drill", true, created.getId()), responder.getId());

        ItemRequestDto found = requestService.getById(responder.getId(), created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getItems()).hasSize(1);
        assertThat(found.getItems().get(0).getName()).isEqualTo("Drill");
    }

    @Test
    void getRequestById_withInvalidId_shouldThrowNotFoundException() {
        assertThrows(NotFoundException.class,
                () -> requestService.getById(requester.getId(), 999L));
    }

    @Test
    void getRequestById_withInvalidUser_shouldThrowNotFoundException() {
        ItemRequestDto created = requestService.create(requester.getId(), requestCreateDto);

        assertThrows(NotFoundException.class,
                () -> requestService.getById(999L, created.getId()));
    }
}