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
        User user = callerUserService.getOrCreate(caller);
        releaseFromPreviousInstallation(request.token(), installationId, user);
        String platform = request.platform().name();
        deviceTokenRepository.insertIfAbsent(user.getId(), installationId, request.token(), platform);
        deviceTokenRepository.updateByInstallationId(user.getId(), installationId, request.token(), platform);
    }

    /**
     * Android hands a reinstalled app the token its previous install had, so the caller's own stale row is
     * released. A row belonging to somebody else is refused rather than released — otherwise anyone holding a
     * token could evict that device and redirect its notifications to themselves.
     */
    private void releaseFromPreviousInstallation(String token, String installationId, User user) {
        deviceTokenRepository
                .findByToken(token)
                .filter(held -> !held.getInstallationId().equals(installationId))
                .ifPresent(held -> {
                    if (!held.getUser().getId().equals(user.getId())) {
                        throw new DeviceTokenConflictException(token);
                    }
                    deviceTokenRepository.delete(held);
                    deviceTokenRepository.flush();
                });
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
