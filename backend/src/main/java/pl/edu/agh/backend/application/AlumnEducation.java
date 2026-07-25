package pl.edu.agh.backend.application;

import java.time.ZoneOffset;

public record AlumnEducation(Integer graduationYear, String fieldOfStudy, Integer alumnSince) {

    public static final AlumnEducation EMPTY = new AlumnEducation(null, null, null);

    public static AlumnEducation from(Application approvedApplication) {
        if (approvedApplication == null) {
            return EMPTY;
        }
        Integer alumnSince = approvedApplication.getReviewedAt() == null
                ? null
                : approvedApplication.getReviewedAt().atZone(ZoneOffset.UTC).getYear();
        return new AlumnEducation(
                approvedApplication.getGraduationYear(), approvedApplication.getFieldOfStudy(), alumnSince);
    }
}
