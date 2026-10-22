package pl.edu.agh.backend.legal;

import static java.util.stream.Collectors.toMap;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.legal.dto.ConsentResponse;
import pl.edu.agh.backend.legal.dto.LegalDocumentResponse;
import pl.edu.agh.backend.legal.dto.MyConsentsResponse;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsentService {

    private final LegalDocumentRepository documentRepository;
    private final ConsentRecordRepository consentRepository;
    private final CallerUserService callerUserService;
    private final Clock clock;

    public MyConsentsResponse mine(Caller caller) {
        return callerUserService
                .findId(caller)
                .map(userId -> mine(userId, Instant.now(clock)))
                .orElseGet(() -> new MyConsentsResponse(List.of(), List.of()));
    }

    /**
     * Grants consent to the current version of a document. Accepting a version already accepted is a no-op,
     * so a retried request or a double click does not fail.
     */
    @Transactional
    public ConsentResponse grant(Caller caller, UUID documentId) {
        User user = callerUserService.getOrCreate(caller);
        Instant now = Instant.now(clock);
        LegalDocument document = documentRepository.findAllCurrent(now).stream()
                .filter(current -> current.getId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> ConsentConflictException.outdatedDocument(documentId));
        ConsentSource source = latestActiveByType(user.getId()).containsKey(document.getType())
                ? ConsentSource.REACCEPTANCE
                : ConsentSource.SETTINGS;
        return ConsentResponse.from(record(user, document, source, now));
    }

    /** Only optional consents can be withdrawn here; withdrawing one never given is a no-op. */
    @Transactional
    public void withdraw(Caller caller, LegalDocumentType type) {
        if (type.isRequired()) {
            throw ConsentConflictException.notWithdrawable(type);
        }
        Instant now = Instant.now(clock);
        callerUserService
                .findId(caller)
                .ifPresent(userId -> consentRepository.findActiveByUserId(userId).stream()
                        .filter(consent -> consent.getDocument().getType() == type)
                        .forEach(consent -> consent.withdraw(now)));
    }

    /**
     * The documents an application accepts, checked before anything is saved: each must be a version in
     * force at {@code now}, and together they must cover every required type.
     */
    public List<LegalDocument> requireCurrentForApplication(Collection<UUID> documentIds, Instant now) {
        Map<UUID, LegalDocument> current = documentRepository.findAllCurrent(now).stream()
                .collect(toMap(LegalDocument::getId, Function.identity()));
        List<LegalDocument> accepted = documentIds.stream()
                .map(id -> {
                    LegalDocument document = current.get(id);
                    if (document == null) {
                        throw ConsentConflictException.outdatedDocument(id);
                    }
                    return document;
                })
                .toList();

        Set<LegalDocumentType> missing = EnumSet.noneOf(LegalDocumentType.class);
        Arrays.stream(LegalDocumentType.values())
                .filter(LegalDocumentType::isRequired)
                .forEach(missing::add);
        accepted.forEach(document -> missing.remove(document.getType()));
        if (!missing.isEmpty()) {
            throw new MissingRequiredConsentException(missing);
        }
        return accepted;
    }

    @Transactional
    public void recordForApplication(User user, List<LegalDocument> documents, Instant now) {
        documents.forEach(document -> record(user, document, ConsentSource.APPLICATION, now));
    }

    private ConsentRecord record(User user, LegalDocument document, ConsentSource source, Instant now) {
        return consentRepository
                .findByUserIdAndDocumentIdAndWithdrawnAtIsNull(user.getId(), document.getId())
                .orElseGet(() -> consentRepository.save(ConsentRecord.builder()
                        .user(user)
                        .document(document)
                        .source(source)
                        .grantedAt(now)
                        .build()));
    }

    private MyConsentsResponse mine(UUID userId, Instant now) {
        Map<LegalDocumentType, ConsentRecord> latest = latestActiveByType(userId);
        Map<LegalDocumentType, Integer> lastRequiringReacceptance =
                documentRepository.findByRequiresReacceptanceTrueAndPublishedAtLessThanEqual(now).stream()
                        .collect(toMap(LegalDocument::getType, LegalDocument::getVersion, Math::max));

        List<LegalDocumentResponse> pending = latest.entrySet().stream()
                .filter(entry -> lastRequiringReacceptance.getOrDefault(entry.getKey(), 0)
                        > entry.getValue().getDocument().getVersion())
                .map(entry -> documentRepository.findCurrent(entry.getKey(), now))
                .flatMap(Optional::stream)
                .map(LegalDocumentResponse::from)
                .toList();

        List<ConsentResponse> consents =
                latest.values().stream().map(ConsentResponse::from).toList();
        return new MyConsentsResponse(consents, pending);
    }

    /** Older versions stay active after a newer one is accepted, so only the newest per type counts. */
    private Map<LegalDocumentType, ConsentRecord> latestActiveByType(UUID userId) {
        return consentRepository.findActiveByUserId(userId).stream()
                .collect(toMap(
                        consent -> consent.getDocument().getType(),
                        Function.identity(),
                        (a, b) ->
                                a.getDocument().getVersion() >= b.getDocument().getVersion() ? a : b,
                        () -> new EnumMap<>(LegalDocumentType.class)));
    }
}
