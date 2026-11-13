"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  ArrowLeft,
  ArrowRight,
  CalendarClock,
  ChartColumn,
  ListChecks,
  Pencil,
  Plus,
  Trash2,
  Users,
} from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import {
  getListSurveysQueryKey,
  useDeleteSurvey,
  useListSurveys,
  type AdminSurveyListItemResponse,
} from "@pkka/api";
import {
  SURVEYS_PAGE_SIZE,
  formatDateTime,
  formatQuestionCount,
  formatSubmissionCount,
  formatSurveyCount,
  isAdmin,
  surveyPhase,
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
import { SurveyPhaseBadge } from "@/components/admin/survey-phase-badge";
import { LabeledDetails } from "@/components/labeled-details";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth-context";
import { ADMIN_SURVEYS_PATH, adminSurveyEditHref, adminSurveyHref } from "@/lib/survey-paths";

export function AdminSurveysList() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);
  const [page, setPage] = useState(0);
  const [surveyToDelete, setSurveyToDelete] = useState<AdminSurveyListItemResponse | null>(null);

  useEffect(() => {
    if (!isLoading && !admin) {
      router.replace("/dashboard");
    }
  }, [admin, isLoading, router]);

  const {
    data: response,
    isLoading: isListLoading,
    isError,
  } = useListSurveys(
    { page, size: SURVEYS_PAGE_SIZE },
    { query: { enabled: admin, placeholderData: (previous) => previous } },
  );

  const deleteSurvey = useDeleteSurvey({
    mutation: {
      onSuccess: () => {
        // Removing the last card on a page would leave an empty page with no pager.
        if (response?.status === 200 && response.data.content?.length === 1) {
          setPage((prev) => Math.max(0, prev - 1));
        }
        queryClient.invalidateQueries({ queryKey: getListSurveysQueryKey() });
        setSurveyToDelete(null);
      },
    },
  });

  if (isLoading || !admin) {
    return (
      <div className="px-4 py-10 md:px-10 md:py-20">
        <Skeleton className="mx-auto h-40 w-full max-w-3xl rounded-lg" />
      </div>
    );
  }

  const pageData = response?.status === 200 ? response.data : undefined;
  const surveys = pageData?.content ?? [];
  const totalElements = pageData?.totalElements ?? 0;
  const totalPages = pageData?.totalPages ?? 0;
  const currentPage = pageData?.number ?? page;
  const firstShown = currentPage * (pageData?.size ?? SURVEYS_PAGE_SIZE) + 1;
  const lastShown = firstShown + surveys.length - 1;

  return (
    <div>
      <section className="bg-background px-4 pt-10 pb-6 md:px-10 md:pt-16 md:pb-8">
        <div className="mx-auto max-w-[1280px]">
          <div className="flex flex-wrap items-start justify-between gap-6">
            <div className="space-y-6">
              <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
                Ankiety
              </h1>
              <p className="text-muted-foreground max-w-2xl text-base leading-relaxed">
                Pytaj alumnów o zdanie i sprawdzaj zebrane odpowiedzi.
              </p>
            </div>
            <Button asChild className="shrink-0">
              <Link href={`${ADMIN_SURVEYS_PATH}/new`}>
                <Plus data-icon="inline-start" />
                Nowa ankieta
              </Link>
            </Button>
          </div>
        </div>
      </section>

      <section className="bg-background px-4 pb-10 md:px-10 md:pb-16">
        <div className="mx-auto max-w-[1280px]">
          {isListLoading ? (
            <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="bg-muted h-64 w-full rounded-none" />
              ))}
            </div>
          ) : isError ? (
            <p className="text-destructive font-medium">Nie udało się załadować ankiet.</p>
          ) : surveys.length === 0 ? (
            <p className="text-muted-foreground">Brak ankiet. Utwórz pierwszą.</p>
          ) : (
            <div className="space-y-8">
              <p className="text-muted-foreground text-sm">
                {totalPages > 1
                  ? `Wyświetlono ${firstShown}–${lastShown} z ${totalElements} ankiet, strona ${currentPage + 1} z ${totalPages}.`
                  : `Wyświetlono ${formatSurveyCount(totalElements)}.`}
              </p>

              <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
                {surveys.map((survey) => {
                  const phase = surveyPhase(survey.status, survey.endsAt);
                  const answered = survey.submissionCount > 0;

                  return (
                    <Card
                      key={survey.id}
                      className="bg-muted flex h-full flex-col gap-6 overflow-visible rounded-none p-6 shadow-none ring-0"
                    >
                      <SurveyPhaseBadge phase={phase} />
                      <h2 className="font-heading text-foreground text-lg leading-snug font-semibold break-words">
                        {survey.title}
                      </h2>
                      <LabeledDetails
                        items={[
                          {
                            icon: CalendarClock,
                            label: phase === "ended" ? "Zakończona" : "Termin",
                            value: formatDateTime(survey.endsAt),
                          },
                          {
                            icon: ListChecks,
                            label: "Pytania",
                            value: formatQuestionCount(survey.questionCount),
                          },
                          {
                            icon: Users,
                            label: "Odzew",
                            value:
                              phase === "draft"
                                ? "jeszcze nieopublikowana"
                                : formatSubmissionCount(survey.submissionCount),
                          },
                        ]}
                      />
                      <div className="mt-auto flex flex-wrap items-center gap-2 pt-2">
                        <Button asChild variant="outline" size="sm">
                          <Link href={adminSurveyHref(survey.id)}>
                            <ChartColumn data-icon="inline-start" />
                            {phase === "draft" ? "Podgląd" : "Wyniki"}
                          </Link>
                        </Button>
                        {/* Answers point at questions by id, so both are off once anyone has answered. */}
                        {answered ? null : (
                          <>
                            <Button asChild variant="outline" size="sm">
                              <Link href={adminSurveyEditHref(survey.id)}>
                                <Pencil data-icon="inline-start" />
                                Edytuj
                              </Link>
                            </Button>
                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              className="text-destructive hover:text-destructive"
                              onClick={() => setSurveyToDelete(survey)}
                            >
                              <Trash2 data-icon="inline-start" />
                              Usuń
                            </Button>
                          </>
                        )}
                      </div>
                    </Card>
                  );
                })}
              </div>

              {totalPages > 1 ? (
                <div className="flex items-center justify-between gap-4 pt-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={currentPage <= 0}
                    onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                  >
                    <ArrowLeft data-icon="inline-start" />
                    Poprzednia
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => setPage((prev) => prev + 1)}
                  >
                    Następna
                    <ArrowRight data-icon="inline-end" />
                  </Button>
                </div>
              ) : null}
            </div>
          )}
        </div>
      </section>

      <AlertDialog
        open={surveyToDelete !== null}
        onOpenChange={(open) => {
          if (!open) {
            setSurveyToDelete(null);
            // Otherwise a failed delete would greet the next survey's dialog.
            deleteSurvey.reset();
          }
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Usunąć ankietę?</AlertDialogTitle>
            <AlertDialogDescription>
              „{surveyToDelete?.title}” zostanie usunięta razem z pytaniami. Tej operacji nie można
              cofnąć.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deleteSurvey.isError ? (
            <p className="text-destructive text-sm font-medium">
              Nie udało się usunąć ankiety. Jeśli ktoś ją w międzyczasie wypełnił, nie można już jej
              usunąć.
            </p>
          ) : null}
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteSurvey.isPending}>Anuluj</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={deleteSurvey.isPending}
              onClick={(clickEvent) => {
                clickEvent.preventDefault();
                if (surveyToDelete) {
                  deleteSurvey.mutate({ id: surveyToDelete.id });
                }
              }}
            >
              {deleteSurvey.isPending ? "Usuwanie…" : "Usuń ankietę"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
