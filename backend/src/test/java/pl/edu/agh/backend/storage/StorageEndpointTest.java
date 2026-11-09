package pl.edu.agh.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.material.MaterialRepository;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MaterialRepository materialRepository;

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

        mockMvc.perform(get("/api/alumni/materials").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + id + "')].url", hasItem(startsWith(signedPrefix))));
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

    private User savedUser() {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setFirstName("Jan");
        user.setLastName("Kowalski");
        return userRepository.save(user);
    }
}
