"use client";

import { useState } from "react";
import Link from "next/link";
import { FileCheck } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import {
  LegalDocumentType,
  getGetMyConsentsQueryKey,
  grantConsent,
  useGetMyConsents,
  useListCurrentLegalDocuments,
  withdrawConsent,
} from "@pkka/api";
import {
  CLUB_CONTACT_EMAIL,
  consentErrorMessage,
  formatPublishedAt,
  legalDocumentPath,
  legalDocumentTypeLabel,
  sortLegalDocuments,
} from "@pkka/domain";
import { ProfileAside, ProfileAsideItem } from "@/components/profile/profile-aside";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Switch } from "@/components/ui/switch";

export function PrivacyConsentsAside() {
  const queryClient = useQueryClient();
  const myConsents = useGetMyConsents();
  const documents = useListCurrentLegalDocuments();
  const [saving, setSaving] = useState(false);

  const consents = sortLegalDocuments(myConsents.data?.data.consents ?? []);
  const accepted = consents.filter(
    (consent) => consent.type !== LegalDocumentType.NEWSLETTER_CONSENT,
  );
  const subscribed = consents.some(
    (consent) => consent.type === LegalDocumentType.NEWSLETTER_CONSENT,
  );
  const currentNewsletter = documents.data?.data.find(
    (document) => document.type === LegalDocumentType.NEWSLETTER_CONSENT,
  );

  const onNewsletterChange = async (checked: boolean) => {
    setSaving(true);
    try {
      if (checked) {
        if (!currentNewsletter) throw new Error("Newsletter consent not loaded");
        await grantConsent({ documentId: currentNewsletter.id });
        toast.success("Zapisano Cię do newslettera.");
      } else {
        await withdrawConsent(LegalDocumentType.NEWSLETTER_CONSENT);
        toast.success("Wypisano Cię z newslettera.");
      }
    } catch (error) {
      toast.error(consentErrorMessage(error));
    } finally {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: getGetMyConsentsQueryKey() }),
        documents.refetch(),
      ]);
      setSaving(false);
    }
  };

  return (
    <ProfileAside title="Prywatność i zgody">
      {myConsents.isPending ? (
        <Skeleton className="h-24 w-full rounded-lg" />
      ) : myConsents.isError ? (
        <p className="text-destructive text-sm">Nie udało się wczytać zgód.</p>
      ) : (
        <>
          {accepted.length === 0 ? (
            <p className="text-muted-foreground text-sm">
              Dokumenty Klubu zaakceptujesz, składając wniosek o weryfikację.
            </p>
          ) : (
            accepted.map((consent) => {
              const href = legalDocumentPath(consent.type);
              const label = `Wersja ${consent.version} · zaakceptowano ${formatPublishedAt(consent.grantedAt).dateLabel}`;
              return (
                <ProfileAsideItem
                  key={consent.documentId}
                  icon={FileCheck}
                  label={legalDocumentTypeLabel(consent.type)}
                >
                  {href ? (
                    <Link
                      href={href}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="hover:underline"
                    >
                      {label}
                    </Link>
                  ) : (
                    label
                  )}
                </ProfileAsideItem>
              );
            })
          )}

          <div className="flex items-center justify-between gap-4">
            <Label htmlFor="newsletter-consent" className="text-foreground text-sm font-semibold">
              Newsletter e-mailem
            </Label>
            <Switch
              id="newsletter-consent"
              checked={subscribed}
              disabled={saving || (!subscribed && !currentNewsletter)}
              onCheckedChange={(checked) => void onNewsletterChange(checked)}
            />
          </div>

          <p className="text-muted-foreground text-[13px] leading-relaxed">
            Aby wystąpić z Klubu i wycofać zgodę na przetwarzanie danych, napisz na{" "}
            <a href={`mailto:${CLUB_CONTACT_EMAIL}`} className="text-accent underline">
              {CLUB_CONTACT_EMAIL}
            </a>
            .
          </p>
        </>
      )}
    </ProfileAside>
  );
}
