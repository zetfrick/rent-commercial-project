package com.example.rentapp.service;

import com.example.rentapp.client.CatalogClient;
import com.example.rentapp.dto.BookingDto;
import com.example.rentapp.dto.PremiseDto;
import com.example.rentapp.entity.Booking;
import com.example.rentapp.entity.User;
import com.example.rentapp.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CatalogClient catalogClient;

    @Autowired
    private NotificationService notificationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Получить занятые даты для помещения (только APPROVED) - для проверки доступности
    public List<LocalDate> getBookedDates(Long premiseId) {
        List<Booking> bookings = bookingRepository.findByPremiseIdAndStatusIn(premiseId, List.of("APPROVED"));
        List<LocalDate> bookedDates = new ArrayList<>();

        for (Booking booking : bookings) {
            LocalDate current = booking.getStartDate();
            while (!current.isAfter(booking.getEndDate())) {
                bookedDates.add(current);
                current = current.plusDays(1);
            }
        }
        return bookedDates;
    }

    // Получить занятые диапазоны дат для помещения (только APPROVED) - для отображения пользователям
    public List<Map<String, Object>> getBookedDateRanges(Long premiseId) {
        List<Booking> bookings = bookingRepository.findByPremiseIdAndStatusIn(premiseId, List.of("APPROVED"));
        List<Map<String, Object>> ranges = new ArrayList<>();

        for (Booking booking : bookings) {
            Map<String, Object> range = new HashMap<>();
            range.put("start", booking.getStartDate());
            range.put("end", booking.getEndDate());
            ranges.add(range);
        }
        return ranges;
    }

    // НОВЫЙ МЕТОД: получить диапазоны ожидающих дат (только PENDING) - для отображения владельцу
    public List<Map<String, Object>> getPendingDateRanges(Long premiseId) {
        List<Booking> bookings = bookingRepository.findByPremiseIdAndStatusIn(premiseId, List.of("PENDING"));
        List<Map<String, Object>> ranges = new ArrayList<>();

        for (Booking booking : bookings) {
            Map<String, Object> range = new HashMap<>();
            range.put("start", booking.getStartDate());
            range.put("end", booking.getEndDate());
            ranges.add(range);
        }
        return ranges;
    }

    // НОВЫЙ МЕТОД: получить ожидающие запросы с деталями (для отображения в карточке помещения)
    public List<Map<String, Object>> getPendingRequestsWithDetails(Long premiseId) {
        List<Booking> bookings = bookingRepository.findByPremiseIdAndStatusIn(premiseId, List.of("PENDING"));
        List<Map<String, Object>> result = new ArrayList<>();

        for (Booking booking : bookings) {
            Map<String, Object> item = new HashMap<>();
            item.put("start", booking.getStartDate());
            item.put("end", booking.getEndDate());
            item.put("renterId", booking.getRenterId());
            item.put("renterName", booking.getRenterName());
            item.put("bookingId", booking.getId());
            result.add(item);
        }
        return result;
    }

    // Проверить, что даты находятся в пределах доступного периода помещения
    public boolean areDatesWithinAvailablePeriod(Long premiseId, LocalDate startDate, LocalDate endDate) {
        PremiseDto premise = catalogClient.getPremiseById(premiseId);
        if (premise == null) {
            return false;
        }
        return !startDate.isBefore(premise.getAvailableFrom()) && !endDate.isAfter(premise.getAvailableTo());
    }

    // Получить все одобренные бронирования для помещения с деталями
    public List<BookingDto> getApprovedBookingsWithDetails(Long premiseId) {
        List<Booking> bookings = bookingRepository.findByPremiseIdAndStatusIn(premiseId, List.of("APPROVED"));
        return bookings.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // Получить все PENDING запросы для владельца
    public List<BookingDto> getPendingRequestsForOwner(Long ownerId) {
        List<Booking> bookings = bookingRepository.findByOwnerIdAndStatusOrderByCreatedAtDesc(ownerId, "PENDING");
        return bookings.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // НОВЫЙ МЕТОД: получить PENDING запросы для владельца по конкретному помещению
    public List<BookingDto> getPendingRequestsForOwnerByPremise(Long ownerId, Long premiseId) {
        List<Booking> bookings = bookingRepository.findByOwnerIdAndStatusOrderByCreatedAtDesc(ownerId, "PENDING");
        return bookings.stream()
                .filter(b -> b.getPremiseId().equals(premiseId))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Получить все запросы арендатора
    public List<BookingDto> getRequestsForRenter(Long renterId) {
        return bookingRepository.findByRenterIdOrderByCreatedAtDesc(renterId)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // Получить все бронирования (разные статусы) для помещения - для владельца
    public List<BookingDto> getBookingsWithDetails(Long premiseId) {
        List<Booking> bookings = bookingRepository.findByPremiseId(premiseId);
        return bookings.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // Получить все активные бронирования (без CANCELLED) для помещения
    public List<BookingDto> getActiveBookingsWithDetails(Long premiseId) {
        List<Booking> bookings = bookingRepository.findByPremiseId(premiseId).stream()
                .filter(b -> !"CANCELLED".equals(b.getStatus()))
                .collect(Collectors.toList());
        return bookings.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // Получить все бронирования пользователя (как владельца) - без CANCELLED
    public List<BookingDto> getActiveBookingsByOwner(Long ownerId) {
        return bookingRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .filter(b -> !"CANCELLED".equals(b.getStatus()))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Получить все бронирования пользователя (как арендатора) - без CANCELLED
    public List<BookingDto> getActiveBookingsByRenter(Long renterId) {
        return bookingRepository.findByRenterIdOrderByCreatedAtDesc(renterId).stream()
                .filter(b -> !"CANCELLED".equals(b.getStatus()))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Получить все бронирования пользователя (как владельца) - ВСЕ (включая CANCELLED)
    public List<BookingDto> getAllBookingsByOwner(Long ownerId) {
        return bookingRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // Проверить, свободны ли даты (только APPROVED бронирования)
    public boolean areDatesAvailable(Long premiseId, LocalDate startDate, LocalDate endDate) {
        List<Booking> overlapping = bookingRepository.findOverlappingApprovedBookings(premiseId, startDate, endDate);
        return overlapping.isEmpty();
    }

    // Проверить, есть ли PENDING запрос на даты
    public boolean hasPendingRequest(Long premiseId, LocalDate startDate, LocalDate endDate) {
        List<Booking> pendingOverlap = bookingRepository.findByPremiseIdAndStatus(premiseId, "PENDING");
        for (Booking pending : pendingOverlap) {
            if (!(pending.getEndDate().isBefore(startDate) || pending.getStartDate().isAfter(endDate))) {
                return true;
            }
        }
        return false;
    }

    // НОВЫЙ МЕТОД: проверить наличие PENDING запроса для конкретного чата
    public boolean hasPendingRequestForChat(Long premiseId, Long currentUserId, Long otherUserId) {
        if (premiseId == null) {
            return false;
        }

        List<Booking> pendingRequests = bookingRepository.findByPremiseIdAndStatus(premiseId, "PENDING");

        for (Booking request : pendingRequests) {
            if (request.getOwnerId().equals(currentUserId) && request.getRenterId().equals(otherUserId)) {
                if (request.getPremiseId().equals(premiseId)) {
                    return true;
                }
            }
            if (request.getRenterId().equals(currentUserId) && request.getOwnerId().equals(otherUserId)) {
                if (request.getPremiseId().equals(premiseId)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Создать запрос на аренду (для арендатора)
    @Transactional
    public BookingDto createBookingRequest(Long premiseId, Long renterId, Long ownerId,
                                           LocalDate startDate, LocalDate endDate) {
        PremiseDto premise = catalogClient.getPremiseById(premiseId);
        if (premise == null) {
            throw new RuntimeException("Помещение не найдено");
        }

        if (startDate.isBefore(premise.getAvailableFrom()) || endDate.isAfter(premise.getAvailableTo())) {
            throw new RuntimeException("Выбранные даты выходят за пределы доступного периода аренды");
        }

        if (!areDatesAvailable(premiseId, startDate, endDate)) {
            throw new RuntimeException("Выбранные даты уже заняты");
        }

        if (hasPendingRequest(premiseId, startDate, endDate)) {
            throw new RuntimeException("На эти даты уже есть ожидающий запрос");
        }

        User renter = userService.findById(renterId)
                .orElseThrow(() -> new RuntimeException("Арендатор не найден"));
        User owner = userService.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Владелец не найден"));

        Booking booking = new Booking(
                premiseId, renterId, ownerId, startDate, endDate,
                renter.getLogin(), owner.getLogin()
        );

        Booking saved = bookingRepository.save(booking);

        String dateRange = startDate.format(DATE_FORMATTER) + " - " + endDate.format(DATE_FORMATTER);
        String chatLink = "/chats/with/" + renter.getLogin() + "/premise/" + premiseId;

        notificationService.createNotification(
                ownerId,
                "BOOKING_REQUEST",
                saved.getId(),
                renterId,
                renter.getLogin(),
                "Запрос на аренду с " + dateRange,
                chatLink
        );

        return convertToDto(saved);
    }

    // Создать бронирование напрямую (для владельца) - сразу APPROVED
    @Transactional
    public BookingDto createBookingDirect(Long premiseId, Long renterId, Long ownerId,
                                          LocalDate startDate, LocalDate endDate) {
        PremiseDto premise = catalogClient.getPremiseById(premiseId);
        if (premise == null) {
            throw new RuntimeException("Помещение не найдено");
        }

        if (startDate.isBefore(premise.getAvailableFrom()) || endDate.isAfter(premise.getAvailableTo())) {
            throw new RuntimeException("Выбранные даты выходят за пределы доступного периода аренды");
        }

        if (!areDatesAvailable(premiseId, startDate, endDate)) {
            throw new RuntimeException("Выбранные даты уже заняты");
        }

        if (hasPendingRequest(premiseId, startDate, endDate)) {
            throw new RuntimeException("На эти даты уже есть ожидающий запрос");
        }

        User renter = userService.findById(renterId)
                .orElseThrow(() -> new RuntimeException("Арендатор не найден"));
        User owner = userService.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Владелец не найден"));

        Booking booking = new Booking(
                premiseId, renterId, ownerId, startDate, endDate,
                renter.getLogin(), owner.getLogin()
        );
        booking.setStatus("APPROVED");

        Booking saved = bookingRepository.save(booking);

        String dateRange = startDate.format(DATE_FORMATTER) + " - " + endDate.format(DATE_FORMATTER);
        String chatLink = "/chats/with/" + owner.getLogin() + "/premise/" + premiseId;

        notificationService.createNotification(
                renterId,
                "BOOKING_APPROVED",
                saved.getId(),
                ownerId,
                owner.getLogin(),
                "Аренда подтверждена на " + dateRange,
                chatLink
        );

        return convertToDto(saved);
    }

    // Одобрить запрос (владелец)
    @Transactional
    public void approveBooking(Long bookingId, Long ownerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Запрос не найден"));

        if (!booking.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Вы не можете одобрить этот запрос");
        }

        if (!"PENDING".equals(booking.getStatus())) {
            throw new RuntimeException("Запрос уже обработан");
        }

        PremiseDto premise = catalogClient.getPremiseById(booking.getPremiseId());
        if (premise == null) {
            throw new RuntimeException("Помещение не найдено");
        }

        if (booking.getStartDate().isBefore(premise.getAvailableFrom()) ||
                booking.getEndDate().isAfter(premise.getAvailableTo())) {
            throw new RuntimeException("Даты выходят за пределы доступного периода аренды");
        }

        if (!areDatesAvailable(booking.getPremiseId(), booking.getStartDate(), booking.getEndDate())) {
            throw new RuntimeException("Даты уже заняты другим бронированием");
        }

        bookingRepository.updateStatus(bookingId, "APPROVED");

        User owner = userService.findById(ownerId).orElse(null);
        String dateRange = booking.getStartDate().format(DATE_FORMATTER) + " - " + booking.getEndDate().format(DATE_FORMATTER);
        String chatLink = "/chats/with/" + (owner != null ? owner.getLogin() : "Владелец") + "/premise/" + booking.getPremiseId();

        notificationService.createNotification(
                booking.getRenterId(),
                "BOOKING_APPROVED",
                bookingId,
                ownerId,
                owner != null ? owner.getLogin() : "Владелец",
                "Аренда подтверждена на " + dateRange,
                chatLink
        );
    }

    // Отказать в запросе (владелец)
    @Transactional
    public void rejectBooking(Long bookingId, Long ownerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Запрос не найден"));

        if (!booking.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Вы не можете отклонить этот запрос");
        }

        if (!"PENDING".equals(booking.getStatus())) {
            throw new RuntimeException("Запрос уже обработан");
        }

        bookingRepository.updateStatus(bookingId, "REJECTED");

        User owner = userService.findById(ownerId).orElse(null);
        String chatLink = "/chats/with/" + (owner != null ? owner.getLogin() : "Владелец") + "/premise/" + booking.getPremiseId();

        notificationService.createNotification(
                booking.getRenterId(),
                "BOOKING_REJECTED",
                bookingId,
                ownerId,
                owner != null ? owner.getLogin() : "Владелец",
                null,
                chatLink
        );
    }

    // Отменить бронирование (владелец) - можно отменить только PENDING и APPROVED
    @Transactional
    public void cancelBookingByOwner(Long bookingId, Long ownerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Бронирование не найдено"));

        if (!booking.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Вы не можете отменить это бронирование");
        }

        if (!"PENDING".equals(booking.getStatus()) && !"APPROVED".equals(booking.getStatus())) {
            throw new RuntimeException("Можно отменить только ожидающие или подтверждённые бронирования");
        }

        bookingRepository.updateStatus(bookingId, "CANCELLED");

        User owner = userService.findById(ownerId).orElse(null);
        String dateRange = booking.getStartDate().format(DATE_FORMATTER) + " - " + booking.getEndDate().format(DATE_FORMATTER);
        String premiseLink = "/premise/" + booking.getPremiseId();

        notificationService.createNotification(
                booking.getRenterId(),
                "BOOKING_CANCELLED_BY_OWNER",
                bookingId,
                ownerId,
                owner != null ? owner.getLogin() : "Владелец",
                "Аренда на " + dateRange + " была отменена владельцем",
                premiseLink
        );
    }

    // Отменить бронирование (арендатор) - можно отменить только PENDING и APPROVED
    @Transactional
    public void cancelBookingByRenter(Long bookingId, Long renterId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Бронирование не найдено"));

        if (!booking.getRenterId().equals(renterId)) {
            throw new RuntimeException("Вы не можете отменить это бронирование");
        }

        if (!"PENDING".equals(booking.getStatus()) && !"APPROVED".equals(booking.getStatus())) {
            throw new RuntimeException("Можно отменить только ожидающие или подтверждённые бронирования");
        }

        bookingRepository.updateStatus(bookingId, "CANCELLED");

        User renter = userService.findById(renterId).orElse(null);
        String dateRange = booking.getStartDate().format(DATE_FORMATTER) + " - " + booking.getEndDate().format(DATE_FORMATTER);
        String premiseLink = "/premise/" + booking.getPremiseId();

        notificationService.createNotification(
                booking.getOwnerId(),
                "BOOKING_CANCELLED_BY_RENTER",
                bookingId,
                renterId,
                renter != null ? renter.getLogin() : "Арендатор",
                "Аренда на " + dateRange + " была отменена арендатором",
                premiseLink
        );
    }

    private BookingDto convertToDto(Booking booking) {
        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setPremiseId(booking.getPremiseId());
        dto.setRenterId(booking.getRenterId());
        dto.setOwnerId(booking.getOwnerId());
        dto.setStartDate(booking.getStartDate());
        dto.setEndDate(booking.getEndDate());
        dto.setRenterName(booking.getRenterName());
        dto.setOwnerName(booking.getOwnerName());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setStatus(booking.getStatus());
        return dto;
    }
}