package pl.edu.agh.backend.infrastructure.discord;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class DiscordEmbedTest {

    @Test
    void aDescriptionOverDiscordsLimitIsCutShortRatherThanRefused() {
        DiscordEmbed embed = new DiscordEmbed("Warsztaty", "a".repeat(5000), List.of());

        assertThat(embed.description()).hasSize(4096).endsWith("…");
    }

    @Test
    void textWithinDiscordsLimitsIsLeftAlone() {
        DiscordEmbed embed = new DiscordEmbed("Warsztaty", "Opis", List.of(new DiscordEmbed.Field("Gdzie", "D17")));

        assertThat(embed.title()).isEqualTo("Warsztaty");
        assertThat(embed.description()).isEqualTo("Opis");
        assertThat(embed.fields()).containsExactly(new DiscordEmbed.Field("Gdzie", "D17"));
    }
}
