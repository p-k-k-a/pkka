package pl.edu.agh.backend.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;
import pl.edu.agh.backend.infrastructure.discord.DiscordEmbed;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildSettings;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildSettingsRepository;
import pl.edu.agh.backend.infrastructure.discord.DiscordMessages;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordEventPublisher {

    private final EventRepository eventRepository;
    private final EventDiscordPostRepository postRepository;
    private final DiscordGuildSettingsRepository guildSettings;
    private final DiscordMessages discordMessages;

    @Value("${app.web.base-url}")
    private String webBaseUrl;

    @Async
    @TransactionalEventListener
    public void onEventCreated(EventCreatedEvent created) {
        sync(created.eventId());
    }

    @Async
    @TransactionalEventListener
    public void onEventUpdated(EventUpdatedEvent updated) {
        sync(updated.eventId());
    }

    @Async
    @TransactionalEventListener
    public void onEventDeleted(EventDeletedEvent deleted) {
        sync(deleted.eventId());
    }

    // Synchronized so that an edit right after creating an event cannot race its announcement into a second post.
    private synchronized void sync(UUID eventId) {
        Optional<Event> event = eventRepository.findById(eventId).filter(DiscordEventPublisher::belongsOnDiscord);
        Optional<EventDiscordPost> post = postRepository.findById(eventId);
        if (event.isPresent() && post.isPresent()) {
            discordMessages.edit(post.get().getChannelId(), post.get().getMessageId(), announcement(event.get()));
        } else if (event.isPresent()) {
            announce(event.get());
        } else if (post.isPresent()) {
            withdraw(post.get());
        }
    }

    private static boolean belongsOnDiscord(Event event) {
        return switch (event.getAudience()) {
            case PUBLIC, ALL_ALUMNI -> true;
            case SPECIFIC_GROUP -> false;
        };
    }

    private void announce(Event event) {
        Optional<String> channelId =
                guildSettings.findFirstBy().flatMap(DiscordGuildSettings::getAnnouncementsChannelId);
        if (channelId.isEmpty()) {
            return;
        }
        discordMessages
                .post(channelId.get(), announcement(event))
                .ifPresent(messageId -> postRepository.save(EventDiscordPost.builder()
                        .eventId(event.getId())
                        .channelId(channelId.get())
                        .messageId(messageId)
                        .postedAt(Instant.now())
                        .build()));
    }

    private void withdraw(EventDiscordPost post) {
        if (discordMessages.delete(post.getChannelId(), post.getMessageId())) {
            postRepository.deleteById(post.getEventId());
        }
    }

    private DiscordEmbed announcement(Event event) {
        List<DiscordEmbed.Field> fields = new ArrayList<>();
        fields.add(new DiscordEmbed.Field("Kiedy", "<t:%d:F>".formatted(event.getStartsAt().getEpochSecond())));
        place(event).ifPresent(place -> fields.add(new DiscordEmbed.Field("Gdzie", place)));
        if (!webBaseUrl.isBlank()) {
            fields.add(new DiscordEmbed.Field("Link", "[Zobacz na stronie](%s)".formatted(eventPage(event))));
        }
        return new DiscordEmbed(event.getTitle(), event.getFullDescription(), fields);
    }

    private String eventPage(Event event) {
        return UriComponentsBuilder.fromUriString(webBaseUrl)
                .pathSegment("events", event.getId().toString())
                .toUriString();
    }

    private static Optional<String> place(Event event) {
        Optional<String> location = Optional.ofNullable(event.getLocation()).filter(text -> !text.isBlank());
        return switch (event.getType()) {
            case ONLINE -> Optional.of("Online");
            case IN_PERSON -> location;
            case HYBRID -> Optional.of(location.map(text -> text + " i online").orElse("Na miejscu i online"));
        };
    }
}
