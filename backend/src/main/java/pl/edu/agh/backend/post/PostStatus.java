package pl.edu.agh.backend.post;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum PostStatus {
    DRAFT,
    PUBLISHED,
}
