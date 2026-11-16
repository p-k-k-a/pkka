package pl.edu.agh.backend.storage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every place in the database that can point at a stored file. Plain SQL on purpose: the cleanup must see
 * each row that still exists, soft-deleted events included, and not only what the entities' filters let
 * through. A new column or table that stores an object key has to be added here, or its files get deleted.
 */
@Repository
@RequiredArgsConstructor
class StoredFileReferences {

    private static final List<String> KEY_COLUMNS = List.of(
            "SELECT avatar_key FROM users WHERE avatar_key IS NOT NULL",
            "SELECT cover_image_url FROM events WHERE cover_image_url IS NOT NULL",
            "SELECT cover_image_url FROM posts WHERE cover_image_url IS NOT NULL",
            "SELECT url FROM materials");

    /** A picture placed in a post. Any mention in the content counts, not just a well-formed image. */
    private static final Pattern POST_IMAGE_KEY =
            Pattern.compile(Pattern.quote(UploadCategory.POST_IMAGE.keyPrefix()) + "/[0-9a-f-]{36}\\.[a-z0-9]+");

    private final JdbcTemplate jdbc;

    /** The values of all file columns (object keys, and external URLs nobody will mistake for one). */
    @Transactional(readOnly = true)
    public Set<String> referencedKeys() {
        Set<String> keys = new HashSet<>();
        for (String query : KEY_COLUMNS) {
            keys.addAll(jdbc.queryForList(query, String.class));
        }
        jdbc.query(
                "SELECT content FROM posts WHERE content LIKE ?",
                row -> {
                    Matcher matcher = POST_IMAGE_KEY.matcher(row.getString(1));
                    while (matcher.find()) {
                        keys.add(matcher.group());
                    }
                },
                "%" + UploadCategory.POST_IMAGE.keyPrefix() + "/%");
        return keys;
    }
}
