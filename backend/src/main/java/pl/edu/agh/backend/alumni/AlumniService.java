package pl.edu.agh.backend.alumni;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.application.AlumnEducation;
import pl.edu.agh.backend.application.Application;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;
import pl.edu.agh.backend.user.UserSpecifications;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlumniService {

    /**
     * Hard ceiling on page size, independent of what the client asks for. Spring's own default cap (2000, see
     * spring.data.web.pageable.max-page-size) is far too generous for a directory that is meant to be browsed by
     * humans, so we clamp locally rather than raise that global setting (which would also affect every other
     * paginated endpoint in the app).
     */
    static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    public Page<AlumniListItemResponse> search(
            String query, Set<UUID> tagIds, Boolean mentor, Integer graduationYear, Pageable pageable) {
        Specification<User> spec = Specification.where(UserSpecifications.matchesQuery(query))
                .and(UserSpecifications.hasAnyTagId(tagIds))
                .and(UserSpecifications.isMentor(mentor))
                // The directory only ever shows genuine (approved) alumni, never every row of `users`
                // (which also covers pending/rejected applicants and staff who merely logged in once).
                .and(AlumniSpecifications.isApprovedAlumnus())
                .and(AlumniSpecifications.hasGraduationYear(graduationYear));

        Page<User> page = userRepository.findAll(spec, capPageSize(pageable));

        // Mapped here, while the transaction (and Hibernate session) is still open: `from()` reads
        // user.getTags(), a LAZY collection. It is intentionally *not* eagerly fetched in the query above
        // (e.g. via @EntityGraph) — combining a collection fetch join with Pageable would make Hibernate
        // paginate in memory instead of in SQL (HHH000104: firstResult/maxResults with collection fetch).
        // Instead, User.tags' existing @BatchSize(30) batches these per-user lazy loads for the whole page
        // into one (or a couple of) extra queries, so this stays free of both the N+1 and the in-memory
        // pagination problems.
        Map<UUID, Integer> graduationYearsByUserId = graduationYearsByUserId(page.getContent());
        return page.map(user -> AlumniListItemResponse.from(user, graduationYearsByUserId.get(user.getId())));
    }

    public AlumniProfileResponse getProfile(UUID id) {
        User user = userRepository.findWithTagsById(id).orElseThrow(() -> new AlumniNotFoundException(id));
        Application approvedApplication = applicationRepository
                .findFirstByApplicantIdAndStatusOrderByReviewedAtDesc(id, ApplicationStatus.APPROVED)
                // A user that never had an application approved is not a visible alumnus in this
                // directory, same invariant as the list endpoint — treat it as "not found" rather than
                // leaking a bare user row with empty education facts.
                .orElseThrow(() -> new AlumniNotFoundException(id));
        return AlumniProfileResponse.from(user, AlumnEducation.from(approvedApplication));
    }

    /** One batched query for the whole page instead of one per row — same rationale as {@code User.tags}. */
    private Map<UUID, Integer> graduationYearsByUserId(List<User> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<UUID> userIds = users.stream().map(User::getId).toList();
        return applicationRepository.findByApplicantIdInAndStatus(userIds, ApplicationStatus.APPROVED).stream()
                .collect(Collectors.toMap(
                        application -> application.getApplicant().getId(), Application::getGraduationYear));
    }

    private Pageable capPageSize(Pageable pageable) {
        if (pageable.getPageSize() <= MAX_PAGE_SIZE) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
    }
}
