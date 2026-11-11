package pl.edu.agh.backend.security.handler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.regex.Pattern;

public final class LoginReturnPath {

    public static final String PARAMETER = "returnTo";
    public static final String SESSION_ATTRIBUTE = LoginReturnPath.class.getName();

    private static final int MAX_LENGTH = 512;
    // A single leading slash followed by printable ASCII with no backslash: rules out "//host", "/\host" and
    // control characters, so the result can only ever be a path on the web app's own origin.
    private static final Pattern SAFE_PATH = Pattern.compile("^/(?![/\\\\])[\\x21-\\x7E&&[^\\\\]]*$");

    private LoginReturnPath() {}

    public static Optional<String> sanitize(String candidate) {
        if (candidate == null || candidate.length() > MAX_LENGTH) {
            return Optional.empty();
        }
        if (!SAFE_PATH.matcher(candidate).matches()) {
            return Optional.empty();
        }
        try {
            URI uri = new URI(candidate);
            return uri.getScheme() == null && uri.getRawAuthority() == null ? Optional.of(candidate) : Optional.empty();
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }
}
