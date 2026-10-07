package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum StudyType {
    BACHELOR,
    MASTER,
    DOCTORAL,
    POSTGRADUATE
}
