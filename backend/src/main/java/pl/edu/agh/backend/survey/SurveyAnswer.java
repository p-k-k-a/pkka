package pl.edu.agh.backend.survey;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "survey_answers",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_survey_answers_submission_question",
                        columnNames = {"submission_id", "question_id"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private SurveySubmission submission;

    /**
     * Denormalized alongside {@link #question}: the database enforces, via a composite foreign
     * key to {@code survey_questions(id, survey_id)}, that the question actually belongs to this
     * survey. This is defense-in-depth — {@code SurveyService} already rejects a question id that
     * isn't part of the target survey — but without this column the constraint could only be
     * enforced in application code.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private SurveyQuestion question;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;
}
