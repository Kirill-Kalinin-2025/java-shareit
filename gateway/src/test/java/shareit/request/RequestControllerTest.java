package shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.ShareItGateway;
import ru.practicum.shareit.request.RequestClient;
import ru.practicum.shareit.request.RequestController;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RequestController.class)
@ContextConfiguration(classes = ShareItGateway.class)
class RequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequestClient requestClient;

    @Test
    void createRequest_shouldReturnRequest() throws Exception {
        ItemRequestCreateDto createDto = new ItemRequestCreateDto("Need a drill");

        when(requestClient.createRequest(eq(1L), any(ItemRequestCreateDto.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"description\":\"Need a drill\"}"));

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.description").value("Need a drill"));
    }

    @Test
    void createRequest_withEmptyDescription_shouldReturnBadRequest() throws Exception {
        String invalidJson = "{\"description\":\"\"}";

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserRequests_shouldReturnList() throws Exception {
        when(requestClient.getUserRequests(1L))
                .thenReturn(ResponseEntity.ok("[{\"id\":1,\"description\":\"Need a drill\"}]"));

        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getUserRequests_notFound_shouldReturn404() throws Exception {
        when(requestClient.getUserRequests(999L))
                .thenReturn(ResponseEntity.notFound().build());

        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllRequests_shouldReturnList() throws Exception {
        when(requestClient.getAllRequests(eq(1L), eq(0), eq(10)))
                .thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", 1L)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllRequests_withNegativeFrom_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", 1L)
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllRequests_withZeroSize_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", 1L)
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRequestById_shouldReturnRequest() throws Exception {
        when(requestClient.getRequest(eq(1L), eq(1L)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"description\":\"Need a drill\"}"));

        mockMvc.perform(get("/requests/{requestId}", 1L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getRequestById_notFound_shouldReturn404() throws Exception {
        when(requestClient.getRequest(eq(1L), eq(999L)))
                .thenReturn(ResponseEntity.notFound().build());

        mockMvc.perform(get("/requests/{requestId}", 999L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isNotFound());
    }
}