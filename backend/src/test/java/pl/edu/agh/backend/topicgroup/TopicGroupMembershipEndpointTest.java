package pl.edu.agh.backend.topicgroup;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class TopicGroupMembershipEndpointTest {

    private static final String GROUPS = "/api/alumni/topic-groups";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicGroupRepository topicGroupRepository;

    @Autowired
    private TopicGroupMembershipRepository membershipRepository;

    private User anna;
    private User piotr;
    private TopicGroup group;

    @BeforeEach
    void setUp() {
        anna = user("Anna", "Nowak");
        piotr = user("Piotr", "Zieliński");
        group = topicGroupRepository.save(TopicGroup.builder()
                .name("Grupa AI")
                .slug("grupa-ai")
                .description("Wymiana doświadczeń z wdrożeń AI.")
                .coordinator(anna)
                .build());
        membershipRepository.save(new TopicGroupMembership(group, anna));
    }

    @Test
    void listShowsTheMemberCountAndWhetherTheCallerIsAMember() throws Exception {
        topicGroupRepository.save(TopicGroup.builder()
                .name("Grupa chmurowa")
                .slug("grupa-chmurowa")
                .build());

        mockMvc.perform(get(GROUPS).with(JwtTestSupport.asVerifiedAlumn(anna.getKeycloakId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Grupa chmurowa"))
                .andExpect(jsonPath("$.content[0].memberCount").value(0))
                .andExpect(jsonPath("$.content[0].member").value(false))
                .andExpect(jsonPath("$.content[1].name").value("Grupa AI"))
                .andExpect(jsonPath("$.content[1].description").value("Wymiana doświadczeń z wdrożeń AI."))
                .andExpect(jsonPath("$.content[1].memberCount").value(1))
                .andExpect(jsonPath("$.content[1].member").value(true));
    }

    @Test
    void alumnJoinsWithoutApprovalAndJoiningAgainChangesNothing() throws Exception {
        join(piotr).andExpect(status().isNoContent());
        join(piotr).andExpect(status().isNoContent());

        listAs(piotr)
                .andExpect(jsonPath("$.content[0].memberCount").value(2))
                .andExpect(jsonPath("$.content[0].member").value(true));
    }

    @Test
    void memberLeavesAndLeavingAgainChangesNothing() throws Exception {
        join(piotr).andExpect(status().isNoContent());

        leave(piotr).andExpect(status().isNoContent());
        leave(piotr).andExpect(status().isNoContent());

        listAs(piotr)
                .andExpect(jsonPath("$.content[0].memberCount").value(1))
                .andExpect(jsonPath("$.content[0].member").value(false));
    }

    @Test
    void coordinatorCannotLeave() throws Exception {
        leave(anna)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Topic group coordinator cannot leave"));

        listAs(anna).andExpect(jsonPath("$.content[0].member").value(true));
    }

    @Test
    void membersSeeEachOtherInJoinOrderWithoutHiddenNames() throws Exception {
        piotr.setShowName(false);
        join(piotr).andExpect(status().isNoContent());

        members(JwtTestSupport.asVerifiedAlumn(anna.getKeycloakId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(anna.getId().toString()))
                .andExpect(jsonPath("$[0].firstName").value("Anna"))
                .andExpect(jsonPath("$[0].joinedAt").exists())
                .andExpect(jsonPath("$[1].id").value(piotr.getId().toString()))
                .andExpect(jsonPath("$[1].firstName").doesNotExist())
                .andExpect(jsonPath("$[1].lastName").doesNotExist());
    }

    @Test
    void onlyMembersAndAdminsSeeTheMembers() throws Exception {
        members(JwtTestSupport.asVerifiedAlumn(piotr.getKeycloakId())).andExpect(status().isForbidden());

        members(asClubAdmin(user("Adam", "Admin"))).andExpect(status().isOk());
    }

    @Test
    void clubAdminJoinsLikeAnAlumn() throws Exception {
        User admin = user("Adam", "Admin");

        mockMvc.perform(put(GROUPS + "/{id}/members/me", group.getId())
                        .with(asClubAdmin(admin))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(GROUPS).with(asClubAdmin(admin)))
                .andExpect(jsonPath("$.content[0].memberCount").value(2))
                .andExpect(jsonPath("$.content[0].member").value(true));
    }

    @Test
    void unknownGroupReturns404() throws Exception {
        UUID unknown = UUID.randomUUID();
        RequestPostProcessor alumn = JwtTestSupport.asVerifiedAlumn(piotr.getKeycloakId());

        mockMvc.perform(put(GROUPS + "/{id}/members/me", unknown).with(alumn).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(GROUPS + "/{id}/members/me", unknown).with(alumn).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(GROUPS + "/{id}/members", unknown).with(alumn)).andExpect(status().isNotFound());
    }

    @Test
    void onlyVerifiedAlumniSeeTheGroups() throws Exception {
        mockMvc.perform(get(GROUPS).with(JwtTestSupport.asUser())).andExpect(status().isForbidden());
    }

    private ResultActions listAs(User caller) throws Exception {
        return mockMvc.perform(get(GROUPS).with(JwtTestSupport.asVerifiedAlumn(caller.getKeycloakId())))
                .andExpect(status().isOk());
    }

    private ResultActions join(User caller) throws Exception {
        return mockMvc.perform(put(GROUPS + "/{id}/members/me", group.getId())
                .with(JwtTestSupport.asVerifiedAlumn(caller.getKeycloakId()))
                .with(csrf()));
    }

    private ResultActions leave(User caller) throws Exception {
        return mockMvc.perform(delete(GROUPS + "/{id}/members/me", group.getId())
                .with(JwtTestSupport.asVerifiedAlumn(caller.getKeycloakId()))
                .with(csrf()));
    }

    private ResultActions members(RequestPostProcessor caller) throws Exception {
        return mockMvc.perform(get(GROUPS + "/{id}/members", group.getId()).with(caller));
    }

    private static RequestPostProcessor asClubAdmin(User admin) {
        return jwt().jwt(token -> token.subject(admin.getKeycloakId()))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_VERIFIED_ALUMN"), new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private User user(String firstName, String lastName) {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return userRepository.save(user);
    }
}
