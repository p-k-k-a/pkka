package pl.edu.agh.backend.topicgroup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectTopicGroupRequest(
        @Schema(
                description = "Reason shown to the alumn who asked for the group",
                example = "Taka grupa już działa na Discordzie.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 1000)
        String reason) {}
