"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowRight, ArrowUpRight } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import {
  getGetMyConsentsQueryKey,
  grantConsent,
  useGetMyConsents,
  withdrawConsent,
  type LegalDocumentResponse,
} from "@pkka/api";
import {
  consentErrorMessage,
  isOutdatedDocumentError,
  legalDocumentPath,
  missingRequiredDocuments,
  sortLegalDocuments,
  toggleInSet,
} from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { ConsentCheckboxList } from "@/components/legal/consent-checkbox-list";
import { PrivacyNoticeSummary } from "@/components/legal/privacy-notice-summary";

/**
 * Holds back the dashboard while a document the member accepted has a newer version that must be
 * accepted again. Loading and errors let the page through: a failed check must not lock anyone out.
 */
export function ReacceptanceGate({ children }: { children: React.ReactNode }) {
  const { data: response } = useGetMyConsents();
  const pending = response?.data.pendingReacceptance ?? [];

  if (pending.length === 0) return children;
  return <ReacceptanceScreen pending={sortLegalDocuments(pending)} />;
}

function ReacceptanceScreen({ pending }: { pending: LegalDocumentResponse[] }) {
  const queryClient = useQueryClient();
  const [acceptedIds, setAcceptedIds] = useState<ReadonlySet<string>>(new Set());
  const [saving, setSaving] = useState(false);

  const missing = missingRequiredDocuments(pending, acceptedIds);

  const toggle = (documentId: string) => setAcceptedIds((prev) => toggleInSet(prev, documentId));

  const onSubmit = async () => {
    setSaving(true);
    try {
      for (const document of pending) {
        if (acceptedIds.has(document.id)) {
          await grantConsent({ documentId: document.id });
        } else if (!document.required) {
          // Leaving an optional consent unticked is a no; otherwise it would stay pending.
          await withdrawConsent(document.type);
        }
      }
      toast.success("Dziękujemy, zapisaliśmy Twoje zgody.");
    } catch (error) {
      if (isOutdatedDocumentError(error)) setAcceptedIds(new Set());
      toast.error(consentErrorMessage(error));
    } finally {
      await queryClient.invalidateQueries({ queryKey: getGetMyConsentsQueryKey() });
      setSaving(false);
    }
  };

  return (
    <div className="bg-background px-4 py-10 md:px-10 md:py-20">
      <div className="mx-auto max-w-2xl space-y-10">
        <div className="space-y-3">
          <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
            Zaktualizowaliśmy dokumenty Klubu
          </h1>
          <p className="text-muted-foreground leading-relaxed">
            Zanim przejdziesz dalej, zapoznaj się ze zmianami i zaakceptuj nowe wersje.
          </p>
        </div>

        <ul className="space-y-6">
          {pending.map((document) => {
            const href = legalDocumentPath(document.type);
            return (
              <li key={document.id} className="border-accent space-y-2 border-l-2 pl-6">
                <p className="font-heading text-foreground text-lg font-semibold">
                  {document.title}
                </p>
                <p className="text-muted-foreground text-xs font-semibold tracking-widest uppercase">
                  Wersja {document.version}
                </p>
                {document.changeSummary ? (
                  <p className="text-muted-foreground text-sm leading-relaxed">
                    {document.changeSummary}
                  </p>
                ) : null}
                {href ? (
                  <Link
                    href={href}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-accent inline-flex items-center gap-1 text-sm font-semibold underline"
                  >
                    Przeczytaj pełną treść
                    <ArrowUpRight className="size-4" aria-hidden="true" />
                  </Link>
                ) : null}
              </li>
            );
          })}
        </ul>

        <div className="space-y-4">
          <ConsentCheckboxList
            documents={pending}
            acceptedIds={acceptedIds}
            onToggle={toggle}
            disabled={saving}
          />
          <PrivacyNoticeSummary />
        </div>

        <Button
          size="xl"
          className="gap-2"
          disabled={saving || missing.length > 0}
          onClick={() => void onSubmit()}
        >
          {saving ? "Zapisywanie…" : "Zapisz i przejdź dalej"}
          {saving ? null : <ArrowRight data-icon="inline-end" />}
        </Button>
      </div>
    </div>
  );
}
