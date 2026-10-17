package pl.edu.agh.backend.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class EventSlugMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Test
    void backfillsUniqueSlugsForExistingEvents() {
        DataSource dataSource =
                new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        MigrationVersion slugVersion = MigrationVersion.fromVersion("28");
        MigrationVersion beforeSlugs = Arrays.stream(
                        flyway(dataSource).load().info().all())
                .map(MigrationInfo::getVersion)
                .filter(version -> version.compareTo(slugVersion) < 0)
                .max(Comparator.naturalOrder())
                .orElseThrow();
        flyway(dataSource).target(beforeSlugs).load().migrate();

        UUID first = insertEvent(jdbc, "Wieczór networkingowy alumnów", "2026-01-01T10:00:00Z", false);
        UUID second = insertEvent(jdbc, "Wieczór  networkingowy, alumnów!", "2026-01-02T10:00:00Z", false);
        UUID third = insertEvent(jdbc, "Wieczór networkingowy alumnów 2", "2026-01-03T10:00:00Z", false);
        UUID symbols = insertEvent(jdbc, "???", "2026-01-04T10:00:00Z", false);
        UUID deleted = insertEvent(jdbc, "Wieczór networkingowy alumnów", "2025-12-01T10:00:00Z", true);
        UUID diacritics = insertEvent(jdbc, "Zażółć gęślą jaźń: ŁÓDŹ", "2026-01-05T10:00:00Z", false);

        flyway(dataSource).load().migrate();

        Map<UUID, String> slugs = jdbc
                .query(
                        "SELECT id, slug FROM events",
                        (rs, i) -> Map.entry(rs.getObject("id", UUID.class), rs.getString("slug")))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(slugs)
                .containsEntry(first, "wieczor-networkingowy-alumnow")
                .containsEntry(second, "wieczor-networkingowy-alumnow-2")
                .containsEntry(third, "wieczor-networkingowy-alumnow-2-2")
                .containsEntry(symbols, "wydarzenie")
                .containsEntry(deleted, "wieczor-networkingowy-alumnow-3")
                .containsEntry(diacritics, "zazolc-gesla-jazn-lodz");

        assertThatThrownBy(() -> jdbc.update("UPDATE events SET slug = 'wydarzenie' WHERE id = ?", first))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static FluentConfiguration flyway(DataSource dataSource) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration");
    }

    private static UUID insertEvent(JdbcTemplate jdbc, String title, String createdAt, boolean deleted) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                        INSERT INTO events (id, title, type, starts_at, ends_at, audience, created_at, updated_at, deleted_at)
                        VALUES (?, ?, 'ONLINE', now(), now() + interval '1 hour', 'PUBLIC',
                                ?::timestamptz, now(), CASE WHEN ? THEN now() END)
                        """, id, title, createdAt, deleted);
        return id;
    }
}
