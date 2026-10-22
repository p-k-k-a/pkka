package pl.edu.agh.backend.legal;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Runs the real migrations up to the one before legal documents, seeds old-style consents, then migrates on. */
@Testcontainers
class LegalDocumentsMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final MigrationVersion LEGAL_DOCUMENTS = MigrationVersion.fromVersion("35");

    private static final Instant FIRST_CONSENT = Instant.parse("2025-03-01T10:00:00Z");

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final UUID CAROL = UUID.randomUUID();

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(dataSource);

        flyway(dataSource, lastVersionBeforeLegalDocuments(dataSource)).migrate();

        // Alice applied twice; both applications carry both consents and she asked for the newsletter once.
        user(ALICE);
        UUID aliceRejected = application(ALICE, "REJECTED", false, FIRST_CONSENT);
        consent(aliceRejected, "REGULATIONS_PRIVACY", FIRST_CONSENT);
        consent(aliceRejected, "GDPR_DATA_PROCESSING", FIRST_CONSENT);
        UUID aliceApproved = application(ALICE, "APPROVED", true, FIRST_CONSENT.plusSeconds(86_400));
        consent(aliceApproved, "REGULATIONS_PRIVACY", FIRST_CONSENT.plusSeconds(86_400));
        consent(aliceApproved, "GDPR_DATA_PROCESSING", FIRST_CONSENT.plusSeconds(86_400));

        user(BOB);
        UUID bobs = application(BOB, "UNDER_REVIEW", false, FIRST_CONSENT.plusSeconds(3_600));
        consent(bobs, "REGULATIONS_PRIVACY", FIRST_CONSENT.plusSeconds(3_600));
        consent(bobs, "GDPR_DATA_PROCESSING", FIRST_CONSENT.plusSeconds(3_600));

        user(CAROL);

        flyway(dataSource, "latest").migrate();
    }

    @Test
    void everyTypeGetsAnArchivalFirstVersionAndADraftSecondOne() {
        List<Map<String, Object>> documents = jdbc.queryForList("""
                select type, version, requires_reacceptance from legal_documents order by type, version
                """);

        assertThat(documents)
                .extracting(d -> d.get("type") + " v" + d.get("version") + " " + d.get("requires_reacceptance"))
                .containsExactly(
                        "MEMBERSHIP_CONSENT v1 false",
                        "MEMBERSHIP_CONSENT v2 true",
                        "NEWSLETTER_CONSENT v1 false",
                        "NEWSLETTER_CONSENT v2 false",
                        "PRIVACY_NOTICE v1 false",
                        "PRIVACY_NOTICE v2 true",
                        "TERMS v1 false",
                        "TERMS v2 true");
    }

    @Test
    void archivalVersionIsNotDatedAfterTheConsentsItHolds() {
        Timestamp publishedAt =
                jdbc.queryForObject("select min(published_at) from legal_documents where version = 1", Timestamp.class);

        assertThat(publishedAt.toInstant()).isEqualTo(FIRST_CONSENT);
        assertThat(jdbc.queryForObject("""
                        select count(*) from consent_records c join legal_documents d on d.id = c.document_id
                        where c.granted_at < d.published_at
                        """, Integer.class)).isZero();
    }

    @Test
    void oldConsentsMoveToTheArchivalVersionsOncePerUser() {
        assertThat(consentsOf(ALICE))
                .containsExactlyInAnyOrder(
                        "TERMS v1 " + FIRST_CONSENT,
                        "PRIVACY_NOTICE v1 " + FIRST_CONSENT,
                        "MEMBERSHIP_CONSENT v1 " + FIRST_CONSENT,
                        "NEWSLETTER_CONSENT v1 " + FIRST_CONSENT.plusSeconds(86_400));
        assertThat(consentsOf(BOB))
                .containsExactlyInAnyOrder(
                        "TERMS v1 " + FIRST_CONSENT.plusSeconds(3_600),
                        "PRIVACY_NOTICE v1 " + FIRST_CONSENT.plusSeconds(3_600),
                        "MEMBERSHIP_CONSENT v1 " + FIRST_CONSENT.plusSeconds(3_600));
        assertThat(consentsOf(CAROL)).isEmpty();
        assertThat(jdbc.queryForList("select distinct source from consent_records", String.class))
                .containsExactly("LEGACY");
    }

    private static List<String> consentsOf(UUID userId) {
        return jdbc.query(
                """
                        select d.type, d.version, c.granted_at
                        from consent_records c join legal_documents d on d.id = c.document_id
                        where c.user_id = ?
                        """,
                (rs, i) -> rs.getString("type") + " v" + rs.getInt("version") + " "
                        + rs.getTimestamp("granted_at").toInstant(),
                userId);
    }

    private static String lastVersionBeforeLegalDocuments(DriverManagerDataSource dataSource) {
        return Arrays.stream(flyway(dataSource, "latest").info().all())
                .map(MigrationInfo::getVersion)
                .filter(version -> version.compareTo(LEGAL_DOCUMENTS) < 0)
                .max(MigrationVersion::compareTo)
                .orElseThrow()
                .getVersion();
    }

    private static Flyway flyway(DriverManagerDataSource dataSource, String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private static void user(UUID id) {
        jdbc.update(
                "insert into users (id, keycloak_id, created_at, updated_at) values (?, ?, now(), now())",
                id,
                id.toString());
    }

    private static UUID application(UUID applicantId, String status, boolean newsletter, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                        insert into applications (id, applicant_id, status, faculty, field_of_study, study_type,
                            graduation_year, phone_number, newsletter_subscription, created_at, updated_at)
                        values (?, ?, ?, 'WI', 'Informatyka', 'MASTER', 2020, '+48123456789', ?, ?, ?)
                        """, id, applicantId, status, newsletter, Timestamp.from(createdAt), Timestamp.from(createdAt));
        return id;
    }

    private static void consent(UUID applicationId, String type, Instant grantedAt) {
        jdbc.update(
                "insert into application_consents (id, application_id, type, granted_at) values (?, ?, ?, ?)",
                UUID.randomUUID(),
                applicationId,
                type,
                Timestamp.from(grantedAt));
    }
}
