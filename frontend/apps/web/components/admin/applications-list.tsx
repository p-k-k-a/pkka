"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowRight } from "lucide-react";
import { List1Status, useList1 } from "@pkka/api";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { SectionShell } from "@/components/content/section-shell";
import { StatusBadge } from "@/components/applications/status-badge";
import { useAuth } from "@/lib/auth-context";
import { isAdmin } from "@/lib/roles";
import { facultyLabel, studyTypeLabel } from "@/lib/application-labels";
import { formatPublishedAt } from "@/lib/format-published-at";

export function ApplicationsList() {
  const router = useRouter();
  const { isAuthenticated, isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);

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
    isLoading: isListLoading,
    isError,
  } = useList1(
    { status: List1Status.UNDER_REVIEW, size: 50 },
    { query: { enabled: isAuthenticated && admin } },
  );

  if (isLoading || !isAuthenticated || !admin) {
    return (
      <SectionShell title="Wnioski" as="section">
        <Skeleton className="h-40 w-full max-w-3xl rounded-2xl" />
      </SectionShell>
    );
  }

  const applications = response?.data?.content ?? [];

  return (
    <SectionShell
      title="Wnioski"
      description="Nierozpatrzone wnioski weryfikacyjne oczekujące na decyzję."
      as="section"
    >
      {isListLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-28 w-full rounded-2xl" />
          ))}
        </div>
      ) : isError ? (
        <p className="text-destructive font-medium">Nie udało się załadować wniosków.</p>
      ) : applications.length === 0 ? (
        <p className="text-muted-foreground">Brak nierozpatrzonych wniosków.</p>
      ) : (
        <div className="space-y-4">
          {applications.map((item) => {
            const { application } = item;
            const { dateLabel } = formatPublishedAt(application.createdAt);

            return (
              <Link
                key={application.id}
                href={`/dashboard/applications/${application.id}`}
                className="group focus-visible:ring-ring block rounded-xl focus-visible:ring-2 focus-visible:outline-none"
              >
                <Card className="hover:bg-muted/30 gap-3 p-5 transition-colors md:p-6">
                  <div className="flex items-start justify-between gap-4">
                    <div className="min-w-0 space-y-1">
                      <p className="text-muted-foreground text-xs font-semibold tracking-widest uppercase">
                        Złożono: {dateLabel}
                      </p>
                      <h2 className="text-foreground truncate text-lg font-bold">
                        {application.fieldOfStudy}
                      </h2>
                      <p className="text-muted-foreground text-sm">
                        {facultyLabel(application.faculty)} · {studyTypeLabel(application.studyType)}{" "}
                        · Rocznik {application.graduationYear}
                      </p>
                    </div>
                    <StatusBadge status={application.status} />
                  </div>
                  <div className="text-foreground flex items-center gap-1.5 text-xs font-bold tracking-widest uppercase">
                    Szczegóły
                    <ArrowRight className="size-3.5 transition-transform group-hover:translate-x-0.5" />
                  </div>
                </Card>
              </Link>
            );
          })}
        </div>
      )}
    </SectionShell>
  );
}
