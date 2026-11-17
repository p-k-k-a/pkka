package pl.edu.agh.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;
import tools.jackson.databind.json.JsonMapper;

/** Every kind of record that can hold a file must show up here, or the cleanup deletes files still in use. */
@SpringBootTest(
        properties = {
            "app.storage.endpoint=https://acct.eu.r2.cloudflarestorage.com",
            "app.storage.bucket=pkka-files",
            "app.storage.access-key-id=test-key-id",
            "app.storage.secret-access-key=test-secret"
        })
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class StoredFileReferencesTest {

    private static final String ADMIN_SUBJECT = "44444444-4444-4444-4444-444444444444";

    /**
     * Columns whose names suggest a file but which never hold an object key. The text ones are here only as
     * long as nothing can upload into them: the day an editor with pictures is put on, say, an event's
     * description, that column has to move to {@link StoredFileReferences}.
     */
    private static final Set<String> NOT_FILES = Set.of(
            "events.full_description",
            "events.transmission_url",
            "materials.description",
            "survey_questions.content",
            "surveys.description",
            "tickets.admin_response",
            "tickets.description",
            "topic_group_requests.description",
            "topic_groups.description",
            "users.bio",
            "users.github_url",
            "users.keycloak_id",
            "users.linkedin_url");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoredFileReferences references;

    @Autowired
    private StorageService storage;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void findsTheFilesOfEveryKindOfRecord() throws Exception {
        String avatarKey = savedUserWithAvatar();
        String materialKey = presign("MATERIAL", "application/pdf");
        String eventCoverKey = presign("EVENT_COVER", "image/png");
        String postCoverKey = presign("POST_COVER", "image/webp");
        String firstPicture = presign("POST_IMAGE", "image/png");
        String secondPicture = presign("POST_IMAGE", "image/jpeg");
        String unattached = presign("POST_IMAGE", "image/png");

        create("/api/admin/materials", Map.of("title", "Slajdy", "type", "PRESENTATION", "url", materialKey));
        create("/api/admin/events", eventWithCover(eventCoverKey));
        create(
                "/api/admin/posts",
                Map.of(
                        "title",
                        "Relacja",
                        "coverImageUrl",
                        postCoverKey,
                        "content",
                        "![a](" + firstPicture + ") tekst ![b](" + secondPicture + " \"podpis\")"));
        entityManager.flush();

        assertThat(references.referencedKeys())
                .contains(avatarKey, materialKey, eventCoverKey, postCoverKey, firstPicture, secondPicture)
                .doesNotContain(unattached);
    }

    @Test
    void aSoftDeletedEventStillHoldsItsCover() throws Exception {
        String coverKey = presign("EVENT_COVER", "image/png");
        String created = create("/api/admin/events", eventWithCover(coverKey));
        String id = JsonPath.read(created, "$.id");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/admin/events/{id}", id)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        entityManager.flush();

        assertThat(references.referencedKeys()).contains(coverKey);
    }

    @Test
    void aPictureIsFoundWhereverItsKeyStandsInTheContent() throws Exception {
        String inRawHtml = presign("POST_IMAGE", "image/png");
        String inALink = presign("POST_IMAGE", "image/jpeg");
        String gluedToText = presign("POST_IMAGE", "image/webp");
        // None of these is a markdown image target, so the content is stored as written: a signed URL stays a
        // signed URL, and the key has to be picked out of the middle of it.
        String content = "<img src=\"" + storage.resolveUrl(inRawHtml) + "\"> [zobacz](" + inALink + ") " + gluedToText
                + "abc123";

        create("/api/admin/posts", Map.of("title", "Nietypowy", "content", content));
        entityManager.flush();

        assertThat(references.referencedKeys()).contains(inRawHtml, inALink, gluedToText);
    }

    @Test
    void aSingleFileIsLookedUpInEveryPlace() throws Exception {
        String avatarKey = savedUserWithAvatar();
        String materialKey = presign("MATERIAL", "application/pdf");
        String eventCoverKey = presign("EVENT_COVER", "image/png");
        String postCoverKey = presign("POST_COVER", "image/webp");
        String picture = presign("POST_IMAGE", "image/png");
        String unattached = presign("POST_IMAGE", "image/png");
        create("/api/admin/materials", Map.of("title", "Slajdy", "type", "PRESENTATION", "url", materialKey));
        create("/api/admin/events", eventWithCover(eventCoverKey));
        create(
                "/api/admin/posts",
                Map.of("title", "Relacja", "coverImageUrl", postCoverKey, "content", "![a](" + picture + ")"));
        entityManager.flush();

        for (String key : new String[] {avatarKey, materialKey, eventCoverKey, postCoverKey, picture}) {
            assertThat(references.isReferenced(key)).as(key).isTrue();
        }
        assertThat(references.isReferenced(unattached)).isFalse();
    }

    @Test
    void everyKindOfUploadHasItsPlaceInTheDatabase() {
        assertThat(StoredFileReferences.COLUMNS).containsOnlyKeys(UploadCategory.values());
    }

    /**
     * A new column that could hold a file has to be decided on: listed in {@link StoredFileReferences} if it
     * stores object keys, or below if it does not. Until then the cleanup would delete its files.
     */
    @Test
    void everyColumnThatCouldHoldAFileIsAccountedFor() {
        Set<String> accountedFor = new TreeSet<>(StoredFileReferences.KEY_COLUMNS);
        accountedFor.add(StoredFileReferences.CONTENT_COLUMN);
        accountedFor.addAll(NOT_FILES);

        List<String> candidates = jdbc.queryForList("""
                SELECT table_name || '.' || column_name FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND data_type IN ('text', 'character varying', 'json', 'jsonb', 'ARRAY')
                  AND table_name NOT IN ('flyway_schema_history', 'storage_orphans')
                  AND column_name ~ '(url|key|image|avatar|cover|file|photo|picture|attachment|content|body|logo|banner|thumbnail|icon|document|media|path|link|description|bio|text|markdown|response)'
                """, String.class);

        assertThat(candidates).isNotEmpty();
        assertThat(new TreeSet<>(candidates)).isSubsetOf(accountedFor);
        assertThat(candidates)
                .containsAll(StoredFileReferences.KEY_COLUMNS)
                .contains(StoredFileReferences.CONTENT_COLUMN);
    }

    private String savedUserWithAvatar() {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user = userRepository.saveAndFlush(user);
        String key = "avatars/" + user.getId() + "/" + UUID.randomUUID() + ".png";
        user.setAvatarKey(key);
        userRepository.saveAndFlush(user);
        return key;
    }

    private String presign(String purpose, String contentType) throws Exception {
        String presigned = mockMvc.perform(post("/api/admin/files/presign")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new JsonMapper()
                                .writeValueAsString(
                                        Map.of("purpose", purpose, "contentType", contentType, "sizeBytes", 2048))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(presigned, "$.objectKey");
    }

    private String create(String path, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(path)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new JsonMapper().writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static Map<String, Object> eventWithCover(String coverKey) {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        return Map.of(
                "title",
                "Wydarzenie z okładką",
                "type",
                "ONLINE",
                "startsAt",
                start.toString(),
                "endsAt",
                start.plus(1, ChronoUnit.HOURS).toString(),
                "transmissionUrl",
                "https://meet.google.com/abc-defg-hij",
                "audience",
                "PUBLIC",
                "coverImageUrl",
                coverKey,
                "tags",
                List.of());
    }
}
