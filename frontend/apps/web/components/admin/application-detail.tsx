"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import {
  ApiError,
  getGetQueryKey,
  getList1QueryKey,
  useApprove,
  useGet,
  useReject,
} from "@pkka/api";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { DetailBackLink } from "@/components/content/detail-back-link";
import { DetailHeader } from "@/components/content/detail-header";
import { ApplicationSummary } from "@/components/applications/application-summary";
import { StatusBadge } from "@/components/applications/status-badge";
import { useAuth } from "@/lib/auth-context";
import { isAdmin } from "@/lib/roles";

const APPLICATIONS_PATH = "/dashboard/applications";

export function ApplicationDetail({ id }: { id: string }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { isAuthenticated, isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);

  const [showRejectForm, setShowRejectForm] = useState(false);
  const [rejectionReason, setRejectionReason] = useState("");
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated) {
      router.replace("/");
    } else if (!admin) {
      router.replace("/dashboard");
    }
  }, [admin, isAuthenticated, isLoading, router]);

  const {
    data: response,
    isLoading: isDetailLoading,
    isError,
  } = useGet(id, { query: { enabled: isAuthenticated && admin } });

  const invalidateAndReturn = () => {
    queryClient.invalidateQueries({ queryKey: getList1QueryKey() });
    queryClient.invalidateQueries({ queryKey: getGetQueryKey(id) });
    router.push(APPLICATIONS_PATH);
  };

  const handleActionError = () => {
    setActionError("Nie udało się zapisać decyzji. Odśwież stronę i spróbuj ponownie.");
  };

  const approveMutation = useApprove<ApiError>({
    mutation: { onSuccess: invalidateAndReturn, onError: handleActionError },
  });
  const rejectMutation = useReject<ApiError>({
    mutation: { onSuccess: invalidateAndReturn, onError: handleActionError },
  });

  const isMutating = approveMutation.isPending || rejectMutation.isPending;

  if (isLoading || !isAuthenticated || !admin) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-8">
        <Skeleton className="mb-6 h-10 w-48" />
        <Skeleton className="h-96 w-full rounded-2xl" />
      </div>
    );
  }

  const item = response?.data;

  if (isDetailLoading) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-8">
        <Skeleton className="mb-6 h-10 w-48" />
        <Skeleton className="h-96 w-full rounded-2xl" />
      </div>
    );
  }

  if (isError || !item) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16 text-center">
        <h1 className="mb-2 text-2xl font-bold">Nie znaleziono wniosku</h1>
        <p className="text-muted-foreground mb-8">
          Ten wniosek nie istnieje lub został już rozpatrzony.
        </p>
        <DetailBackLink href={APPLICATIONS_PATH} label="Wróć do wniosków" />
      </div>
    );
  }

  const { application } = item;

  return (
    <div className="mx-auto max-w-3xl px-6 py-8">
      <DetailHeader backHref={APPLICATIONS_PATH} title="Szczegóły wniosku" />

      <div className="mb-6 flex items-center justify-between gap-4">
        <div className="min-w-0">
          <h1 className="text-foreground text-2xl font-extrabold tracking-tight">
            {application.fieldOfStudy}
          </h1>
          <p className="text-muted-foreground mt-1 text-sm">ID wnioskodawcy: {item.applicantId}</p>
        </div>
        <StatusBadge status={application.status} />
      </div>

      <Card className="mb-6 p-6 md:p-8">
        <ApplicationSummary application={application} />
      </Card>

      {actionError ? (
        <p className="text-destructive mb-4 text-sm font-medium">{actionError}</p>
      ) : null}

      {showRejectForm ? (
        <div className="border-border bg-card space-y-4 rounded-xl border p-6">
          <div className="space-y-2">
            <Label htmlFor="rejectionReason">Powód odrzucenia (opcjonalnie)</Label>
            <Textarea
              id="rejectionReason"
              maxLength={1000}
              placeholder="Powód zostanie pokazany wnioskodawcy"
              value={rejectionReason}
              onChange={(event) => setRejectionReason(event.target.value)}
            />
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <Button
              variant="destructive"
              size="xl"
              className="flex-1 rounded-xl font-semibold"
              disabled={isMutating}
              onClick={() => {
                setActionError(null);
                rejectMutation.mutate({
                  id,
                  data: rejectionReason.trim() ? { reason: rejectionReason.trim() } : undefined,
                });
              }}
            >
              {rejectMutation.isPending ? "Odrzucanie…" : "Potwierdź odrzucenie"}
            </Button>
            <Button
              variant="outline"
              size="xl"
              className="flex-1 rounded-xl font-semibold"
              disabled={isMutating}
              onClick={() => {
                setShowRejectForm(false);
                setRejectionReason("");
              }}
            >
              Anuluj
            </Button>
          </div>
        </div>
      ) : (
        <div className="flex flex-col gap-3 sm:flex-row">
          <Button
            size="xl"
            className="flex-1 rounded-xl font-semibold"
            disabled={isMutating}
            onClick={() => {
              setActionError(null);
              approveMutation.mutate({ id });
            }}
          >
            {approveMutation.isPending ? "Akceptowanie…" : "Akceptuj"}
          </Button>
          <Button
            variant="destructive"
            size="xl"
            className="flex-1 rounded-xl font-semibold"
            disabled={isMutating}
            onClick={() => {
              setActionError(null);
              setShowRejectForm(true);
            }}
          >
            Odrzuć
          </Button>
        </div>
      )}
    </div>
  );
}
