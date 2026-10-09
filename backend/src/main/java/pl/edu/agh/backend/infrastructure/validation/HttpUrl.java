package pl.edu.agh.backend.infrastructure.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A link users will open: http or https only, so a {@code javascript:} or {@code data:} URL can't be stored and
 * rendered as a clickable link. One definition for the request DTO and the entity, so the two can't drift apart.
 * {@code null} is valid; pair with {@code @NotBlank} when the link is required.
 */
@Documented
@Constraint(validatedBy = {})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Pattern(regexp = "^https?://.+")
@ReportAsSingleViolation
public @interface HttpUrl {

    String message() default "url must start with http:// or https://";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
