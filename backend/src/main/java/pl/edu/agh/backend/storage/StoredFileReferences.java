package pl.edu.agh.backend.storage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every place in the database that can point at a stored file. Plain SQL on purpose: the cleanup must see
 * each row that still exists, soft-deleted events included, and not only what the entities' filters let
 * through. A new column that stores an object key has to be added here, or its files get deleted;
 * {@code StoredFileReferencesTest} fails for a new column that looks like one until it is accounted for.
 */
@Repository
@RequiredArgsConstructor
class StoredFileReferences {

    /** table.column of every column holding one object key (or an external URL) per row. */
    static final List<String> KEY_COLUMNS =
            List.of("users.avatar_key", "events.cover_image_url", "posts.cover_image_url", "materials.url");

    /** Markdown in which pictures uploaded for a post are mentioned by their keys. */
    static final String CONTENT_COLUMN = "posts.content";

    private final JdbcTemplate jdbc;

    /** The values of all file columns (object keys, and external URLs nobody will mistake for one). */
    @Transactional(readOnly = true)
    public Set<String> referencedKeys() {
        Set<String> keys = new HashSet<>();
        for (String column : KEY_COLUMNS) {
            keys.addAll(jdbc.queryForList(
                    "SELECT " + columnOf(column) + " FROM " + tableOf(column) + " WHERE " + columnOf(column)
                            + " IS NOT NULL",
                    String.class));
        }
        jdbc.query(
                "SELECT " + columnOf(CONTENT_COLUMN) + " FROM " + tableOf(CONTENT_COLUMN) + " WHERE "
                        + columnOf(CONTENT_COLUMN) + " LIKE ?",
                row -> {
                    // Any mention counts, wherever it stands: inside an image, a link, raw HTML, a signed URL.
                    Matcher matcher = UploadCategory.POST_IMAGE.keyPattern().matcher(row.getString(1));
                    while (matcher.find()) {
                        keys.add(matcher.group());
                    }
                },
                "%" + UploadCategory.POST_IMAGE.keyPrefix() + "/%");
        return keys;
    }

    /** The same question for one file, asked fresh; used right before that file is deleted. */
    @Transactional(readOnly = true)
    public boolean isReferenced(String key) {
        for (String column : KEY_COLUMNS) {
            Boolean found = jdbc.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM " + tableOf(column) + " WHERE " + columnOf(column) + " = ?)",
                    Boolean.class,
                    key);
            if (Boolean.TRUE.equals(found)) {
                return true;
            }
        }
        // Keys are made of hex digits, dashes, slashes and a dot: none of them is a LIKE wildcard.
        Boolean mentioned = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM " + tableOf(CONTENT_COLUMN) + " WHERE " + columnOf(CONTENT_COLUMN)
                        + " LIKE ?)",
                Boolean.class,
                "%" + key + "%");
        return Boolean.TRUE.equals(mentioned);
    }

    private static String tableOf(String column) {
        return column.substring(0, column.indexOf('.'));
    }

    private static String columnOf(String column) {
        return column.substring(column.indexOf('.') + 1);
    }
}
