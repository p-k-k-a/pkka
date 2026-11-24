package pl.edu.agh.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.material.MaterialRepository;
import pl.edu.agh.backend.post.PostRepository;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;
import tools.jackson.databind.json.JsonMapper;

/** Presign endpoints end to end; the presigner signs locally, so no R2 is contacted. */
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
class StorageEndpointTest {

    private static final String ADMIN_SUBJECT = "22222222-2222-2222-2222-222222222222";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoSpyBean
    private StorageService storage;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void everyFileIsUploaded() {
        doNothing().when(storage).requireUploaded(anyString());
    }

    @Test
    void adminGetsAPresignedUrlForAMaterial() throws Exception {
        mockMvc.perform(post("/api/admin/files/presign")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purpose":"MATERIAL","contentType":"application/pdf","sizeBytes":2048}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.uploadUrl", startsWith("https://acct.eu.r2.cloudflarestorage.com/pkka-files/materials/")))
                .andExpect(jsonPath("$.headers['content-type']").value("application/pdf"))
                .andExpect(jsonPath("$.objectKey", startsWith("materials/")))
                .andExpect(jsonPath("$.publicUrl").doesNotExist())
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void adminUploadRejectsDisallowedTypesAndOversizeFiles() throws Exception {
        mockMvc.perform(post("/api/admin/files/presign")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purpose":"EVENT_COVER","contentType":"application/pdf","sizeBytes":2048}
                                """))
                .andExpect(status().isUnsupportedMediaType());

        mockMvc.perform(post("/api/admin/files/presign")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purpose":"EVENT_COVER","contentType":"image/png","sizeBytes":999999999}
                                """))
                .andExpect(status().isContentTooLarge());

