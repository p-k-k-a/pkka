import type { ApplicationResponse } from "@pkka/api";
import { facultyLabel, formatDateTime, meetingPreferenceLabel, studyTypeLabel } from "@pkka/domain";

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

export function ApplicationSummary({
  application,
  hideRejectionReason = false,
}: {
  application: ApplicationResponse;
  hideRejectionReason?: boolean;
}) {
  const interests = application.interests ?? [];
  const meetingPreferences = application.meetingPreferences ?? [];

  return (
    <div className="space-y-3">
      <SummaryRow label="Wydział" value={facultyLabel(application.faculty)} />
      <SummaryRow label="Kierunek studiów" value={application.fieldOfStudy} />
      <SummaryRow label="Rodzaj studiów" value={studyTypeLabel(application.studyType)} />
      <SummaryRow label="Rok ukończenia" value={application.graduationYear} />
      <SummaryRow label="Telefon" value={application.phoneNumber} />
      <SummaryRow
        label="Zainteresowania"
        value={interests.length > 0 ? interests.join(", ") : "-"}
      />
      <SummaryRow
        label="Preferowane formy spotkań"
        value={
          meetingPreferences.length > 0
            ? meetingPreferences.map(meetingPreferenceLabel).join(", ")
            : "-"
        }
      />
      <SummaryRow
        label="Chęć współtworzenia klubu"
        value={application.coCreationInterest ? "Tak" : "Nie"}
      />
      <SummaryRow label="Data złożenia" value={formatDateTime(application.createdAt)} />
      {application.reviewedAt ? (
        <SummaryRow label="Data rozpatrzenia" value={formatDateTime(application.reviewedAt)} />
      ) : null}
      {!hideRejectionReason && application.rejectionReason ? (
        <SummaryRow label="Powód odrzucenia" value={application.rejectionReason} />
      ) : null}
    </div>
  );
}
