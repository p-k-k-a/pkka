"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Trash2 } from "lucide-react";
import { toast } from "sonner";
import {
  ApiError,
  getListMyTopicGroupRequestsQueryKey,
  TopicGroupRequestStatus,
  useWithdrawTopicGroupRequest,
  type TopicGroupRequestResponse,
} from "@pkka/api";
import {
  formatEventDateLong,
  topicGroupRequestStatusLabel,
  topicGroupRequestWithdrawErrorMessage,
} from "@pkka/domain";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { StatusBadge } from "@/components/applications/status-badge";

type RequestCardProps = {
  request: TopicGroupRequestResponse;
  onPropose: () => void;
};

export function RequestCard({ request, onPropose }: RequestCardProps) {
  const { status } = request;
  const isUnderReview = status === TopicGroupRequestStatus.UNDER_REVIEW;
  const queryClient = useQueryClient();
  const { mutateAsync: withdrawRequest, isPending: isWithdrawing } = useWithdrawTopicGroupRequest();
  const [confirmingWithdraw, setConfirmingWithdraw] = useState(false);

  const withdrawAndRefresh = async () => {
    try {
      await withdrawRequest({ id: request.id });
    } catch (error) {
      toast.error(
        topicGroupRequestWithdrawErrorMessage(error instanceof ApiError ? error.status : undefined),
      );
    }
    await queryClient.invalidateQueries({ queryKey: getListMyTopicGroupRequestsQueryKey() });
    setConfirmingWithdraw(false);
  };
  const dateLabel =
    isUnderReview || !request.reviewedAt
      ? `Złożony ${formatEventDateLong(request.createdAt)}`
      : `Rozpatrzony ${formatEventDateLong(request.reviewedAt)}`;

  return (
    <Card className="gap-4 p-6 md:p-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-muted-foreground text-xs font-bold tracking-wider uppercase">
          Twój wniosek
        </p>
        <StatusBadge status={status} label={topicGroupRequestStatusLabel(status)} />
      </div>

      <div className="space-y-1">
        <h3 className="font-heading text-foreground text-[23px] leading-tight font-semibold">
          {request.name}
        </h3>
        <p className="text-muted-foreground text-xs font-medium">{dateLabel}</p>
      </div>

      {isUnderReview ? (
        <>
          <p className="text-muted-foreground text-sm leading-relaxed whitespace-pre-line">
            {request.description}
          </p>
          <div className="border-border flex flex-col gap-3 border-t pt-4 sm:flex-row sm:items-start sm:justify-between">
            <p className="text-muted-foreground text-sm leading-relaxed">
              Po zatwierdzeniu grupa pojawi się na liście, a Ty zostaniesz jej koordynatorem.
            </p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="text-destructive hover:text-destructive shrink-0 self-start"
              onClick={() => setConfirmingWithdraw(true)}
            >
              <Trash2 data-icon="inline-start" />
              Wycofaj wniosek
            </Button>
          </div>
          <AlertDialog
            open={confirmingWithdraw}
            onOpenChange={(open) => !open && !isWithdrawing && setConfirmingWithdraw(false)}
          >
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Wycofać wniosek?</AlertDialogTitle>
                <AlertDialogDescription>
                  Wniosek o grupę „{request.name}” zostanie usunięty.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel disabled={isWithdrawing}>Anuluj</AlertDialogCancel>
                <AlertDialogAction
                  variant="destructive"
                  disabled={isWithdrawing}
                  onClick={(clickEvent) => {
                    clickEvent.preventDefault();
                    void withdrawAndRefresh();
                  }}
                >
                  {isWithdrawing ? "Wycofywanie…" : "Wycofaj"}
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
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
    </Card>
  );
}