        mockMvc.perform(post("/api/admin/files/presign")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purpose":"AVATAR","contentType":"image/png","sizeBytes":10}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdminsCannotUseTheAdminUploadEndpoint() throws Exception {
        String body = """
                {"purpose":"MATERIAL","contentType":"application/pdf","sizeBytes":2048}
                """;

        mockMvc.perform(post("/api/admin/files/presign")
                        .with(JwtTestSupport.asUser())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/files/presign")
                        .with(JwtTestSupport.asVerifiedAlumn())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/files/presign")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oneUserGetsALimitedNumberOfAvatarUploads() throws Exception {
        User user = savedUser();
        String valid = "{\"contentType\":\"image/webp\",\"sizeBytes\":4096}";

        // rejected requests do not use the limit up
        avatarPresign(user, "{\"contentType\":\"text/html\",\"sizeBytes\":4096}")
                .andExpect(status().isUnsupportedMediaType());
        for (int i = 0; i < UploadLimiter.Kind.AVATAR.maxPerWindow(); i++) {
            avatarPresign(user, valid).andExpect(status().isOk());
        }

        avatarPresign(user, valid)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Too many uploads"));
        avatarPresign(savedUser(), valid).andExpect(status().isOk());
    }

    private ResultActions avatarPresign(User user, String body) throws Exception {
        return mockMvc.perform(post("/api/profiles/me/avatar/presign")
                .with(JwtTestSupport.asUser(user.getKeycloakId()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void userUploadsAnAvatarAndAttachesIt() throws Exception {
        User user = savedUser();

        String presigned = mockMvc.perform(post("/api/profiles/me/avatar/presign")
                        .with(JwtTestSupport.asUser(user.getKeycloakId()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"image/webp","sizeBytes":4096}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectKey", startsWith("avatars/" + user.getId() + "/")))
                .andExpect(jsonPath("$.uploadUrl", containsString("/pkka-files/avatars/" + user.getId() + "/")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String key = JsonPath.read(presigned, "$.objectKey");
        String signedPrefix = "https://acct.eu.r2.cloudflarestorage.com/pkka-files/" + key + "?";

        mockMvc.perform(patch("/api/profiles/me")
                        .with(JwtTestSupport.asUser(user.getKeycloakId()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatarKey\":\"" + key + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl", startsWith(signedPrefix)))
                .andExpect(jsonPath("$.avatarUrl", containsString("X-Amz-Signature=")));

        mockMvc.perform(get("/api/profiles/me").with(JwtTestSupport.asUser(user.getKeycloakId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl", startsWith(signedPrefix)));

        mockMvc.perform(patch("/api/profiles/me")
                        .with(JwtTestSupport.asUser(user.getKeycloakId()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatarKey\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());
    }

    @Test
    void aFileThatCannotBeFoundOrCheckedIsNotAttached() throws Exception {
        String key = presignAdminUpload("MATERIAL", "application/pdf");
        String body = "{\"title\":\"Slajdy\",\"type\":\"PRESENTATION\",\"url\":\"" + key + "\"}";

        doThrow(new InvalidFileException("No file was uploaded")).when(storage).requireUploaded(key);
        mockMvc.perform(post("/api/admin/materials")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        doThrow(new StorageUnavailableException(null)).when(storage).requireUploaded(key);
        mockMvc.perform(post("/api/admin/materials")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void adminUploadedMaterialIsStoredAsKeyAndServedAsSignedUrl() throws Exception {
        String presigned = mockMvc.perform(post("/api/admin/files/presign")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purpose":"MATERIAL","contentType":"application/pdf","sizeBytes":2048}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String key = JsonPath.read(presigned, "$.objectKey");
        String signedPrefix = "https://acct.eu.r2.cloudflarestorage.com/pkka-files/" + key + "?";

        String created = mockMvc.perform(post("/api/admin/materials")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Slajdy\",\"type\":\"PRESENTATION\",\"url\":\"" + key + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", startsWith(signedPrefix)))
                .andExpect(jsonPath("$.fileExtension").value("pdf"))
                .andExpect(jsonPath("$.fileKey").value(key))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(created, "$.id"));
        String signedUrl = JsonPath.read(created, "$.url");
        assertThat(materialRepository.findById(id).orElseThrow().getUrl()).isEqualTo(key);

        // an edit form echoes the signed URL back; it must not end up in the database
        mockMvc.perform(put("/api/admin/materials/{id}", id)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Slajdy v2\",\"type\":\"PRESENTATION\",\"url\":\"" + signedUrl + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", startsWith(signedPrefix)));
        assertThat(materialRepository.findById(id).orElseThrow().getUrl()).isEqualTo(key);

        mockMvc.perform(get("/api/materials").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + id + "')].url", hasItem(startsWith(signedPrefix))))
                .andExpect(jsonPath("$.content[?(@.id=='" + id + "')].fileExtension", hasItem("pdf")));
    }

    @Test
    void postCoverAndPicturesAreStoredAsKeysAndServedAsSignedUrls() throws Exception {
        String coverKey = presignAdminUpload("POST_COVER", "image/webp");
        String imageKey = presignAdminUpload("POST_IMAGE", "image/png");
        String signedCoverPrefix = "https://acct.eu.r2.cloudflarestorage.com/pkka-files/" + coverKey + "?";
        String signedImagePrefix = "](https://acct.eu.r2.cloudflarestorage.com/pkka-files/" + imageKey + "?";

        String created = mockMvc.perform(post("/api/admin/posts")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Relacja\",\"status\":\"PUBLISHED\",\"coverImageUrl\":\"" + coverKey
                                + "\",\"content\":\"Zdjęcie: ![sala](" + imageKey + ")\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coverImageUrl", startsWith(signedCoverPrefix)))
                .andExpect(jsonPath("$.content", containsString(signedImagePrefix)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(created, "$.id"));
        String slug = JsonPath.read(created, "$.slug");
        String signedCover = JsonPath.read(created, "$.coverImageUrl");
        String signedContent = JsonPath.read(created, "$.content");
        assertThat(postRepository.findById(id).orElseThrow().getCoverImageUrl()).isEqualTo(coverKey);
        assertThat(postRepository.findById(id).orElseThrow().getContent())
                .isEqualTo("Zdjęcie: ![sala](" + imageKey + ")");

        // the editor echoes signed URLs back; none of them may end up in the database
        mockMvc.perform(put("/api/admin/posts/{id}", id)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new JsonMapper()
                                .writeValueAsString(Map.of(
                                        "title",
                                        "Relacja",
                                        "status",
                                        "PUBLISHED",
                                        "coverImageUrl",
                                        signedCover,
                                        "content",
                                        signedContent))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl", startsWith(signedCoverPrefix)));
        assertThat(postRepository.findById(id).orElseThrow().getCoverImageUrl()).isEqualTo(coverKey);
        assertThat(postRepository.findById(id).orElseThrow().getContent())
                .isEqualTo("Zdjęcie: ![sala](" + imageKey + ")");

        mockMvc.perform(get("/api/public/posts/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl", startsWith(signedCoverPrefix)))
                .andExpect(jsonPath("$.content", containsString(signedImagePrefix)));
        mockMvc.perform(get("/api/public/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.content[?(@.slug=='" + slug + "')].coverImageUrl", hasItem(startsWith(signedCoverPrefix))));
        mockMvc.perform(get("/api/admin/posts").with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.content[?(@.id=='" + id + "')].coverImageUrl", hasItem(startsWith(signedCoverPrefix))));
    }

    @Test
    void anUploadedPostCoverIsDetachedWhenTheUpdateLeavesItOut() throws Exception {
        String coverKey = presignAdminUpload("POST_COVER", "image/png");
        String created = mockMvc.perform(post("/api/admin/posts")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Z okładką\",\"content\":\"x\",\"coverImageUrl\":\"" + coverKey + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(created, "$.id"));

        mockMvc.perform(
                        put("/api/admin/posts/{id}", id)
                                .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"title\":\"Z okładką\",\"content\":\"x\",\"status\":\"DRAFT\",\"coverImageUrl\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl").doesNotExist());
        assertThat(postRepository.findById(id).orElseThrow().getCoverImageUrl()).isNull();
    }

    @Test
    void postContentRejectsLinksThatWouldExpire() throws Exception {
        String materialKey = presignAdminUpload("MATERIAL", "application/pdf");
        String created = mockMvc.perform(post("/api/admin/materials")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Slajdy\",\"type\":\"PRESENTATION\",\"url\":\"" + materialKey + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String signedMaterial = JsonPath.read(created, "$.url");
        long postsBefore = postRepository.count();

        mockMvc.perform(post("/api/admin/posts")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new JsonMapper()
                                .writeValueAsString(
                                        Map.of("title", "X", "content", "[slajdy](" + signedMaterial + ")"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("POST_CONTENT_LINK"));
        assertThat(postRepository.count()).isEqualTo(postsBefore);
    }

    @Test
    void postCoverRejectsAnotherPurposesKey() throws Exception {
        String eventCoverKey = presignAdminUpload("EVENT_COVER", "image/png");
        String imageKey = presignAdminUpload("POST_IMAGE", "image/png");

        for (String cover : new String[] {eventCoverKey, imageKey, "covers/posts/../x.png"}) {
            mockMvc.perform(post("/api/admin/posts")
                            .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"X\",\"content\":\"y\",\"coverImageUrl\":\"" + cover + "\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void adminCannotStoreAnotherPurposesKeyOrArbitraryText() throws Exception {
        for (String url : new String[] {"covers/events/" + UUID.randomUUID() + ".png", "materials/../etc/passwd"}) {
            mockMvc.perform(post("/api/admin/materials")
                            .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"X\",\"type\":\"PRESENTATION\",\"url\":\"" + url + "\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void userCannotAttachSomeoneElsesOrAnExternalAvatar() throws Exception {
        User user = savedUser();
        String foreign = "avatars/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".png";

        for (String url : new String[] {foreign, "https://evil.example.com/a.png"}) {
            mockMvc.perform(patch("/api/profiles/me")
                            .with(JwtTestSupport.asUser(user.getKeycloakId()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"avatarKey\":\"" + url + "\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void avatarPresignRejectsNonImages() throws Exception {
        User user = savedUser();

        mockMvc.perform(post("/api/profiles/me/avatar/presign")
                        .with(JwtTestSupport.asUser(user.getKeycloakId()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentType":"application/pdf","sizeBytes":4096}
                                """))
                .andExpect(status().isUnsupportedMediaType());
    }

    private String presignAdminUpload(String purpose, String contentType) throws Exception {
        String presigned = mockMvc.perform(post("/api/admin/files/presign")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purpose\":\"" + purpose + "\",\"contentType\":\"" + contentType
                                + "\",\"sizeBytes\":2048}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(presigned, "$.objectKey");
    }

    private User savedUser() {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setFirstName("Jan");
        user.setLastName("Kowalski");
        return userRepository.save(user);
    }
}
