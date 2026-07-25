package pl.edu.agh.backend.user.alumni;

import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;
import pl.edu.agh.backend.user.UserSpecifications;
import pl.edu.agh.backend.user.alumni.dto.AlumniListItemResponse;

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

    public Page<AlumniListItemResponse> search(String query, Set<UUID> tagIds, Pageable pageable) {
        Specification<User> spec =
                Specification.where(UserSpecifications.matchesQuery(query)).and(UserSpecifications.hasAnyTagId(tagIds));

        Page<User> page = userRepository.findAll(spec, capPageSize(pageable));

        // Mapped here, while the transaction (and Hibernate session) is still open: `from()` reads
        // user.getTags(), a LAZY collection. It is intentionally *not* eagerly fetched in the query above
        // (e.g. via @EntityGraph) — combining a collection fetch join with Pageable would make Hibernate
        // paginate in memory instead of in SQL (HHH000104: firstResult/maxResults with collection fetch).
        // Instead, User.tags' existing @BatchSize(30) batches these per-user lazy loads for the whole page
        // into one (or a couple of) extra queries, so this stays free of both the N+1 and the in-memory
        // pagination problems.
        return page.map(AlumniListItemResponse::from);
    }

    private Pageable capPageSize(Pageable pageable) {
        if (pageable.getPageSize() <= MAX_PAGE_SIZE) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
    }
}
