package pl.edu.agh.backend.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-tags")
@RequiredArgsConstructor
@Tag(name = "User Tags", description = "Available alumni skill tags — requires USER role")
public class UserTagController {

    private final UserTagRepository userTagRepository;

    @GetMapping
    @Operation(
            summary = "List all alumni skill tags with how many listed alumni have each",
            description = "Most used first, then alphabetical")
    public List<UserTagCatalogResponse> listUserTags() {
        return userTagRepository.findCatalog().stream()
                .sorted(Comparator.comparingLong(UserTagCatalogResponse::usageCount)
                        .reversed()
                        .thenComparing(UserTagCatalogResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
