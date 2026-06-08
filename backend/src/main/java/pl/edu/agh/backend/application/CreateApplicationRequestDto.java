package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequestDto(
        @Schema(
                description = "Faculty (wydział)",
                example = "Wydział Informatyki",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 200)
        String faculty,

        @Schema(
                description = "Field of study (kierunek)",
                example = "Informatyka",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 200)
        String fieldOfStudy,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull
        StudyType studyType,

        @Schema(
                description = "Year of graduation (rok ukończenia)",
                example = "2020",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(1919)
        Integer graduationYear) {}
