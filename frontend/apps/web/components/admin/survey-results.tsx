"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Pencil } from "lucide-react";
import { useGetResults, useGetSurvey, type ApiError } from "@pkka/api";
import { formatDateTime, isAdmin, surveyPhase, type SurveyPhase } from "@pkka/domain";
import { StatTile } from "@/components/admin/statistics/stat-tile";
import { SurveyAvailabilityActions } from "@/components/admin/survey-availability-actions";
import { SurveyPhaseBadge } from "@/components/admin/survey-phase-badge";
import { SurveyQuestionResult } from "@/components/admin/survey-question-result";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth-context";
import { ADMIN_SURVEYS_PATH, adminSurveyEditHref } from "@/lib/survey-paths";

const DEADLINE_HINTS: Record<SurveyPhase, string> = {
  draft: "Szkic, alumni jeszcze go nie widzą.",
  active: "Do tego czasu alumni mogą odpowiadać.",
  ended: "Ankieta nie przyjmuje już odpowiedzi.",
};

export function SurveyResults({ id }: { id: string }) {
  const router = useRouter();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);

  useEffect(() => {
    if (!isLoading && !admin) {
      router.replace("/dashboard");
    }
  }, [admin, isLoading, router]);

  const surveyQuery = useGetSurvey(id, { query: { enabled: admin } });
  const resultsQuery = useGetResults(id, { query: { enabled: admin } });

  if (isLoading || !admin || surveyQuery.isLoading) {
    return (
      <div className="px-4 py-10 md:px-10">
        <div className="mx-auto max-w-[1280px] space-y-6">
          <Skeleton className="h-10 w-64 rounded-lg" />
          <Skeleton className="h-96 w-full rounded-2xl" />
        </div>
      </div>
    );
  }

  const survey = surveyQuery.data?.status === 200 ? surveyQuery.data.data : undefined;
  if (!survey) {
    const notFound = (surveyQuery.error as unknown as ApiError | null)?.status === 404;
    return (
      <div className="px-4 py-16 text-center">
        <h1 className="font-heading text-foreground mb-2 text-[28px] font-semibold">
          {notFound ? "Nie znaleziono ankiety" : "Nie udało się załadować ankiety"}
        </h1>
        <p className="text-muted-foreground mb-8">
          {notFound
            ? "Ta ankieta nie istnieje albo została usunięta."
            : "Spróbuj odświeżyć stronę za chwilę."}
        </p>
        <Button asChild variant="outline">
          <Link href={ADMIN_SURVEYS_PATH}>
            <ArrowLeft data-icon="inline-start" />
            Wróć do listy ankiet
          </Link>
        </Button>
      </div>
    );
  }

  const phase = surveyPhase(survey.status, survey.endsAt);
  const results = resultsQuery.data?.status === 200 ? resultsQuery.data.data : undefined;
  // The results request is the fresher of the two right after someone answers.
  const submissionCount = results?.submissionCount ?? survey.submissionCount;

  return (
    <div className="bg-background px-4 py-10 md:px-10 md:py-16">
      <div className="mx-auto max-w-[1280px] space-y-10">
        <div>
          <Button asChild variant="ghost" size="sm" className="mb-8 -ml-2">
            <Link href={ADMIN_SURVEYS_PATH}>
              <ArrowLeft data-icon="inline-start" />
              Wróć do listy ankiet
            </Link>
          </Button>

          <div className="flex flex-wrap items-start justify-between gap-6">
            <div className="min-w-0 flex-1 space-y-4">
              <SurveyPhaseBadge phase={phase} />
              <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight break-words md:text-[33px]">
                {survey.title}
              </h1>
              {survey.description ? (
                <p className="text-muted-foreground max-w-2xl text-base leading-relaxed break-words whitespace-pre-line">
                  {survey.description}
                </p>
              ) : null}
            </div>
            <div className="flex shrink-0 flex-wrap items-center gap-2">
              {submissionCount === 0 ? (
                <Button asChild variant="outline">
                  <Link href={adminSurveyEditHref(survey.id)}>
                    <Pencil data-icon="inline-start" />
                    Edytuj ankietę
                  </Link>
                </Button>
              ) : null}
              <SurveyAvailabilityActions survey={survey} />
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <StatTile
            label="Wypełnienia"
            value={submissionCount}
            info="Liczba osób, które wysłały ankietę. Każda osoba może ją wypełnić raz."
          />
          <StatTile
            label="Termin zakończenia"
            value={<span className="text-[23px]">{formatDateTime(survey.endsAt)}</span>}
            hint={DEADLINE_HINTS[phase]}
          />
          <StatTile label="Pytania" value={survey.questions.length} />
        </div>

        <section aria-labelledby="survey-results-heading" className="space-y-5">
          <h2
            id="survey-results-heading"
            className="font-heading text-foreground text-[23px] font-semibold tracking-tight"
          >
            Wyniki
          </h2>
          {resultsQuery.isLoading ? (
            <div className="space-y-4">
              {Array.from({ length: 2 }).map((_, i) => (
                <Skeleton key={i} className="h-40 rounded-xl" />
              ))}
            </div>
          ) : !results ? (
            <p className="text-destructive font-medium">Nie udało się załadować wyników.</p>
          ) : (
            <>
              {submissionCount === 0 ? (
                <p className="text-muted-foreground">
                  {phase === "draft"
                    ? "Odpowiedzi pojawią się tutaj po publikacji ankiety."
                    : "Nikt jeszcze nie wypełnił tej ankiety."}
                </p>
              ) : null}
              <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                {results.questions.map((result, index) => (
                  <SurveyQuestionResult
                    key={result.questionId}
                    result={result}
                    number={index + 1}
                    submissionCount={submissionCount}
                  />
                ))}
              </div>
            </>
          )}
        </section>
      </div>
    </div>
  );
}
