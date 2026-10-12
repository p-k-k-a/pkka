package pl.edu.agh.backend.notifications;

public class DeviceTokenNotFoundException extends RuntimeException {
    public DeviceTokenNotFoundException(String token) {
        super("Device with push token %s not found".formatted(token));
    }
}
