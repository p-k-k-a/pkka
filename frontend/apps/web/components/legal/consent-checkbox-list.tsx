import Link from "next/link";
import { LegalDocumentType, type LegalDocumentResponse } from "@pkka/api";
import { legalDocumentPath } from "@pkka/domain";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";

function DocumentLink({
  document,
  children,
}: {
  document: LegalDocumentResponse;
  children: string;
}) {
  const href = legalDocumentPath(document.type);
  if (!href) return children;
  // A new tab keeps a half-filled form intact.
  return (
    <Link
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className="text-accent font-semibold underline"
    >
      {children}
    </Link>
  );
}

function ConsentText({ document }: { document: LegalDocumentResponse }) {
  switch (document.type) {
    case LegalDocumentType.TERMS:
      return (
        <>
          Akceptuję{" "}
          <DocumentLink document={document}>
            Regulamin Klubu Alumnów Wydziału Informatyki AGH
          </DocumentLink>
          .
        </>
      );
    case LegalDocumentType.PRIVACY_NOTICE:
      return (
        <>
          Potwierdzam zapoznanie się z{" "}
          <DocumentLink document={document}>
            Informacją o przetwarzaniu danych osobowych
          </DocumentLink>
          .
        </>
      );
    default:
      // A consent's wording is the document itself, so the record proves exactly this text.
      return <>{document.content}</>;
  }
}

type ConsentCheckboxListProps = {
  documents: readonly LegalDocumentResponse[];
  acceptedIds: ReadonlySet<string>;
  onToggle: (documentId: string) => void;
  disabled?: boolean;
};

export function ConsentCheckboxList({
  documents,
  acceptedIds,
  onToggle,
  disabled,
}: ConsentCheckboxListProps) {
  return (
    <div className="flex flex-col gap-3">
      {documents.map((document) => (
        <Label
          key={document.id}
          htmlFor={`consent-${document.id}`}
          className="items-start leading-relaxed font-normal"
        >
          <Checkbox
            id={`consent-${document.id}`}
            className="mt-1"
            checked={acceptedIds.has(document.id)}
            disabled={disabled}
            onCheckedChange={() => onToggle(document.id)}
          />
          <span>
            <ConsentText document={document} />
            {document.required ? (
              <>
                <span className="text-destructive" aria-hidden="true">
                  {" "}
                  *
                </span>
                <span className="sr-only"> (wymagane)</span>
              </>
            ) : (
              <span className="text-muted-foreground"> (opcjonalnie)</span>
            )}
          </span>
        </Label>
      ))}
    </div>
  );
}
