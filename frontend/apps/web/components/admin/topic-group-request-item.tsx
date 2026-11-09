"use client";

import { useState } from "react";
import Link from "next/link";
import {
  ApiError,
  useApproveTopicGroupRequest,
  useRejectTopicGroupRequest,
  type TopicGroupRequestResponse,
} from "@pkka/api";
import {
  formatEventDateLong,
  TOPIC_GROUP_REJECTION_REASON_MAX_LENGTH,
  topicGroupRequestReviewErrorMessage,
} from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

type TopicGroupRequestItemProps = {
  request: TopicGroupRequestResponse;
  onReviewed: () => void;
};

export function TopicGroupRequestItem({ request, onReviewed }: TopicGroupRequestItemProps) {
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [reason, setReason] = useState("");
  const [actionError, setActionError] = useState<string | null>(null);

  const mutation = {
    onSuccess: onReviewed,
    onError: (error: ApiError) => {
      setActionError(topicGroupRequestReviewErrorMessage(error.status, error.body));
      // Unless the name is taken, a 409 means the request was reviewed in the meantime.
      if (error.status === 409 || error.status === 404) onReviewed();
    },
  };
  const approveMutation = useApproveTopicGroupRequest<ApiError>({ mutation });
  const rejectMutation = useRejectTopicGroupRequest<ApiError>({ mutation });
  const isMutating = approveMutation.isPending || rejectMutation.isPending;

  const { requester } = request;
  const requesterName = [requester.firstName, requester.lastName].filter(Boolean).join(" ");
  const reasonId = `rejection-reason-${request.id}`;

  return (
    <Card className="gap-4 p-5 md:p-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div className="min-w-0 space-y-1">
          <h3 className="font-heading text-foreground text-xl leading-tight font-semibold">
            {request.name}
          </h3>
          <p className="text-muted-foreground text-sm">
            Zgłoszone przez{" "}
            <Link
              href={`/dashboard/admin/alumni/${requester.id}`}
              className="text-accent font-semibold underline-offset-4 hover:underline"
            >
              {requesterName || "alumna bez imienia i nazwiska"}
            </Link>{" "}
            · {formatEventDateLong(request.createdAt)}
          </p>
        </div>

        {showRejectForm ? null : (
          <div className="flex shrink-0 gap-2">
            <Button
              type="button"
              variant="destructive"
              disabled={isMutating}
              onClick={() => {
                setActionError(null);
                setShowRejectForm(true);
              }}
            >
              Odrzuć
            </Button>
            <Button
              type="button"
              disabled={isMutating}
              onClick={() => {
                setActionError(null);
                approveMutation.mutate({ id: request.id });
              }}
            >
              {approveMutation.isPending ? "Zatwierdzanie…" : "Zatwierdź"}
            </Button>
          </div>
        )}
      </div>

      <p className="text-foreground text-sm leading-relaxed whitespace-pre-line">
        {request.description}
      </p>

      {showRejectForm ? (
        <form
          className="border-border space-y-3 border-t pt-4"
          onSubmit={(event) => {
            event.preventDefault();
            if (!reason.trim()) return;
            setActionError(null);
            rejectMutation.mutate({ id: request.id, data: { reason: reason.trim() } });
          }}
        >
          <div className="space-y-2">
            <Label htmlFor={reasonId}>Powód odrzucenia</Label>
            <Textarea
              id={reasonId}
              required
              maxLength={TOPIC_GROUP_REJECTION_REASON_MAX_LENGTH}
              placeholder="Powód zostanie pokazany wnioskodawcy"
              value={reason}
              onChange={(event) => setReason(event.target.value)}
            />
          </div>
          <div className="flex flex-wrap gap-2">
            <Button type="submit" variant="destructive" disabled={isMutating || !reason.trim()}>
              {rejectMutation.isPending ? "Odrzucanie…" : "Potwierdź odrzucenie"}
            </Button>
            <Button
              type="button"
              variant="outline"
              disabled={isMutating}
              onClick={() => {
                setShowRejectForm(false);
                setReason("");
              }}
            >
              Anuluj
            </Button>
          </div>
        </form>
      ) : null}

      {actionError ? (
        <p role="alert" className="text-destructive text-sm font-medium">
          {actionError}
        </p>
      ) : null}
    </Card>
  );
}
