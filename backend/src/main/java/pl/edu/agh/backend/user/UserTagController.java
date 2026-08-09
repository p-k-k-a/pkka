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
    @Operation(summary = "List all available alumni skill tags")
    public List<UserTagResponse> listUserTags() {
        return userTagRepository.findAll().stream()
                .map(UserTagResponse::from)
                .sorted(Comparator.comparing(UserTagResponse::name))
                .toList();
    }
}
