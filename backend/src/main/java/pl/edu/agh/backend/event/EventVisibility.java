package pl.edu.agh.backend.event;

import java.util.EnumSet;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import pl.edu.agh.backend.security.Roles;

/**
 * Which events a caller may see. Registration uses it too, so an event you cannot see is also one you
 * cannot sign up for, and both report the same 404 rather than leaking its existence.
 */
@UtilityClass
public class EventVisibility {

    /** The audiences the caller belongs to. {@code SPECIFIC_GROUP} is nobody's until groups exist. */
    public Set<Audience> audiencesOf(Authentication authentication) {
        return Roles.has(authentication, Roles.VERIFIED_ALUMN)
                ? EnumSet.of(Audience.PUBLIC, Audience.ALL_ALUMNI)
                : EnumSet.of(Audience.PUBLIC);
    }

    public boolean isVisibleTo(Event event, Authentication authentication) {
        return audiencesOf(authentication).contains(event.getAudience());
    }
}
