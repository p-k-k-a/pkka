package pl.edu.agh.backend.notifications;

import java.util.Map;

/**
 * {@code details.error} stays a String: the published error set and Expo's own SDK disagree on its members, so an
 * enum would fail to deserialize the first time Expo adds one.
 */
public record ExpoPushTicket(String status, String id, String message, Map<String, Object> details) {

    private static final String DEVICE_NOT_REGISTERED = "DeviceNotRegistered";

    public boolean ok() {
        return "ok".equals(status);
    }

    /** The only error that means the token is permanently dead rather than this send being wrong. */
    public boolean deviceGone() {
        return details != null && DEVICE_NOT_REGISTERED.equals(details.get("error"));
    }
}
