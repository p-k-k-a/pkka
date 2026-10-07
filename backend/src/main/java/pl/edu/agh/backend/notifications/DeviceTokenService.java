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

    @Transactional
    public void register(Caller caller, String token, RegisterDeviceRequest request) {
        User user = callerUserService.getOrCreate(caller);
        DeviceToken device = deviceTokenRepository
                .findById(token)
                .orElseGet(() -> DeviceToken.builder().token(token).build());
        device.setUser(user);
        device.setPlatform(request.platform());
        deviceTokenRepository.save(device);
    }

    @Transactional
    public void unregister(Caller caller, String token) {
        UUID userId = callerUserService.findId(caller).orElseThrow(() -> new DeviceTokenNotFoundException(token));
        DeviceToken device = deviceTokenRepository
                .findById(token)
                .filter(candidate -> candidate.getUser().getId().equals(userId))
                .orElseThrow(() -> new DeviceTokenNotFoundException(token));
        deviceTokenRepository.delete(device);
    }
}
