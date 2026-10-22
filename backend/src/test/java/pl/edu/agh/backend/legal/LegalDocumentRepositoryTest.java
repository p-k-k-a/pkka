package pl.edu.agh.backend.legal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class LegalDocumentRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private LegalDocumentRepository documents;

    @Autowired
    private ConsentRecordRepository consents;

    @Autowired
    private UserRepository users;

    @Test
    void currentVersionIsTheHighestAlreadyPublished() {
        Instant now = Instant.now();

        assertThat(documents.findCurrent(LegalDocumentType.TERMS, now))
                .get()
                .extracting(LegalDocument::getVersion)
                .isEqualTo(2);
    }

    @Test
    void versionPublishedInTheFutureIsNotCurrentYet() {
        Instant now = Instant.now();
        documents.saveAndFlush(version(LegalDocumentType.TERMS, 3, now.plus(Duration.ofDays(7))));

        assertThat(documents.findCurrent(LegalDocumentType.TERMS, now))
                .get()
                .extracting(LegalDocument::getVersion)
                .isEqualTo(2);
        assertThat(documents.findCurrent(LegalDocumentType.TERMS, now.plus(Duration.ofDays(8))))
                .get()
                .extracting(LegalDocument::getVersion)
                .isEqualTo(3);
    }

    @Test
    void allCurrentHoldsOneVersionPerType() {
        Instant now = Instant.now();
        documents.saveAndFlush(version(LegalDocumentType.PRIVACY_NOTICE, 3, now.minusSeconds(60)));
        documents.saveAndFlush(version(LegalDocumentType.TERMS, 3, now.plus(Duration.ofDays(1))));

        assertThat(documents.findAllCurrent(now))
                .extracting(d -> d.getType() + " v" + d.getVersion())
                .containsExactlyInAnyOrder(
                        "TERMS v2", "PRIVACY_NOTICE v3", "MEMBERSHIP_CONSENT v2", "NEWSLETTER_CONSENT v2");
    }

    @Test
    void withdrawnConsentIsNotActiveButIsKept() {
        Instant now = Instant.now();
        User user = new User();
        user.setKeycloakId("legal-repository-test");
        users.saveAndFlush(user);
        LegalDocument terms =
                documents.findCurrent(LegalDocumentType.TERMS, now).orElseThrow();
        LegalDocument newsletter =
                documents.findCurrent(LegalDocumentType.NEWSLETTER_CONSENT, now).orElseThrow();
        consents.save(consent(user, terms, now));
        ConsentRecord newsletterConsent = consents.save(consent(user, newsletter, now));

        newsletterConsent.withdraw(now.plusSeconds(1));
        consents.flush();

        assertThat(consents.findActiveByUserId(user.getId()))
                .extracting(c -> c.getDocument().getType())
                .containsExactly(LegalDocumentType.TERMS);
        assertThat(consents.existsByUserIdAndDocumentId(user.getId(), newsletter.getId()))
                .isTrue();
    }

    private static LegalDocument version(LegalDocumentType type, int version, Instant publishedAt) {
        return LegalDocument.builder()
                .type(type)
                .version(version)
                .title(type + " v" + version)
                .content("Treść")
                .publishedAt(publishedAt)
                .build();
    }

    private static ConsentRecord consent(User user, LegalDocument document, Instant grantedAt) {
        return ConsentRecord.builder()
                .user(user)
                .document(document)
                .source(ConsentSource.APPLICATION)
                .grantedAt(grantedAt)
                .build();
    }
}
