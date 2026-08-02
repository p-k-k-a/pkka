package pl.edu.agh.backend.alumni;

import jakarta.persistence.criteria.Subquery;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import pl.edu.agh.backend.application.Application;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.user.User;

/**
 * Filters specific to what makes a {@link User} a genuine, visible member of the alumni directory —
 * as opposed to {@link pl.edu.agh.backend.user.UserSpecifications}, which covers generic profile
 * fields. Kept separate so {@code user.UserSpecifications} has no dependency on the {@code application}
 * package.
 */
@UtilityClass
class AlumniSpecifications {

    /**
     * Restricts to users who actually have an approved application — i.e. real verified alumni, not
     * every row in {@code users} (which also covers pending/rejected applicants and staff accounts
     * that only ever authenticated). Applied unconditionally by the directory, not exposed as a
     * toggleable filter.
     */
    Specification<User> isApprovedAlumnus() {
        return (root, query, cb) -> {
            Subquery<Long> approvedApplication = query.subquery(Long.class);
            var application = approvedApplication.from(Application.class);
            approvedApplication
                    .select(cb.literal(1L))
                    .where(
                            cb.equal(application.get("applicant"), root),
                            cb.equal(application.get("status"), ApplicationStatus.APPROVED));
            return cb.exists(approvedApplication);
        };
    }

    /** Matches alumni whose approved application has the given graduation year; {@code null} means "don't filter". */
    Specification<User> hasGraduationYear(Integer graduationYear) {
        return (root, query, cb) -> {
            if (graduationYear == null) {
                return null;
            }
            Subquery<Long> approvedApplication = query.subquery(Long.class);
            var application = approvedApplication.from(Application.class);
            approvedApplication
                    .select(cb.literal(1L))
                    .where(
                            cb.equal(application.get("applicant"), root),
                            cb.equal(application.get("status"), ApplicationStatus.APPROVED),
                            cb.equal(application.get("graduationYear"), graduationYear));
            return cb.exists(approvedApplication);
        };
    }
}
