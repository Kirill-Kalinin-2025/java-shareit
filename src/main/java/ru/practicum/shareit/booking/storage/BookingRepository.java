package ru.practicum.shareit.booking.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByBookerIdOrderByStartDesc(Long bookerId);

    List<Booking> findByItemIdInOrderByStartDesc(List<Long> itemIds);

    @Query("SELECT b FROM Booking b WHERE b.itemId = ?1 AND b.start <= ?2 AND b.end >= ?2")
    List<Booking> findCurrentBookings(Long itemId, LocalDateTime now);

    List<Booking> findByItemIdAndStatusOrderByStartAsc(Long itemId, BookingStatus status);

    Optional<Booking> findFirstByItemIdAndBookerIdAndEndBefore(Long itemId, Long bookerId, LocalDateTime end);

    boolean existsByItemIdAndBookerIdAndEndBefore(Long itemId, Long bookerId, LocalDateTime end);

    @Query("SELECT b FROM Booking b WHERE b.itemId = ?1 AND b.status = 'APPROVED' AND b.end < ?2 ORDER BY b.end DESC")
    Optional<Booking> findLastBooking(Long itemId, LocalDateTime now);

    @Query("SELECT b FROM Booking b WHERE b.itemId = ?1 AND b.status = 'APPROVED' AND b.start > ?2 ORDER BY b.start ASC")
    Optional<Booking> findNextBooking(Long itemId, LocalDateTime now);
}