import { LegalDocumentType, type LegalDocumentResponse } from "@pkka/api";

export const CLUB_CONTACT_EMAIL = "alumni@informatyka.agh.edu.pl";

// Only the long documents get a page; consent wordings are one sentence and show inline.
const LEGAL_PAGE_PATHS: Partial<Record<LegalDocumentType, string>> = {
  TERMS: "/terms",
  PRIVACY_NOTICE: "/privacy",
};

export const TERMS_PATH = LEGAL_PAGE_PATHS.TERMS!;
export const PRIVACY_PATH = LEGAL_PAGE_PATHS.PRIVACY_NOTICE!;

export function legalDocumentPath(type: LegalDocumentType): string | undefined {
  return LEGAL_PAGE_PATHS[type];
}

const LEGAL_DOCUMENT_TYPE_LABELS: Record<LegalDocumentType, string> = {
  TERMS: "Regulamin",
  PRIVACY_NOTICE: "Informacja o przetwarzaniu danych osobowych",
  MEMBERSHIP_CONSENT: "Zgoda na przetwarzanie danych osobowych",
  NEWSLETTER_CONSENT: "Newsletter",
};

export function legalDocumentTypeLabel(type: LegalDocumentType): string {
  return LEGAL_DOCUMENT_TYPE_LABELS[type];
}

// The order the documents are read and accepted in, on every screen that lists them.
const DOCUMENT_ORDER: LegalDocumentType[] = [
  LegalDocumentType.TERMS,
  LegalDocumentType.PRIVACY_NOTICE,
  LegalDocumentType.MEMBERSHIP_CONSENT,
  LegalDocumentType.NEWSLETTER_CONSENT,
];

export function sortLegalDocuments<T extends { type: LegalDocumentType }>(documents: readonly T[]) {
  return [...documents].sort(
    (a, b) => DOCUMENT_ORDER.indexOf(a.type) - DOCUMENT_ORDER.indexOf(b.type),
  );
}

/** A copy of the set with the id ticked or unticked, for checkbox state kept in React. */
export function toggleInSet(ids: ReadonlySet<string>, id: string): ReadonlySet<string> {
  const next = new Set(ids);
  if (!next.delete(id)) next.add(id);
  return next;
}

/** Required documents not yet ticked; empty means the application can be sent. */
export function missingRequiredDocuments(
  documents: readonly LegalDocumentResponse[],
  acceptedIds: ReadonlySet<string>,
): LegalDocumentResponse[] {
  return documents.filter((document) => document.required && !acceptedIds.has(document.id));
}

function conflictReason(error: unknown): unknown {
  const { status, body } = (error ?? {}) as { status?: unknown; body?: unknown };
  return status === 409 ? (body as { reason?: unknown } | null | undefined)?.reason : undefined;
}

/**
 * A newer version was published while the form was open. Reads the error structurally, like
 * `eventRegistrationErrorMessage`, since this package must not import the client's mutator.
 */
export function isOutdatedDocumentError(error: unknown): boolean {
  return conflictReason(error) === "OUTDATED_DOCUMENT";
}

export const OUTDATED_DOCUMENTS_MESSAGE =
  "Dokumenty zostały właśnie zaktualizowane. Przeczytaj nową wersję i zaznacz zgody ponownie.";

export function consentErrorMessage(error: unknown): string {
  const { status } = (error ?? {}) as { status?: unknown };
  if (status === 401) return "Sesja wygasła. Zaloguj się ponownie.";
  if (isOutdatedDocumentError(error)) return OUTDATED_DOCUMENTS_MESSAGE;
  if (conflictReason(error) === "NOT_WITHDRAWABLE") {
    return `Tej zgody nie można wycofać w aplikacji. Napisz na ${CLUB_CONTACT_EMAIL}.`;
  }
  return "Nie udało się zapisać zgody. Spróbuj ponownie.";
}
