package pl.edu.agh.backend.notifications;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Expo reports an uninstalled app's token as gone only in the receipt, minutes after accepting the send, and keeps
 * receipts for about a day.
 */
@Component
@RequiredArgsConstructor
public class PushReceiptChecker {

    private static final Duration RECEIPT_DELAY = Duration.ofMinutes(15);
    private static final Duration RECEIPT_RETENTION = Duration.ofHours(24);

    private final PushTicketRepository pushTicketRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final ExpoPushClient expoPushClient;

    @Scheduled(cron = "${app.notifications.receipt-cron:0 */15 * * * *}")
    @Transactional
    public void checkReceipts() {
        Instant now = Instant.now();
        pushTicketRepository.deleteCreatedBefore(now.minus(RECEIPT_RETENTION));

        List<PushTicket> ripe = pushTicketRepository.findByCreatedAtBefore(now.minus(RECEIPT_DELAY));
        if (ripe.isEmpty()) {
            return;
        }

        ExpoPushClient.Receipts receipts = expoPushClient.fetchReceipts(
                ripe.stream().map(PushTicket::getId).toList());
        List<String> goneTokens = ripe.stream()
                .filter(ticket -> receipts.deviceGone().contains(ticket.getId()))
                .map(PushTicket::getToken)
                .toList();
        if (!goneTokens.isEmpty()) {
            deviceTokenRepository.deleteByTokenIn(goneTokens);
        }
        pushTicketRepository.deleteAllById(receipts.checked());
    }
}
