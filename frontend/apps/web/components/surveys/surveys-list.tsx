"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowLeft, ArrowRight, CalendarClock, ListChecks } from "lucide-react";
import { useListActiveSurveys } from "@pkka/api";
import {
  SURVEYS_PAGE_SIZE,
  formatDateTime,
  formatQuestionCount,
  surveyTimeLeft,
} from "@pkka/domain";
import { LabeledDetails } from "@/components/labeled-details";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { surveyHref } from "@/lib/survey-paths";

export function SurveysList() {
  const [page, setPage] = useState(0);
  const {
    data: response,
    isPending,
    isError,
    isPlaceholderData,
    refetch,
  } = useListActiveSurveys(
    { page, size: SURVEYS_PAGE_SIZE },
    { query: { placeholderData: (previous) => previous } },
  );

  const pageData = response?.status === 200 ? response.data : undefined;
  const surveys = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;

  return (
    <div className="flex flex-col">
      <section className="bg-background px-4 pt-10 pb-6 md:px-10 md:pt-16 md:pb-8">
        <div className="mx-auto max-w-[1280px] space-y-6">
          <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
            Ankiety
          </h1>
          <p className="text-muted-foreground max-w-2xl text-base leading-relaxed">
            Twoje zdanie pomaga planować działania klubu. Każdą ankietę wypełniasz raz, a po
            wysłaniu znika ona z tej listy.
          </p>
        </div>
      </section>

      <section className="bg-background px-4 pb-10 md:px-10 md:pb-20">
        <div className="mx-auto flex max-w-[1280px] flex-col gap-6">
          {isPending ? (
            <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="bg-muted h-64 w-full rounded-none" />
              ))}
            </div>
          ) : isError && !pageData ? (
            <div className="flex flex-col items-start gap-3">
              <p className="text-destructive font-medium">Nie udało się załadować ankiet.</p>
              <Button type="button" variant="outline" onClick={() => void refetch()}>
                Spróbuj ponownie
              </Button>
            </div>
          ) : surveys.length === 0 ? (
            <p className="text-muted-foreground">
              Nie masz teraz żadnych ankiet do wypełnienia. Nowe pojawią się tutaj.
            </p>
          ) : (
            <>
              <div
                className={`grid grid-cols-1 gap-8 transition-opacity md:grid-cols-2 lg:grid-cols-3 ${isPlaceholderData ? "opacity-60" : ""}`}
              >
                {surveys.map((survey) => {
                  const timeLeft = surveyTimeLeft(survey.endsAt);
                  return (
                    <Card
                      key={survey.id}
                      className="bg-muted flex h-full flex-col gap-6 rounded-none p-6 shadow-none ring-0"
                    >
                      {timeLeft ? (
                        <span className="bg-band text-band-foreground inline-flex w-fit rounded-lg px-3 py-1 text-[11px] font-semibold tracking-widest uppercase">
                          Zostało: {timeLeft}
                        </span>
                      ) : null}
                      <div className="space-y-3">
                        <h2 className="font-heading text-foreground text-lg leading-snug font-semibold break-words">
                          {survey.title}
                        </h2>
                        {survey.description ? (
                          <p className="text-muted-foreground line-clamp-3 text-sm leading-relaxed break-words">
                            {survey.description}
                          </p>
                        ) : null}
                      </div>
                      <LabeledDetails
                        items={[
                          {
                            icon: CalendarClock,
                            label: "Termin",
                            value: formatDateTime(survey.endsAt),
                          },
                          {
                            icon: ListChecks,
                            label: "Pytania",
                            value: formatQuestionCount(survey.questionCount),
                          },
                        ]}
                      />
                      <div className="mt-auto pt-2">
                        <Button asChild size="sm">
                          <Link href={surveyHref(survey.id)}>
                            Wypełnij ankietę
                            <ArrowRight data-icon="inline-end" />
                          </Link>
                        </Button>
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
                    disabled={page <= 0 || isPlaceholderData}
                    onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                  >
                    <ArrowLeft data-icon="inline-start" />
                    Poprzednia
                  </Button>
                  <span className="text-muted-foreground text-sm">
                    Strona {page + 1} z {totalPages}
                  </span>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={page >= totalPages - 1 || isPlaceholderData}
                    onClick={() => setPage((prev) => prev + 1)}
                  >
                    Następna
                    <ArrowRight data-icon="inline-end" />
                  </Button>
                </div>
              ) : null}
            </>
          )}
        </div>
      </section>
    </div>
  );
}
