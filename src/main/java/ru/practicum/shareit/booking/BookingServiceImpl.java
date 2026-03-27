package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.Status;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Transactional
    @Override
    public BookingResponseDto create(Long userId, BookingDto bookingDto) {
        User booker = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Item item = itemRepository.findById(bookingDto.getItemId()).orElseThrow(() -> new NotFoundException("Item not found"));
        if (!item.getAvailable()) throw new ValidationException("Not available");
        if (item.getOwner().getId().equals(userId)) throw new NotFoundException("Owner cannot book");
        if (bookingDto.getEnd().isBefore(bookingDto.getStart()) || bookingDto.getEnd().equals(bookingDto.getStart())) {
            throw new ValidationException("Wrong dates");
        }
        Booking booking = new Booking();
        booking.setStart(bookingDto.getStart());
        booking.setEnd(bookingDto.getEnd());
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(Status.WAITING);
        return BookingMapper.toBookingResponse(bookingRepository.save(booking));
    }

    @Transactional
    @Override
    public BookingResponseDto approve(Long userId, Long bookingId, boolean approved) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Not found"));
        if (!booking.getItem().getOwner().getId().equals(userId)) throw new ValidationException("Not an owner");
        if (booking.getStatus() != Status.WAITING) throw new ValidationException("Already changed");
        booking.setStatus(approved ? Status.APPROVED : Status.REJECTED);
        return BookingMapper.toBookingResponse(bookingRepository.save(booking));
    }

    @Override
    public BookingResponseDto getById(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Not found"));
        if (!booking.getBooker().getId().equals(userId) && !booking.getItem().getOwner().getId().equals(userId)) {
            throw new NotFoundException("No access");
        }
        return BookingMapper.toBookingResponse(booking);
    }

    @Override
    public List<BookingResponseDto> getAllByBooker(Long userId, String state) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;
        switch (state.toUpperCase()) {
            case "ALL": bookings = bookingRepository.findUserBookings(userId); break;
            case "CURRENT": bookings = bookingRepository.findUserCurrent(userId, now); break;
            case "PAST": bookings = bookingRepository.findUserPast(userId, now); break;
            case "FUTURE": bookings = bookingRepository.findUserFuture(userId, now); break;
            case "WAITING": bookings = bookingRepository.findUserStatus(userId, Status.WAITING); break;
            case "REJECTED": bookings = bookingRepository.findUserStatus(userId, Status.REJECTED); break;
            default: throw new ValidationException("Unknown state: " + state);
        }
        return bookings.stream().map(BookingMapper::toBookingResponse).collect(Collectors.toList());
    }

    @Override
    public List<BookingResponseDto> getAllByOwner(Long userId, String state) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;
        switch (state.toUpperCase()) {
            case "ALL": bookings = bookingRepository.findOwnerBookings(userId); break;
            case "CURRENT": bookings = bookingRepository.findOwnerCurrent(userId, now); break;
            case "PAST": bookings = bookingRepository.findOwnerPast(userId, now); break;
            case "FUTURE": bookings = bookingRepository.findOwnerFuture(userId, now); break;
            case "WAITING": bookings = bookingRepository.findOwnerStatus(userId, Status.WAITING); break;
            case "REJECTED": bookings = bookingRepository.findOwnerStatus(userId, Status.REJECTED); break;
            default: throw new ValidationException("Unknown state: " + state);
        }
        return bookings.stream().map(BookingMapper::toBookingResponse).collect(Collectors.toList());
    }
}