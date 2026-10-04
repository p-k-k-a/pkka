package pl.edu.agh.backend.notifications.expo;

import java.util.Map;

record ExpoPushTicket(String status, String id, String message, Map<String, Object> details) {

    boolean ok() {
        return "ok".equals(status);
    }

    boolean deviceGone() {
        return details != null && "DeviceNotRegistered".equals(details.get("error"));
    }
}
