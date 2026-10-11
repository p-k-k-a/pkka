package pl.edu.agh.backend.notifications;

public class DeviceTokenConflictException extends RuntimeException {
    public DeviceTokenConflictException(String token) {
        super("Push token %s is registered to a different user".formatted(token));
    }
}
