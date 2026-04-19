package ru.practicum.shareit.booking;

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
import ru.practicum.shareit.booking.dto.BookItemRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookingController.class)
@ContextConfiguration(classes = ShareItGateway.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingClient bookingClient;

    @Test
    void createBooking_shouldReturnBooking() throws Exception {
        BookItemRequestDto requestDto = new BookItemRequestDto(
                1L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );

        when(bookingClient.bookItem(eq(1L), any(BookItemRequestDto.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"status\":\"WAITING\"}"));

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void createBooking_withoutStart_shouldReturnBadRequest() throws Exception {
        String invalidJson = "{\"itemId\":1,\"end\":\"2026-04-20T10:00:00\"}";

        when(bookingClient.bookItem(eq(1L), any(BookItemRequestDto.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Validation error"));

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBooking_withStartInPast_shouldReturnBadRequest() throws Exception {
        String invalidJson = "{\"itemId\":1,\"start\":\"2020-01-01T10:00:00\",\"end\":\"2026-04-20T10:00:00\"}";

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveBooking_shouldReturnApprovedBooking() throws Exception {
        when(bookingClient.approveBooking(eq(1L), eq(1L), eq(true)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"status\":\"APPROVED\"}"));

        mockMvc.perform(patch("/bookings/{bookingId}", 1L)
                        .param("approved", "true")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void getBookingById_shouldReturnBooking() throws Exception {
        when(bookingClient.getBooking(eq(1L), eq(1L)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"status\":\"WAITING\"}"));

        mockMvc.perform(get("/bookings/{bookingId}", 1L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getBookingById_notFound_shouldReturn404() throws Exception {
        when(bookingClient.getBooking(eq(1L), eq(999L)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not found"));

        mockMvc.perform(get("/bookings/{bookingId}", 999L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllByUser_shouldReturnList() throws Exception {
        when(bookingClient.getBookings(eq(1L), eq(BookingState.ALL), eq(0), eq(10)))
                .thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void getAllByUser_withInvalidState_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/bookings")
                        .param("state", "INVALID")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllByOwner_shouldReturnList() throws Exception {
        when(bookingClient.getBookingsByOwner(eq(1L), eq(BookingState.ALL)))
                .thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk());
    }
}