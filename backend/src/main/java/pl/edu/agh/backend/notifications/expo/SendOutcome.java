package pl.edu.agh.backend.notifications.expo;

import java.util.Map;
import java.util.Set;

public record SendOutcome(Map<String, String> tickets, Set<String> refusedTokens) {}
