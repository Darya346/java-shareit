package ru.practicum.shareit.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.Status;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("select b from Booking b where b.booker.id = ?1 order by b.start desc")
    List<Booking> findUserBookings(Long bookerId);

    @Query("select b from Booking b where b.booker.id = ?1 and b.status = ?2 order by b.start desc")
    List<Booking> findUserStatus(Long bookerId, Status status);

    @Query("select b from Booking b where b.booker.id = ?1 and b.start <= ?2 and b.end >= ?2 order by b.start desc")
    List<Booking> findUserCurrent(Long bookerId, LocalDateTime now);

    @Query("select b from Booking b where b.booker.id = ?1 and b.end < ?2 order by b.start desc")
    List<Booking> findUserPast(Long bookerId, LocalDateTime now);

    @Query("select b from Booking b where b.booker.id = ?1 and b.start > ?2 order by b.start desc")
    List<Booking> findUserFuture(Long bookerId, LocalDateTime now);

    @Query("select b from Booking b where b.item.owner.id = ?1 order by b.start desc")
    List<Booking> findOwnerBookings(Long ownerId);

    @Query("select b from Booking b where b.item.owner.id = ?1 and b.status = ?2 order by b.start desc")
    List<Booking> findOwnerStatus(Long ownerId, Status status);

    @Query("select b from Booking b where b.item.owner.id = ?1 and b.start <= ?2 and b.end >= ?2 order by b.start desc")
    List<Booking> findOwnerCurrent(Long ownerId, LocalDateTime now);

    @Query("select b from Booking b where b.item.owner.id = ?1 and b.end < ?2 order by b.start desc")
    List<Booking> findOwnerPast(Long ownerId, LocalDateTime now);

    @Query("select b from Booking b where b.item.owner.id = ?1 and b.start > ?2 order by b.start desc")
    List<Booking> findOwnerFuture(Long ownerId, LocalDateTime now);

    @Query("select b from Booking b where b.item.id = ?1 order by b.start asc")
    List<Booking> findItemBookings(Long itemId);
}