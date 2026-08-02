package pl.edu.agh.backend.alumni;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.application.Application;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.application.Faculty;
import pl.edu.agh.backend.application.StudyType;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;
import pl.edu.agh.backend.user.UserTag;
import pl.edu.agh.backend.user.UserTagRepository;

/**
 * Covers the {@code GET /api/alumni} paginated/filterable list endpoint. See {@link AlumniProfileIntegrationTest}
 * for the {@code GET /api/alumni/{id}} single-profile endpoint and the own-profile ({@code /api/profiles/me})
 * endpoints — kept in a separate file since they exercise a different slice of the alumni domain.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(AlumniListEndpointTest.TestSecurityBeans.class)
@Transactional
class AlumniListEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTagRepository userTagRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private UserTag pythonTag;
    private UserTag devopsTag;

    private static final int DEFAULT_GRADUATION_YEAR = 2020;

    @BeforeEach
    void setUp() {
        pythonTag = userTagRepository.save(UserTag.builder().name("test-python").build());
        devopsTag = userTagRepository.save(UserTag.builder().name("test-devops").build());
    }

    /**
     * Creates a user with an approved application (graduation year {@link #DEFAULT_GRADUATION_YEAR}) — i.e. a
     * genuine, directory-visible alumnus. Every list test relies on this by default; tests that specifically
     * exercise the "unverified users are excluded" invariant create a bare {@link User} directly instead.
     */
    private User newUser(String keycloakId, String position, String company, UserTag... tags) {
        return newUser(keycloakId, position, company, DEFAULT_GRADUATION_YEAR, tags);
    }

    private User newUser(String keycloakId, String position, String company, int graduationYear, UserTag... tags) {
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setCurrentPosition(position);
        user.setCompany(company);
        user.getTags().addAll(Set.of(tags));
        user = userRepository.save(user);
        approveApplication(user, graduationYear);
        return user;
    }

    private void approveApplication(User applicant, int graduationYear) {
        Application application = Application.builder()
                .applicant(applicant)
                .status(ApplicationStatus.UNDER_REVIEW)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(graduationYear)
                .phoneNumber("+48123456789")
                .build();
        application.approve(applicant);
        applicationRepository.save(application);
    }

    /**
     * Flushes and detaches all managed entities so that the next query hits the database instead of
     * returning objects already cached in this session's identity map. Without this, entities created in
     * test setup would still have their {@code tags} collection already initialized in memory (we just
     * populated it ourselves before saving), which would silently hide a broken lazy-loading/batching
     * setup: the assertions would pass for the wrong reason.
     */
    private void flushAndClearSession() {
        entityManager.flush();
        entityManager.clear();
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor verifiedAlumn() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_VERIFIED_ALUMN"));
    }

    @Test
    void listWithoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/alumni")).andExpect(status().isUnauthorized());
    }

    @Test
    void listWithWrongRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/alumni").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAsVerifiedAlumn_returnsOk() throws Exception {
        newUser(UUID.randomUUID().toString(), "Backend Engineer", "Acme", pythonTag);
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni").with(verifiedAlumn())).andExpect(status().isOk());
    }

    @Test
    void pagination_splitsResultsAcrossPages() throws Exception {
        // Scoped with a q filter unique to this test so that pre-existing rows (e.g. the dev-profile
        // seed author created once at application startup by PostDataInitializer, outside of any
        // test transaction) cannot affect the total count.
        String marker = "PagerTestCo-" + UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            newUser(UUID.randomUUID().toString(), "Engineer " + i, marker);
        }
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni")
                        .param("q", marker)
                        .param("page", "0")
                        .param("size", "2")
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));

        mockMvc.perform(get("/api/alumni")
                        .param("q", marker)
                        .param("page", "2")
                        .param("size", "2")
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void filterByTagId_matchesAnyOfTheGivenTags() throws Exception {
        User pythonDev = newUser(UUID.randomUUID().toString(), "Python Dev", "Acme", pythonTag);
        User devopsEngineer = newUser(UUID.randomUUID().toString(), "DevOps Engineer", "Acme", devopsTag);
        newUser(UUID.randomUUID().toString(), "Frontend Dev", "Acme");
        // Forces the tags returned below to come from a genuine (batched) lazy load off a fresh User
        // proxy rather than the in-memory collection we populated ourselves a few lines above — see
        // flushAndClearSession() javadoc.
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni")
                        .param(
                                "tagIds",
                                pythonTag.getId().toString(),
                                devopsTag.getId().toString())
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].id")
                        .value(containsInAnyOrder(
                                pythonDev.getId().toString(),
                                devopsEngineer.getId().toString())))
                .andExpect(jsonPath("$.content[?(@.id=='%s')].tags[0].name".formatted(pythonDev.getId()))
                        .value("test-python"))
                .andExpect(jsonPath("$.content[?(@.id=='%s')].tags[0].name".formatted(devopsEngineer.getId()))
                        .value("test-devops"));
    }

    @Test
    void filterByUnknownTagId_returnsEmptyPageNotError() throws Exception {
        newUser(UUID.randomUUID().toString(), "Python Dev", "Acme", pythonTag);
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni")
                        .param("tagIds", UUID.randomUUID().toString())
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void filterByMentor_matchesOnlyWillingMentors() throws Exception {
        String marker = "MentorTestCo-" + UUID.randomUUID();
        User mentor = newUser(UUID.randomUUID().toString(), "Staff Engineer", marker);
        mentor.setWillingToMentor(true);
        User nonMentor = newUser(UUID.randomUUID().toString(), "Junior Engineer", marker);
        nonMentor.setWillingToMentor(false);
        userRepository.saveAll(Set.of(mentor, nonMentor));
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni")
                        .param("q", marker)
                        .param("mentor", "true")
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mentor.getId().toString()))
                .andExpect(jsonPath("$.content[0].willingToMentor").value(true));

        mockMvc.perform(get("/api/alumni")
                        .param("q", marker)
                        .param("mentor", "false")
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(nonMentor.getId().toString()));

        // omitted mentor param: both match
        mockMvc.perform(get("/api/alumni").param("q", marker).with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void excludesUsersWithoutApprovedApplication() throws Exception {
        String marker = "UnverifiedTestCo-" + UUID.randomUUID();
        User verified = newUser(UUID.randomUUID().toString(), "Engineer", marker);

        // A bare user row — e.g. someone who merely logged in, or whose application is still under
        // review / was rejected — must never surface in the directory, even though a local User row
        // exists for them.
        User unverified = new User();
        unverified.setKeycloakId(UUID.randomUUID().toString());
        unverified.setCompany(marker);
        userRepository.save(unverified);
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni").param("q", marker).with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(verified.getId().toString()));
    }

    @Test
    void filterByGraduationYearRange_matchesOnlyYearsWithinBounds() throws Exception {
        String marker = "GradYearTestCo-" + UUID.randomUUID();
        User classOf2015 = newUser(UUID.randomUUID().toString(), "Engineer", marker, 2015);
        User classOf2019 = newUser(UUID.randomUUID().toString(), "Engineer", marker, 2019);
        User classOf2021 = newUser(UUID.randomUUID().toString(), "Engineer", marker, 2021);
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni")
                        .param("q", marker)
                        .param("graduationYearFrom", "2019")
                        .param("graduationYearTo", "2021")
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/alumni")
                        .param("q", marker)
                        .param("graduationYearFrom", "2019")
                        .param("graduationYearTo", "2019")
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(classOf2019.getId().toString()))
                .andExpect(jsonPath("$.content[0].graduationYear").value(2019));

        mockMvc.perform(get("/api/alumni")
                        .param("q", marker)
                        .param("graduationYearFrom", "2020")
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(classOf2021.getId().toString()));

        mockMvc.perform(get("/api/alumni")
                        .param("q", marker)
                        .param("graduationYearTo", "2016")
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(classOf2015.getId().toString()));

        mockMvc.perform(get("/api/alumni").param("q", marker).with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/alumni")
                        .param("q", marker)
                        .param("graduationYearFrom", "1990")
                        .param("graduationYearTo", "1999")
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void sortByGraduationYear_ordersByTheDenormalizedUserColumn() throws Exception {
        String marker = "GradSortTestCo-" + UUID.randomUUID();
        newUser(UUID.randomUUID().toString(), "Engineer", marker, 2019);
        newUser(UUID.randomUUID().toString(), "Engineer", marker, 2012);
        newUser(UUID.randomUUID().toString(), "Engineer", marker, 2024);
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni")
                        .param("q", marker)
                        .param("sort", "graduationYear,asc")
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].graduationYear").value(2012))
                .andExpect(jsonPath("$.content[1].graduationYear").value(2019))
                .andExpect(jsonPath("$.content[2].graduationYear").value(2024));

        mockMvc.perform(get("/api/alumni")
                        .param("q", marker)
                        .param("sort", "graduationYear,desc")
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].graduationYear").value(2024))
                .andExpect(jsonPath("$.content[1].graduationYear").value(2019))
                .andExpect(jsonPath("$.content[2].graduationYear").value(2012));
    }

    @Test
    void freeTextSearch_matchesPositionCompanyAndTagName() throws Exception {
        String marker = "FreeTextTest-" + UUID.randomUUID();
        User positionMatch = newUser(UUID.randomUUID().toString(), "Backend Engineer " + marker, "Irrelevant Co");
        User companyMatch = newUser(UUID.randomUUID().toString(), "Designer", "Acme " + marker + " Corp");
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni")
                        .param("q", "backend engineer " + marker)
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(
                        jsonPath("$.content[0].id").value(positionMatch.getId().toString()));

        mockMvc.perform(get("/api/alumni").param("q", "ACME " + marker).with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(
                        jsonPath("$.content[0].id").value(companyMatch.getId().toString()));
    }

    @Test
    void freeTextSearch_matchesTagName() throws Exception {
        User pythonDev = newUser(UUID.randomUUID().toString(), "Engineer", "Acme", pythonTag);
        newUser(UUID.randomUUID().toString(), "Engineer", "Acme", devopsTag);
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni").param("q", "test-python").with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(pythonDev.getId().toString()));
    }

    @Test
    void freeTextSearch_matchesFirstOrLastName() throws Exception {
        String marker = UUID.randomUUID().toString().substring(0, 8);
        User named = newUser(UUID.randomUUID().toString(), "Engineer", "Acme");
        named.setFirstName("Zdzisław" + marker);
        named.setLastName("Kowalski");
        userRepository.save(named);
        newUser(UUID.randomUUID().toString(), "Engineer", "Acme");
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni").param("q", marker).with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(named.getId().toString()));
    }

    @Test
    void freeTextSearch_doesNotMatchNameHiddenByOwner() throws Exception {
        String marker = UUID.randomUUID().toString().substring(0, 8);
        User hidden = newUser(UUID.randomUUID().toString(), "Engineer-" + marker, "Acme");
        hidden.setFirstName("Zdzisław" + marker);
        hidden.setShowName(false);
        userRepository.save(hidden);
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni").param("q", "Zdzisław" + marker).with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // still findable by a non-hidden field (position) — and the list view must null out the hidden name
        mockMvc.perform(get("/api/alumni").param("q", "Engineer-" + marker).with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(hidden.getId().toString()))
                .andExpect(jsonPath("$.content[0].firstName").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void combinedFilters_areCombinedWithAnd() throws Exception {
        User match = newUser(UUID.randomUUID().toString(), "Backend Engineer", "Acme", pythonTag);
        newUser(UUID.randomUUID().toString(), "Backend Engineer", "Acme", devopsTag);
        newUser(UUID.randomUUID().toString(), "Designer", "Acme", pythonTag);
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni")
                        .param("q", "backend")
                        .param("tagIds", pythonTag.getId().toString())
                        .with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(match.getId().toString()));
    }

    @Test
    void search_withNoMatches_returnsEmptyPage() throws Exception {
        newUser(UUID.randomUUID().toString(), "Backend Engineer", "Acme");
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni").param("q", "no-such-thing-anywhere").with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void freeTextSearch_escapesLikeWildcards() throws Exception {
        User discounted = newUser(UUID.randomUUID().toString(), "Sales", "50% Off Corp");
        newUser(UUID.randomUUID().toString(), "Sales", "Full Price Corp");
        flushAndClearSession();

        // A literal '%' in the query must be matched literally, not as a SQL LIKE wildcard that
        // would match every row.
        mockMvc.perform(get("/api/alumni").param("q", "50%").with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(discounted.getId().toString()));
    }

    @Test
    void oversizedPageSize_isClampedToMax() throws Exception {
        newUser(UUID.randomUUID().toString(), "Engineer", "Acme");
        flushAndClearSession();

        mockMvc.perform(get("/api/alumni").param("size", "100000").with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(AlumniService.MAX_PAGE_SIZE));
    }

    @Test
    void negativePage_isTreatedAsFirstPageRatherThanErroring() throws Exception {
        // Spring's Pageable resolver clamps a negative page index to 0 rather than rejecting the
        // request; asserting that behaviour explicitly so a future Spring upgrade that changes it
        // (e.g. to a 400) is caught by this test instead of silently changing API behaviour.
        mockMvc.perform(get("/api/alumni").param("page", "-1").with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0));
    }

    @TestConfiguration
    static class TestSecurityBeans {
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return mock(ClientRegistrationRepository.class);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }
}
