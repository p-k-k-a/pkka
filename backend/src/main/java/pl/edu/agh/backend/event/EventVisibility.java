package pl.edu.agh.backend.event;

import java.util.EnumSet;
import java.util.Set;
import lombok.experimental.UtilityClass;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.Roles;

@UtilityClass
public class EventVisibility {

    /** The audiences the caller belongs to. {@code SPECIFIC_GROUP} is nobody's until groups exist. */
    public Set<Audience> audiencesOf(Caller caller) {
        return caller.hasRole(Roles.VERIFIED_ALUMN)
                ? EnumSet.of(Audience.PUBLIC, Audience.ALL_ALUMNI)
                : EnumSet.of(Audience.PUBLIC);
    }

    public boolean isVisibleTo(Event event, Caller caller) {
        return audiencesOf(caller).contains(event.getAudience());
    }
}
