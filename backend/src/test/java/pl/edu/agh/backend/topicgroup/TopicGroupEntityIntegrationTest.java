package pl.edu.agh.backend.topicgroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
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
class TopicGroupEntityIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TopicGroupRepository topicGroupRepository;

    @Autowired
    private TopicGroupRequestRepository topicGroupRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private User newUser() {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        return userRepository.saveAndFlush(user);
    }

    private TopicGroup newGroup(String name, String slug, User owner) {
        return TopicGroup.builder()
                .name(name)
                .slug(slug)
                .description("Dla wszystkich, kto pracuje z modelami językowymi.")
                .owner(owner)
                .build();
    }

    private TopicGroupRequest newRequest(User requester, String proposedName) {
        return TopicGroupRequest.builder()
                .requester(requester)
                .proposedName(proposedName)
                .description("Chcemy wymieniać się doświadczeniem z wdrożeń AI.")
                .build();
    }

    private void deleteAccount(User user) {
        jdbc.update("delete from users where id = ?", user.getId());
        entityManager.clear();
    }

    @Test
    void persistsAGroupWithItsOwner() {
        User owner = newUser();

        TopicGroup saved = topicGroupRepository.saveAndFlush(newGroup("Grupa AI", "grupa-ai", owner));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
        assertThat(topicGroupRepository.findById(saved.getId()))
                .hasValueSatisfying(
                        group -> assertThat(group.getOwner().getId()).isEqualTo(owner.getId()));
        assertThat(topicGroupRepository.existsBySlug("grupa-ai")).isTrue();
        assertThat(topicGroupRepository.existsBySlug("grupa-chmurowa")).isFalse();
    }

    @Test
    void refusesASecondGroupWithTheSameSlug() {
        User owner = newUser();
        topicGroupRepository.saveAndFlush(newGroup("Grupa AI", "grupa-ai", owner));

        assertThatThrownBy(() -> topicGroupRepository.saveAndFlush(newGroup("Grupa AI inaczej", "grupa-ai", owner)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refusesASecondGroupWithTheSameName() {
        User owner = newUser();
        topicGroupRepository.saveAndFlush(newGroup("Grupa AI", "grupa-ai", owner));

        assertThatThrownBy(() -> topicGroupRepository.saveAndFlush(newGroup("Grupa AI", "grupa-ai-2", owner)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aFreshRequestAwaitsReview() {
        TopicGroupRequest saved = topicGroupRequestRepository.saveAndFlush(newRequest(newUser(), "Grupa AI"));

        assertThat(saved.getStatus()).isEqualTo(TopicGroupRequestStatus.UNDER_REVIEW);
        assertThat(saved.getReviewedBy()).isNull();
        assertThat(saved.getReviewedAt()).isNull();
        assertThat(saved.getCreatedGroup()).isNull();
    }

    @Test
    void refusesASecondRequestAwaitingReviewFromTheSamePerson() {
        User requester = newUser();
        topicGroupRequestRepository.saveAndFlush(newRequest(requester, "Grupa AI"));

        assertThatThrownBy(() -> topicGroupRequestRepository.saveAndFlush(newRequest(requester, "Grupa chmurowa")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void approvingARequestRecordsTheReviewerAndTheGroupItCreated() {
        User requester = newUser();
        User admin = newUser();
        TopicGroupRequest request = topicGroupRequestRepository.saveAndFlush(newRequest(requester, "Grupa AI"));
        TopicGroup group = topicGroupRepository.saveAndFlush(newGroup("Grupa AI", "grupa-ai", requester));

        request.approve(admin, group);
        TopicGroupRequest reviewed = topicGroupRequestRepository.saveAndFlush(request);

        assertThat(reviewed.getStatus()).isEqualTo(TopicGroupRequestStatus.APPROVED);
        assertThat(reviewed.getReviewedBy().getId()).isEqualTo(admin.getId());
        assertThat(reviewed.getReviewedAt()).isNotNull();
        assertThat(reviewed.getCreatedGroup().getId()).isEqualTo(group.getId());
        assertThat(reviewed.getRejectionReason()).isNull();
    }

    @Test
    void rejectingARequestRecordsWhy() {
        User admin = newUser();
        TopicGroupRequest request = topicGroupRequestRepository.saveAndFlush(newRequest(newUser(), "Grupa AI"));

        request.reject(admin, "Taka grupa już istnieje.");
        TopicGroupRequest reviewed = topicGroupRequestRepository.saveAndFlush(request);

        assertThat(reviewed.getStatus()).isEqualTo(TopicGroupRequestStatus.REJECTED);
        assertThat(reviewed.getRejectionReason()).isEqualTo("Taka grupa już istnieje.");
        assertThat(reviewed.getCreatedGroup()).isNull();
    }

    @Test
    void aRequestCannotBeReviewedTwice() {
        User admin = newUser();
        TopicGroupRequest request = topicGroupRequestRepository.saveAndFlush(newRequest(newUser(), "Grupa AI"));
        request.reject(admin, "Zbyt wąski temat.");

        assertThatThrownBy(() -> request.approve(admin, null))
                .isInstanceOf(InvalidTopicGroupRequestStateException.class);
        assertThatThrownBy(() -> request.reject(admin, "Jeszcze raz"))
                .isInstanceOf(InvalidTopicGroupRequestStateException.class);
    }

    @Test
    void deletingTheOwnersAccountLeavesTheGroupWithoutAnOwner() {
        User owner = newUser();
        TopicGroup group = topicGroupRepository.saveAndFlush(newGroup("Grupa AI", "grupa-ai", owner));

        deleteAccount(owner);

        assertThat(topicGroupRepository.findById(group.getId()))
                .hasValueSatisfying(saved -> assertThat(saved.getOwner()).isNull());
    }

    @Test
    void deletingTheRequestersAccountDeletesTheirRequests() {
        User requester = newUser();
        TopicGroupRequest request = topicGroupRequestRepository.saveAndFlush(newRequest(requester, "Grupa AI"));

        deleteAccount(requester);

        assertThat(topicGroupRequestRepository.findById(request.getId())).isEmpty();
    }

    @Test
    void deletingTheReviewersAccountKeepsTheReviewedRequest() {
        User admin = newUser();
        TopicGroupRequest request = topicGroupRequestRepository.saveAndFlush(newRequest(newUser(), "Grupa AI"));
        request.reject(admin, "Zbyt wąski temat.");
        topicGroupRequestRepository.saveAndFlush(request);

        deleteAccount(admin);

        assertThat(topicGroupRequestRepository.findById(request.getId())).hasValueSatisfying(saved -> {
            assertThat(saved.getStatus()).isEqualTo(TopicGroupRequestStatus.REJECTED);
            assertThat(saved.getReviewedBy()).isNull();
        });
    }

    @Test
    void deletingAGroupKeepsTheRequestThatCreatedIt() {
        User requester = newUser();
        TopicGroupRequest request = topicGroupRequestRepository.saveAndFlush(newRequest(requester, "Grupa AI"));
        TopicGroup group = topicGroupRepository.saveAndFlush(newGroup("Grupa AI", "grupa-ai", requester));
        request.approve(newUser(), group);
        topicGroupRequestRepository.saveAndFlush(request);

        jdbc.update("delete from topic_groups where id = ?", group.getId());
        entityManager.clear();

        assertThat(topicGroupRequestRepository.findById(request.getId())).hasValueSatisfying(saved -> {
            assertThat(saved.getStatus()).isEqualTo(TopicGroupRequestStatus.APPROVED);
            assertThat(saved.getCreatedGroup()).isNull();
        });
    }
}
