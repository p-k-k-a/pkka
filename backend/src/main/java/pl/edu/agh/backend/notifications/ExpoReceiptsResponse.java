package pl.edu.agh.backend.notifications;

import java.util.Map;

public record ExpoReceiptsResponse(Map<String, ExpoPushTicket> data) {}
