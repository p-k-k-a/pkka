import type { ApplicationResponseDto } from "@pkka/api";
import {
  facultyLabel,
  meetingPreferenceLabel,
  studyTypeLabel,
  consentLabel,
} from "@/lib/application-labels";
import { formatPublishedAt } from "@/lib/format-published-at";

function SummaryRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="bg-muted flex flex-col gap-1 rounded-lg px-4 py-3 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
      <span className="text-muted-foreground text-sm font-medium">{label}</span>
      <span className="text-foreground text-sm font-semibold sm:max-w-[60%] sm:text-right">
        {value}
      </span>
    </div>
  );
}

function yesNo(value: boolean) {
  return value ? "Tak" : "Nie";
}

function formatDateTime(value?: string) {
  if (!value) return "—";
  const { dateLabel, timeLabel } = formatPublishedAt(value);
  return `${dateLabel}, ${timeLabel}`;
}

export function ApplicationSummary({ application }: { application: ApplicationResponseDto }) {
  const interests = application.interests ?? [];
  const meetingPreferences = application.meetingPreferences ?? [];
  const consents = application.consents ?? [];

  return (
    <div className="space-y-3">
      <SummaryRow label="Wydział" value={facultyLabel(application.faculty)} />
      <SummaryRow label="Kierunek studiów" value={application.fieldOfStudy} />
      <SummaryRow label="Rodzaj studiów" value={studyTypeLabel(application.studyType)} />
      <SummaryRow label="Rok ukończenia" value={application.graduationYear} />
      <SummaryRow label="Telefon" value={application.phoneNumber} />
      <SummaryRow
        label="Zainteresowania"
        value={interests.length > 0 ? interests.join(", ") : "—"}
      />
      <SummaryRow
        label="Preferowane formy spotkań"
        value={
          meetingPreferences.length > 0
            ? meetingPreferences.map(meetingPreferenceLabel).join(", ")
            : "—"
        }
      />
      <SummaryRow label="Chęć współtworzenia klubu" value={yesNo(application.coCreationInterest)} />
      <SummaryRow label="Newsletter" value={yesNo(application.newsletterSubscription)} />
      <SummaryRow
        label="Zgody"
        value={
          consents.length > 0 ? (
            <span className="flex flex-col gap-1 sm:items-end">
              {consents.map((consent) => (
                <span key={consent}>{consentLabel(consent)}</span>
              ))}
            </span>
          ) : (
            "—"
          )
        }
      />
      <SummaryRow label="Data złożenia" value={formatDateTime(application.createdAt)} />
      {application.reviewedAt ? (
        <SummaryRow label="Data rozpatrzenia" value={formatDateTime(application.reviewedAt)} />
      ) : null}
      {application.rejectionReason ? (
        <SummaryRow label="Powód odrzucenia" value={application.rejectionReason} />
      ) : null}
    </div>
  );
}
