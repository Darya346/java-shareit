package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.Status;
import ru.practicum.shareit.exception.ForbiddenException;
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
        if (!item.getAvailable()) throw new ValidationException("Вещь недоступна");
        if (item.getOwner().getId().equals(userId)) throw new NotFoundException("Владелец не может бронировать");
        if (bookingDto.getEnd().isBefore(bookingDto.getStart()) || bookingDto.getEnd().equals(bookingDto.getStart())) {
            throw new ValidationException("Неверные даты");
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
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Not an owner");
        }

        if (booking.getStatus() == Status.APPROVED) {
            throw new ValidationException("Already approved");
        }

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
    public List<BookingResponseDto> getAllByBooker(Long userId, String state, Integer from, Integer size) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Pageable page = PageRequest.of(from / size, size);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> list = switch (state.toUpperCase()) {
            case "ALL" -> bookingRepository.findUserBookings(userId, page);
            case "CURRENT" -> bookingRepository.findUserCurrent(userId, now, page);
            case "PAST" -> bookingRepository.findUserPast(userId, now, page);
            case "FUTURE" -> bookingRepository.findUserFuture(userId, now, page);
            case "WAITING" -> bookingRepository.findUserStatus(userId, Status.WAITING, page);
            case "REJECTED" -> bookingRepository.findUserStatus(userId, Status.REJECTED, page);
            default -> throw new ValidationException("Unknown state: " + state);
        };
        return list.stream().map(BookingMapper::toBookingResponse).collect(Collectors.toList());
    }

    @Override
    public List<BookingResponseDto> getAllByOwner(Long userId, String state, Integer from, Integer size) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Pageable page = PageRequest.of(from / size, size);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> list = switch (state.toUpperCase()) {
            case "ALL" -> bookingRepository.findOwnerBookings(userId, page);
            case "CURRENT" -> bookingRepository.findOwnerCurrent(userId, now, page);
            case "PAST" -> bookingRepository.findOwnerPast(userId, now, page);
            case "FUTURE" -> bookingRepository.findOwnerFuture(userId, now, page);
            case "WAITING" -> bookingRepository.findOwnerStatus(userId, Status.WAITING, page);
            case "REJECTED" -> bookingRepository.findOwnerStatus(userId, Status.REJECTED, page);
            default -> throw new ValidationException("Unknown state: " + state);
        };
        return list.stream().map(BookingMapper::toBookingResponse).collect(Collectors.toList());
    }
}