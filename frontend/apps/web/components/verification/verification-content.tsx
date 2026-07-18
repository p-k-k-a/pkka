"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight } from "lucide-react";
import {
  ApiError,
  ApplicationResponseDtoStatus,
  useGetMine,
  type ApplicationResponseDto,
} from "@pkka/api";
import { useAuth } from "@/lib/auth-context";
import { isAdmin, isVerifiedAlumn } from "@/lib/roles";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { SectionShell } from "@/components/content/section-shell";
import { ApplicationSummary } from "@/components/applications/application-summary";
import { StatusBadge } from "@/components/applications/status-badge";
import { VerificationForm } from "@/components/verification/verification-form";

export function VerificationContent() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);
  const verifiedByRole = isVerifiedAlumn(user?.roles);
  const canQuery = isAuthenticated && !admin && !verifiedByRole;
  const [isReapplying, setIsReapplying] = useState(false);

  const {
    data,
    isLoading: isApplicationLoading,
    isError,
    error,
    refetch,
  } = useGetMine({
    query: { enabled: canQuery, retry: false },
  });

  const application = data?.data as ApplicationResponseDto | undefined;
  const verifiedByApplication = application?.status === ApplicationResponseDtoStatus.APPROVED;
  const isVerified = verifiedByRole || verifiedByApplication;
  const isRejected = application?.status === ApplicationResponseDtoStatus.REJECTED;

  useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated) {
      router.replace("/");
      return;
    }
    if (admin) {
      router.replace("/dashboard/applications");
      return;
    }
    if (isVerified) {
      router.replace("/dashboard");
    }
  }, [admin, isAuthenticated, isLoading, isVerified, router]);

  if (isLoading || !isAuthenticated || admin || isVerified) {
    return (
      <SectionShell title="Zweryfikuj się" as="section">
        <Skeleton className="h-96 w-full max-w-2xl rounded-2xl" />
      </SectionShell>
    );
  }

  const apiError = error as unknown as ApiError | null;
  const notFound = isError && apiError?.status === 404;

  const reapplying = isRejected && isReapplying;
  const showRejectedSummary = Boolean(application && isRejected && !reapplying);
  const showSubmittedSummary = Boolean(application && !isRejected);
  const showForm = !application || reapplying;

  return (
    <SectionShell
      title="Zweryfikuj się"
      description="Wypełnij wniosek, aby potwierdzić status absolwenta WI AGH i uzyskać dostęp do pełnej oferty Klubu Alumna."
      as="section"
    >
      <div className="max-w-2xl">
        {isApplicationLoading ? (
          <Skeleton className="h-96 w-full rounded-2xl" />
        ) : isError && !notFound ? (
          <p className="text-destructive font-medium">
            Nie udało się sprawdzić statusu wniosku. Odśwież stronę i spróbuj ponownie.
          </p>
        ) : (
          <div className="space-y-6">
            {showRejectedSummary && application ? (
              <Card className="gap-5 p-6 md:p-8">
                <div className="flex items-center justify-between gap-4">
                  <h2 className="font-heading text-foreground text-lg font-semibold">
                    Twój wniosek
                  </h2>
                  <StatusBadge status={application.status} />
                </div>

                <div className="border-destructive/30 bg-destructive/10 space-y-3 rounded-lg border p-4">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div className="min-w-0 space-y-1">
                      <p className="text-destructive text-sm font-semibold">Powód odrzucenia</p>
                      <p className="text-foreground text-sm leading-relaxed">
                        {application.rejectionReason?.trim()
                          ? application.rejectionReason
                          : "Nie podano szczegółowego powodu."}
                      </p>
                    </div>
                    <Button
                      type="button"
                      size="sm"
                      className="shrink-0 gap-1.5 self-start"
                      onClick={() => setIsReapplying(true)}
                    >
                      Złóż ponownie
                      <ArrowRight data-icon="inline-end" />
                    </Button>
                  </div>
                </div>

                <p className="text-muted-foreground text-sm">
                  Poniżej znajdują się dane z odrzuconego wniosku. Po kliknięciu „Złóż ponownie”
                  możesz wysłać nową wersję.
                </p>
                <ApplicationSummary application={application} hideRejectionReason />
              </Card>
            ) : null}

            {showSubmittedSummary && application ? (
              <Card className="gap-5 p-6 md:p-8">
                <div className="flex items-center justify-between gap-4">
                  <h2 className="font-heading text-foreground text-lg font-semibold">
                    Twój wniosek
                  </h2>
                  <StatusBadge status={application.status} />
                </div>
                <p className="text-muted-foreground text-sm">
                  Twój wniosek został złożony. Poniżej znajdują się przesłane dane.
                </p>
                <ApplicationSummary application={application} />
              </Card>
            ) : null}

            {showForm ? (
              <div className="space-y-3">
                {isRejected ? (
                  <h2 className="font-heading text-foreground text-lg font-semibold">
                    Nowy wniosek
                  </h2>
                ) : null}
                <VerificationForm
                  onSubmitted={async () => {
                    setIsReapplying(false);
                    await refetch();
                  }}
                />
              </div>
            ) : null}
          </div>
        )}
      </div>
    </SectionShell>
  );
}
