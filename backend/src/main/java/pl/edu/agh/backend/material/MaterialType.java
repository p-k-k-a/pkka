package pl.edu.agh.backend.material;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum MaterialType {
    RECORDING,
    PRESENTATION,
    OTHER,
}
