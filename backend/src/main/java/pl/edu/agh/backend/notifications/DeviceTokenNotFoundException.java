package pl.edu.agh.backend.notifications;

public class DeviceTokenNotFoundException extends RuntimeException {
    public DeviceTokenNotFoundException(String installationId) {
        super("Device with installation id %s not found".formatted(installationId));
    }
}
