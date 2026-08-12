package pl.edu.agh.backend.application;

import java.time.LocalDate;
import java.time.ZoneOffset;

public record AlumnEducation(Integer graduationYear, String fieldOfStudy, LocalDate alumnSince) {

    public static final AlumnEducation EMPTY = new AlumnEducation(null, null, null);

    public static AlumnEducation from(Application approvedApplication) {
        if (approvedApplication == null) {
            return EMPTY;
        }
        LocalDate alumnSince = approvedApplication.getReviewedAt() == null
                ? null
                : approvedApplication.getReviewedAt().atZone(ZoneOffset.UTC).toLocalDate();
        return new AlumnEducation(
                approvedApplication.getGraduationYear(), approvedApplication.getFieldOfStudy(), alumnSince);
    }
}
