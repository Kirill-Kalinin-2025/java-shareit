package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import ru.practicum.shareit.ShareItGateway;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ItemController.class)
@ContextConfiguration(classes = ShareItGateway.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemClient itemClient;

    @Test
    void createItem_shouldReturnItem() throws Exception {
        ItemDto itemDto = new ItemDto(null, "Drill", "Power drill", true, null);

        when(itemClient.createItem(eq(1L), any(ItemDto.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"name\":\"Drill\",\"description\":\"Power drill\",\"available\":true,\"requestId\":null}"));

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Drill"));
    }

    @Test
    void createItem_withEmptyName_shouldReturnBadRequest() throws Exception {
        String invalidJson = "{\"name\":\"\",\"description\":\"desc\",\"available\":true}";

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createItem_withoutAvailable_shouldReturnBadRequest() throws Exception {
        String invalidJson = "{\"name\":\"Drill\",\"description\":\"desc\"}";

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getItemById_shouldReturnItem() throws Exception {
        when(itemClient.getItem(eq(1L), eq(1L)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"name\":\"Drill\",\"description\":\"Power drill\",\"available\":true}"));

        mockMvc.perform(get("/items/{id}", 1L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Drill"));
    }

    @Test
    void getItemById_notFound_shouldReturn404() throws Exception {
        when(itemClient.getItem(eq(1L), eq(999L)))  // ← порядок: userId, itemId
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not found"));

        mockMvc.perform(get("/items/{id}", 999L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByOwner_shouldReturnItems() throws Exception {
        when(itemClient.getUserItems(1L))
                .thenReturn(ResponseEntity.ok("[{\"id\":1,\"name\":\"Drill\",\"description\":\"Power drill\",\"available\":true}]"));

        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void search_shouldReturnItems() throws Exception {
        when(itemClient.searchItems(eq(1L), eq("drill")))
                .thenReturn(ResponseEntity.ok("[{\"id\":1,\"name\":\"Drill\",\"description\":\"Power drill\",\"available\":true}]"));

        mockMvc.perform(get("/items/search")
                        .param("text", "drill")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updateItem_shouldReturnUpdatedItem() throws Exception {
        ItemDto update = new ItemDto(null, "Updated", "Updated desc", false, null);

        when(itemClient.updateItem(eq(1L), eq(1L), any(ItemDto.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"name\":\"Updated\",\"description\":\"Updated desc\",\"available\":false,\"requestId\":null}"));

        mockMvc.perform(patch("/items/{itemId}", 1L)
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void addComment_shouldReturnComment() throws Exception {
        CommentDto commentDto = new CommentDto(null, "Great!", null, null);

        when(itemClient.addComment(eq(1L), eq(1L), any(CommentDto.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"text\":\"Great!\",\"authorName\":\"Booker\"}"));

        mockMvc.perform(post("/items/{itemId}/comment", 1L)
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.text").value("Great!"));
    }

    @Test
    void addComment_withEmptyText_shouldReturnBadRequest() throws Exception {
        String invalidJson = "{\"text\":\"\"}";

        mockMvc.perform(post("/items/{itemId}/comment", 1L)
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}