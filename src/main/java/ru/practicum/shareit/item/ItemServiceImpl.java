package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.Status;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Transactional
    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Item item = ItemMapper.toItem(itemDto, owner);
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Transactional
    @Override
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        if (!item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Not an owner");
        }
        if (itemDto.getName() != null) item.setName(itemDto.getName());
        if (itemDto.getDescription() != null) item.setDescription(itemDto.getDescription());
        if (itemDto.getAvailable() != null) item.setAvailable(itemDto.getAvailable());
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDto getById(Long itemId, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        ItemDto dto = ItemMapper.toItemDto(item);

        dto.setComments(commentRepository.findAllByItemId(itemId).stream()
                .map(CommentMapper::toCommentDto).collect(Collectors.toList()));

        if (item.getOwner().getId().equals(userId)) {
            setBookings(dto, bookingRepository.findAllByItemIdOrderByStartAsc(itemId));
        }
        return dto;
    }

    @Override
    public List<ItemDto> getAllByOwnerId(Long ownerId) {
        List<Item> items = itemRepository.findAllByOwnerId(ownerId);
        return items.stream().map(item -> {
            ItemDto dto = ItemMapper.toItemDto(item);
            dto.setComments(commentRepository.findAllByItemId(item.getId()).stream()
                    .map(CommentMapper::toCommentDto).collect(Collectors.toList()));
            setBookings(dto, bookingRepository.findAllByItemIdOrderByStartAsc(item.getId()));
            return dto;
        }).sorted(Comparator.comparing(ItemDto::getId)).collect(Collectors.toList());
    }
    
    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return itemRepository.searchByText(text).stream()
                .map(ItemMapper::toItemDto).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public CommentDto createComment(Long userId, Long itemId, CommentDto commentDto) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new NotFoundException("Item not found"));

        List<Booking> userBookings = bookingRepository.findAllPastByBookerId(userId, LocalDateTime.now());
        boolean hasFinishedBooking = userBookings.stream()
                .anyMatch(b -> b.getItem().getId().equals(itemId) && b.getStatus() == Status.APPROVED);

        if (!hasFinishedBooking) {
            throw new ValidationException("No finished bookings for this item");
        }

        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        comment.setItem(item);
        comment.setAuthor(user);
        comment.setCreated(LocalDateTime.now());

        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    private void setBookings(ItemDto dto, List<Booking> bookings) {
        LocalDateTime now = LocalDateTime.now();
        Booking last = bookings.stream()
                .filter(b -> b.getStart().isBefore(now))
                .reduce((first, second) -> second).orElse(null);
        Booking next = bookings.stream()
                .filter(b -> b.getStart().isAfter(now))
                .findFirst().orElse(null);

        if (last != null) {
            dto.setLastBooking(new ItemDto.BookingShortDto(last.getId(), last.getBooker().getId()));
        }
        if (next != null) {
            dto.setNextBooking(new ItemDto.BookingShortDto(next.getId(), next.getBooker().getId()));
        }
    }
}