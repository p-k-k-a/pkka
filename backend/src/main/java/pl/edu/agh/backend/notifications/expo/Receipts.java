package pl.edu.agh.backend.notifications.expo;

import java.util.Set;

public record Receipts(Set<String> checked, Set<String> deviceGone) {}
