import { TopicGroupRequestStatus, type TopicGroupRequestResponse } from "@pkka/api";
import { formatEventDateLong } from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { RequestStatusBadge } from "@/components/topic-groups/request-status-badge";

type RequestCardProps = {
  request: TopicGroupRequestResponse;
  onPropose: () => void;
};

export function RequestCard({ request, onPropose }: RequestCardProps) {
  const { status } = request;
  const underReview = status === TopicGroupRequestStatus.UNDER_REVIEW;
  const dateLabel =
    underReview || !request.reviewedAt
      ? `Złożony ${formatEventDateLong(request.createdAt)}`
      : `Rozpatrzony ${formatEventDateLong(request.reviewedAt)}`;

  return (
    <Card className="gap-4 p-6 md:p-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-muted-foreground text-xs font-bold tracking-wider uppercase">
          Twój wniosek
        </p>
        <RequestStatusBadge status={status} />
      </div>

      <div className="space-y-1">
        <h2 className="font-heading text-foreground text-[23px] leading-tight font-semibold">
          {request.name}
        </h2>
        <p className="text-muted-foreground text-xs font-medium">{dateLabel}</p>
      </div>

      {underReview ? (
        <>
          <p className="text-muted-foreground text-sm leading-relaxed whitespace-pre-line">
            {request.description}
          </p>
          <p className="border-border text-muted-foreground border-t pt-4 text-sm leading-relaxed">
            Decyzja administracji pojawi się w tym miejscu. Kolejny wniosek złożysz po rozpatrzeniu
            tego.
          </p>
        </>
      ) : null}

      {status === TopicGroupRequestStatus.REJECTED ? (
        <div className="border-destructive/30 bg-destructive/10 flex flex-col gap-3 rounded-lg border p-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0 space-y-1">
            <p className="text-destructive text-sm font-semibold">Powód odrzucenia</p>
            <p className="text-foreground text-sm leading-relaxed">
              {request.rejectionReason?.trim()
                ? request.rejectionReason
                : "Nie podano szczegółowego powodu."}
            </p>
          </div>
          <Button type="button" size="sm" className="shrink-0 self-start" onClick={onPropose}>
            Zaproponuj ponownie
          </Button>
        </div>
      ) : null}

      {status === TopicGroupRequestStatus.APPROVED ? (
        <>
          <p className="text-muted-foreground text-sm leading-relaxed">
            Grupa została utworzona, a Ty jesteś jej właścicielem.
          </p>
          <Button type="button" variant="link" className="self-start px-0" onClick={onPropose}>
            Zaproponuj kolejną grupę
          </Button>
        </>
      ) : null}
    </Card>
  );
}
