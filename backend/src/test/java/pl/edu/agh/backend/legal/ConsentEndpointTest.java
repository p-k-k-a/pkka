package pl.edu.agh.backend.legal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.legal.dto.ConsentResponse;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class ConsentEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LegalDocumentRepository documents;

    @Autowired
    private ConsentRecordRepository consents;

    @Autowired
    private UserRepository users;

    @Autowired
    private ConsentService consentService;

    @Test
    void currentDocumentsArePublic() throws Exception {
        mockMvc.perform(get("/api/public/legal-documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type")
                        .value(contains("TERMS", "PRIVACY_NOTICE", "MEMBERSHIP_CONSENT", "NEWSLETTER_CONSENT")))
                .andExpect(jsonPath("$[*].version").value(contains(2, 2, 2, 2)))
                .andExpect(jsonPath("$[*].required").value(contains(true, true, true, false)));

        mockMvc.perform(get("/api/public/legal-documents/PRIVACY_NOTICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.title").value("Informacja o przetwarzaniu danych osobowych"));
    }

    @Test
    void unknownDocumentTypeIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/public/legal-documents/COOKIES")).andExpect(status().isBadRequest());
    }

    @Test
    void consentsNeedALogin() throws Exception {
        mockMvc.perform(get("/api/me/consents")).andExpect(status().isUnauthorized());
    }

    @Test
    void userWithoutALocalRowHasNoConsentsAndIsNotProvisioned() throws Exception {
        String keycloakId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/me/consents").with(JwtTestSupport.asUser(keycloakId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consents").isEmpty())
                .andExpect(jsonPath("$.pendingReacceptance").isEmpty());

        assertThat(users.findByKeycloakId(keycloakId)).isEmpty();
    }

    @Test
    void acceptingTheSameVersionTwiceKeepsOneRecord() throws Exception {
        String keycloakId = UUID.randomUUID().toString();
        LegalDocument newsletter = current(LegalDocumentType.NEWSLETTER_CONSENT);

        grant(keycloakId, newsletter.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("NEWSLETTER_CONSENT"))
                .andExpect(jsonPath("$.version").value(2));
        grant(keycloakId, newsletter.getId()).andExpect(status().isOk());

        UUID userId = users.findByKeycloakId(keycloakId).orElseThrow().getId();
        assertThat(consents.findActiveByUserId(userId)).hasSize(1);
        assertThat(consents.findActiveByUserId(userId).getFirst().getSource()).isEqualTo(ConsentSource.SETTINGS);
    }

    @Test
    void acceptingAVersionNotInForceIsAConflict() throws Exception {
        grant(UUID.randomUUID().toString(), version(LegalDocumentType.TERMS, 1).getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("OUTDATED_DOCUMENT"));
        grant(UUID.randomUUID().toString(), UUID.randomUUID())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("OUTDATED_DOCUMENT"));
    }

    @Test
    void newsletterCanBeWithdrawnAndGivenAgain() throws Exception {
        String keycloakId = UUID.randomUUID().toString();
        UUID newsletter = current(LegalDocumentType.NEWSLETTER_CONSENT).getId();
        grant(keycloakId, newsletter);

        mockMvc.perform(delete("/api/me/consents/NEWSLETTER_CONSENT")
                        .with(JwtTestSupport.asUser(keycloakId))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        myConsents(keycloakId).andExpect(jsonPath("$.consents").isEmpty());

        grant(keycloakId, newsletter).andExpect(status().isOk());
        myConsents(keycloakId).andExpect(jsonPath("$.consents[*].type").value(contains("NEWSLETTER_CONSENT")));
    }

    @Test
    void withdrawingAConsentNeverGivenIsANoOp() throws Exception {
        mockMvc.perform(delete("/api/me/consents/NEWSLETTER_CONSENT")
                        .with(JwtTestSupport.asUser())
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void requiredConsentsCannotBeWithdrawnInTheApp() throws Exception {
        mockMvc.perform(delete("/api/me/consents/TERMS")
                        .with(JwtTestSupport.asUser())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("NOT_WITHDRAWABLE"));
    }

    @Test
    void newVersionRequiringReacceptanceIsPendingUntilAccepted() throws Exception {
        String keycloakId = UUID.randomUUID().toString();
        grant(keycloakId, current(LegalDocumentType.TERMS).getId());
        LegalDocument termsV3 = publish(LegalDocumentType.TERMS, 3, true);

        myConsents(keycloakId)
                .andExpect(jsonPath("$.pendingReacceptance[*].id")
                        .value(contains(termsV3.getId().toString())))
                .andExpect(jsonPath("$.pendingReacceptance[0].changeSummary").value("Zmiana"));

        grant(keycloakId, termsV3.getId()).andExpect(status().isOk());

        myConsents(keycloakId)
                .andExpect(jsonPath("$.pendingReacceptance").isEmpty())
                .andExpect(jsonPath("$.consents[*].version").value(contains(3)));
        UUID userId = users.findByKeycloakId(keycloakId).orElseThrow().getId();
        assertThat(consents.findByUserIdAndDocumentIdAndWithdrawnAtIsNull(userId, termsV3.getId()))
                .get()
                .extracting(ConsentRecord::getSource)
                .isEqualTo(ConsentSource.REACCEPTANCE);
    }

    @Test
    void newVersionWithoutTheFlagIsNotPending() throws Exception {
        String keycloakId = UUID.randomUUID().toString();
        grant(keycloakId, current(LegalDocumentType.TERMS).getId());
        publish(LegalDocumentType.TERMS, 3, false);

        myConsents(keycloakId)
                .andExpect(jsonPath("$.pendingReacceptance").isEmpty())
                .andExpect(jsonPath("$.consents[*].version").value(contains(2)));
    }

    @Test
    void skippedVersionRequiringReacceptanceAsksForTheCurrentOne() throws Exception {
        User user = user();
        accept(user, version(LegalDocumentType.TERMS, 1));
        LegalDocument termsV3 = publish(LegalDocumentType.TERMS, 3, false);

        // v2 demanded re-acceptance and v3 did not; someone still on v1 accepts what is in force now.
        myConsents(user.getKeycloakId())
                .andExpect(jsonPath("$.pendingReacceptance[*].id")
                        .value(contains(termsV3.getId().toString())));
    }

    @Test
    void documentsNeverAcceptedAreNotPending() throws Exception {
        String keycloakId = UUID.randomUUID().toString();
        grant(keycloakId, current(LegalDocumentType.NEWSLETTER_CONSENT).getId());

        myConsents(keycloakId)
                .andExpect(jsonPath("$.pendingReacceptance[*].type").value(not(hasItem("TERMS"))));
    }

    @Test
    void withdrawnConsentIsNotPending() throws Exception {
        String keycloakId = UUID.randomUUID().toString();
        grant(keycloakId, current(LegalDocumentType.NEWSLETTER_CONSENT).getId());
        mockMvc.perform(delete("/api/me/consents/NEWSLETTER_CONSENT")
                .with(JwtTestSupport.asUser(keycloakId))
                .with(csrf()));
        publish(LegalDocumentType.NEWSLETTER_CONSENT, 3, true);

        myConsents(keycloakId).andExpect(jsonPath("$.pendingReacceptance").isEmpty());
    }

    /**
     * Outside the test transaction so each grant commits on its own and the two really race. Lives in this
     * class rather than its own: another Spring context per test class tips the suite past the test heap.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousGrantsOfOneVersionBothSucceedWithOneActiveRecord() throws Exception {
        User user = users.save(newUser());
        Caller caller = new Caller(user.getKeycloakId(), Set.of("USER"));
        UUID newsletter = current(LegalDocumentType.NEWSLETTER_CONSENT).getId();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int attempt = 0; attempt < 20; attempt++) {
                CountDownLatch start = new CountDownLatch(1);
                Callable<ConsentResponse> grant = () -> {
                    start.await();
                    return consentService.grant(caller, newsletter);
                };
                List<Future<ConsentResponse>> grants = List.of(executor.submit(grant), executor.submit(grant));
                start.countDown();
                for (Future<ConsentResponse> result : grants) {
                    result.get();
                }

                assertThat(consents.findActiveByUserId(user.getId())).hasSize(1);
                consentService.withdraw(caller, LegalDocumentType.NEWSLETTER_CONSENT);
            }
        } finally {
            executor.shutdownNow();
            users.delete(user);
        }
    }

    private ResultActions grant(String keycloakId, UUID documentId) throws Exception {
        return mockMvc.perform(post("/api/me/consents")
                .with(JwtTestSupport.asUser(keycloakId))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"documentId\": \"%s\"}".formatted(documentId)));
    }

    private ResultActions myConsents(String keycloakId) throws Exception {
        return mockMvc.perform(get("/api/me/consents").with(JwtTestSupport.asUser(keycloakId)))
                .andExpect(status().isOk());
    }

    private LegalDocument current(LegalDocumentType type) {
        return documents.findCurrent(type, Instant.now()).orElseThrow();
    }

    private LegalDocument version(LegalDocumentType type, int version) {
        return documents.findAll().stream()
                .filter(d -> d.getType() == type && d.getVersion() == version)
                .findFirst()
                .orElseThrow();
    }

    private LegalDocument publish(LegalDocumentType type, int version, boolean requiresReacceptance) {
        return documents.saveAndFlush(LegalDocument.builder()
                .type(type)
                .version(version)
                .title(type + " v" + version)
                .content("Treść")
                .changeSummary("Zmiana")
                .requiresReacceptance(requiresReacceptance)
                .publishedAt(Instant.now().minusSeconds(1))
                .build());
    }

    private User user() {
        return users.saveAndFlush(newUser());
    }

    private static User newUser() {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        return user;
    }

    private void accept(User user, LegalDocument document) {
        consents.saveAndFlush(ConsentRecord.builder()
                .user(user)
                .document(document)
                .source(ConsentSource.LEGACY)
                .grantedAt(Instant.now())
                .build());
    }
}
