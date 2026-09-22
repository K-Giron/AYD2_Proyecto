package com.megastore.sgologic.notifications;

import com.megastore.sgologic.shared.http.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** NOT-01: solicitudes de notificación no bloqueantes (CUN-11). */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request, HttpServletRequest httpRequest) {
        UUID correlationId = UUID.fromString(
                httpRequest.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME).toString());
        NotificationRequest created = notificationService.requestNotification(
                request.eventType(), request.channel(), request.recipient(), request.sourceAggregateId(),
                correlationId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(NotificationResponse.from(created));
    }

    @GetMapping("/{id}")
    public NotificationResponse getNotification(@PathVariable UUID id) {
        return NotificationResponse.from(notificationService.getNotification(id));
    }

    @GetMapping
    public List<NotificationResponse> getNotificationsFor(@RequestParam UUID sourceAggregateId) {
        return notificationService.getNotificationsFor(sourceAggregateId).stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
