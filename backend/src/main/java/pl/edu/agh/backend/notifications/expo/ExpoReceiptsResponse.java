package pl.edu.agh.backend.notifications.expo;

import java.util.Map;

record ExpoReceiptsResponse(Map<String, ExpoPushTicket> data) {}
