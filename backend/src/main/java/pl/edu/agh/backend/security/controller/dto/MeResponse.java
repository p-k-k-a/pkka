package pl.edu.agh.backend.security.controller.dto;

import java.util.List;

public record MeResponse(String username, String name, String email, List<String> roles) {}
