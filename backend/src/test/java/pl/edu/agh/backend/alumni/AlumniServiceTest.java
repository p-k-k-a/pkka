package pl.edu.agh.backend.alumni;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AlumniServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private AlumniService alumniService;

    @Test
    void rejectsUnsupportedSortProperty() {
        assertThatThrownBy(() ->
                        alumniService.search(null, null, null, null, null, PageRequest.of(0, 20, Sort.by("email"))))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void clampsPageSizeToMax() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        alumniService.search(null, null, null, null, null, PageRequest.of(0, 500));

        org.mockito.Mockito.verify(userRepository)
                .findAll(
                        any(Specification.class),
                        org.mockito.ArgumentMatchers.argThat(
                                (Pageable pageable) -> pageable.getPageSize() == AlumniService.MAX_PAGE_SIZE));
    }

    @Test
    void getProfileThrowsWhenNoApprovedApplication() {
        UUID id = UUID.randomUUID();
        when(userRepository.findWithTagsById(id)).thenReturn(java.util.Optional.of(new pl.edu.agh.backend.user.User()));
        when(applicationRepository.findFirstByApplicantIdAndStatusOrderByReviewedAtDesc(
                        id, pl.edu.agh.backend.application.ApplicationStatus.APPROVED))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> alumniService.getProfile(id)).isInstanceOf(AlumniNotFoundException.class);
    }
}
