"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { ApiError, useGetMine, type ApplicationResponseDto } from "@pkka/api";
import { useAuth } from "@/lib/auth-context";
import { isAdmin } from "@/lib/roles";
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
  const canQuery = isAuthenticated && !admin;

  useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated) {
      router.replace("/");
    } else if (admin) {
      router.replace("/dashboard/applications");
    }
  }, [admin, isAuthenticated, isLoading, router]);

  const {
    data,
    isLoading: isApplicationLoading,
    isError,
    error,
    refetch,
  } = useGetMine({
    query: { enabled: canQuery, retry: false },
  });

  if (isLoading || !isAuthenticated || admin) {
    return (
      <SectionShell title="Zweryfikuj się" as="section">
        <Skeleton className="h-96 w-full max-w-2xl rounded-2xl" />
      </SectionShell>
    );
  }

  const application = data?.data as ApplicationResponseDto | undefined;
  const apiError = error as unknown as ApiError | null;
  const notFound = isError && apiError?.status === 404;

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
        ) : application ? (
          <Card className="gap-5 p-6 md:p-8">
            <div className="flex items-center justify-between gap-4">
              <h2 className="text-foreground text-lg font-bold">Twój wniosek</h2>
              <StatusBadge status={application.status} />
            </div>
            <p className="text-muted-foreground text-sm">
              Twój wniosek został złożony. Poniżej znajdują się przesłane dane.
            </p>
            <ApplicationSummary application={application} />
          </Card>
        ) : (
          <VerificationForm onSubmitted={() => refetch()} />
        )}
      </div>
    </SectionShell>
  );
}
