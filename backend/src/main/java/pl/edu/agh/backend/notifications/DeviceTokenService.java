package pl.edu.agh.backend.notifications;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.notifications.dto.RegisterDeviceRequest;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final CallerUserService callerUserService;

    /** Idempotent, so a first registration and a rotated token are the same call. */
    @Transactional
    public void register(Caller caller, String installationId, RegisterDeviceRequest request) {
        deviceTokenRepository.releaseToken(request.token(), installationId);

        User user = callerUserService.getOrCreate(caller);
        DeviceToken device = deviceTokenRepository
                .findByInstallationId(installationId)
                .orElseGet(() ->
                        DeviceToken.builder().installationId(installationId).build());
        device.setUser(user);
        device.setToken(request.token());
        device.setPlatform(request.platform());
        deviceTokenRepository.save(device);
    }

    /** Someone else's installation reports 404 rather than 403, so a device id cannot be probed. */
    @Transactional
    public void unregister(Caller caller, String installationId) {
        UUID userId =
                callerUserService.findId(caller).orElseThrow(() -> new DeviceTokenNotFoundException(installationId));
        DeviceToken device = deviceTokenRepository
                .findByInstallationId(installationId)
                .filter(candidate -> candidate.getUser().getId().equals(userId))
                .orElseThrow(() -> new DeviceTokenNotFoundException(installationId));
        deviceTokenRepository.delete(device);
    }
}
