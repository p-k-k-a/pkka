package pl.edu.agh.backend.legal;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.legal.dto.LegalDocumentResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LegalDocumentService {

    private final LegalDocumentRepository documentRepository;
    private final Clock clock;

    public List<LegalDocumentResponse> current() {
        return documentRepository.findAllCurrent(Instant.now(clock)).stream()
                .sorted(Comparator.comparing(LegalDocument::getType))
                .map(LegalDocumentResponse::from)
                .toList();
    }

    public LegalDocumentResponse current(LegalDocumentType type) {
        return documentRepository
                .findCurrent(type, Instant.now(clock))
                .map(LegalDocumentResponse::from)
                .orElseThrow(() -> new LegalDocumentNotFoundException(type));
    }
}
