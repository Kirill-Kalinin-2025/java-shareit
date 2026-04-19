package ru.practicum.shareit.booking.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByBookerIdOrderByStartDesc(Long bookerId);

    @Query("SELECT b FROM Booking b WHERE b.bookerId = :bookerId AND b.status = :status ORDER BY b.start DESC")
    List<Booking> findByBookerIdAndStatusOrderByStartDesc(@Param("bookerId") Long bookerId,
                                                          @Param("status") BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.itemId IN :itemIds ORDER BY b.start DESC")
    List<Booking> findByItemIdInOrderByStartDesc(@Param("itemIds") List<Long> itemIds);

    @Query("SELECT b FROM Booking b WHERE b.itemId = :itemId AND b.start <= :now AND b.end >= :now")
    List<Booking> findCurrentBookings(@Param("itemId") Long itemId, @Param("now") LocalDateTime now);

    List<Booking> findByItemIdAndStatusOrderByStartAsc(Long itemId, BookingStatus status);

    Optional<Booking> findFirstByItemIdAndBookerIdAndEndBefore(Long itemId, Long bookerId, LocalDateTime end);

    boolean existsByItemIdAndBookerIdAndEndBefore(Long itemId, Long bookerId, LocalDateTime end);

    @Query("SELECT b FROM Booking b WHERE b.itemId = :itemId AND b.status = 'APPROVED' AND b.end < :now ORDER BY b.end DESC LIMIT 1")
    Optional<Booking> findLastBooking(@Param("itemId") Long itemId, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b WHERE b.itemId = :itemId AND b.status = 'APPROVED' AND b.start > :now ORDER BY b.start ASC LIMIT 1")
    Optional<Booking> findNextBooking(@Param("itemId") Long itemId, @Param("now") LocalDateTime now);

    // Методы для фильтрации по bookerId с учетом времени
    @Query("SELECT b FROM Booking b WHERE b.bookerId = :bookerId AND b.start <= :now AND b.end >= :now ORDER BY b.start DESC")
    List<Booking> findCurrentBookingsByBookerId(@Param("bookerId") Long bookerId, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b WHERE b.bookerId = :bookerId AND b.end < :now ORDER BY b.start DESC")
    List<Booking> findPastBookingsByBookerId(@Param("bookerId") Long bookerId, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b WHERE b.bookerId = :bookerId AND b.start > :now ORDER BY b.start DESC")
    List<Booking> findFutureBookingsByBookerId(@Param("bookerId") Long bookerId, @Param("now") LocalDateTime now);

    // Методы для фильтрации по ownerId - используем ItemRepository для получения itemIds
    @Query("SELECT b FROM Booking b WHERE b.itemId IN :itemIds ORDER BY b.start DESC")
    List<Booking> findByItemOwnerIdOrderByStartDesc(@Param("itemIds") List<Long> itemIds);

    @Query("SELECT b FROM Booking b WHERE b.itemId IN :itemIds AND b.status = :status ORDER BY b.start DESC")
    List<Booking> findByItemOwnerIdAndStatusOrderByStartDesc(@Param("itemIds") List<Long> itemIds,
                                                             @Param("status") BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.itemId IN :itemIds AND b.start <= :now AND b.end >= :now ORDER BY b.start DESC")
    List<Booking> findCurrentBookingsByOwnerId(@Param("itemIds") List<Long> itemIds, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b WHERE b.itemId IN :itemIds AND b.end < :now ORDER BY b.start DESC")
    List<Booking> findPastBookingsByOwnerId(@Param("itemIds") List<Long> itemIds, @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b WHERE b.itemId IN :itemIds AND b.start > :now ORDER BY b.start DESC")
    List<Booking> findFutureBookingsByOwnerId(@Param("itemIds") List<Long> itemIds, @Param("now") LocalDateTime now);
}