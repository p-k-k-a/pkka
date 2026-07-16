package pl.edu.agh.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.backend.controller.dto.TagResponse;
import pl.edu.agh.backend.event.TagRepository;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Available skill tags — requires USER role")
public class TagController {

    private final TagRepository tagRepository;

    @GetMapping
    @Operation(summary = "List all available skill tags")
    public List<TagResponse> listTags() {
        return tagRepository.findAll().stream()
                .map(TagResponse::from)
                .sorted(Comparator.comparing(TagResponse::name))
                .toList();
    }
}
