package pl.edu.agh.backend.storage;

import io.swagger.v3.oas.annotations.media.Schema;

/** What an admin is uploading a file for. Alumni upload only their own avatar, through the profile API. */
@Schema(enumAsRef = true)
public enum AdminUploadPurpose {
    MATERIAL(UploadCategory.MATERIAL),
    EVENT_COVER(UploadCategory.EVENT_COVER);

    private final UploadCategory category;

    AdminUploadPurpose(UploadCategory category) {
        this.category = category;
    }

    UploadCategory category() {
        return category;
    }
}
