"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft, CircleCheck, ClipboardX } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import {
  getListActiveSurveysQueryKey,
  useGetActiveSurvey,
  useSubmitSurvey,
  type SurveyResponse,
} from "@pkka/api";
import {
  formatDateTime,
  formatQuestionCount,
  isSurveyAnswerGiven,
  surveyProblem,
  type SurveyProblem,
} from "@pkka/domain";
import { SurveyQuestionField } from "@/components/surveys/survey-question-field";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth-context";
import { SURVEYS_PATH } from "@/lib/survey-paths";

const PROBLEM_COPY: Record<
  Exclude<SurveyProblem, "session" | "invalid">,
  { title: string; description: string }
> = {
  submitted: {
    title: "Ta ankieta jest już wypełniona",
    description: "Twoje odpowiedzi są zapisane. Każdą ankietę można wypełnić tylko raz.",
  },
  ended: {
    title: "Ta ankieta jest już zakończona",
    description: "Termin minął albo organizatorzy zamknęli ją wcześniej.",
  },
  missing: {
    title: "Nie znaleziono ankiety",
    description: "Ta ankieta nie istnieje albo została usunięta.",
  },
  unknown: {
    title: "Nie udało się załadować ankiety",
    description: "Spróbuj odświeżyć stronę za chwilę.",
  },
};

function BackToSurveys() {
  return (
    <Button asChild variant="ghost" size="sm" className="-ml-2">
      <Link href={SURVEYS_PATH}>
        <ArrowLeft data-icon="inline-start" />
        Wróć do ankiet
      </Link>
    </Button>
  );
}

function SurveyNotice({
  icon: Icon,
  title,
  description,
}: {
  icon: React.ComponentType<{ className?: string }>;
  title: string;
  description: string;
}) {
  return (
    <section className="bg-background px-4 py-16 md:px-10 md:py-20">
      <div className="mx-auto flex max-w-[720px] flex-col items-start gap-4">
        <Icon className="text-accent size-8" aria-hidden="true" />
        <h1 className="font-heading text-foreground text-[28px] leading-tight font-semibold">
          {title}
        </h1>
        <p className="text-muted-foreground leading-relaxed">{description}</p>
        <Button asChild size="xl">
          <Link href={SURVEYS_PATH}>Wróć do ankiet</Link>
        </Button>
      </div>
    </section>
  );
}

export function SurveyFill({ id }: { id: string }) {
  // The questions must not change under someone who is halfway through them, and a later
  // visit must ask again: by then the survey may be filled in or closed.
  const surveyQuery = useGetActiveSurvey(id, { query: { staleTime: Infinity, gcTime: 0 } });
  // Set when sending the answers finds the survey gone, closed or already filled in.
  const [submitProblem, setSubmitProblem] = useState<SurveyProblem | null>(null);
  const [submitted, setSubmitted] = useState(false);

  if (submitted) {
    return (
      <SurveyNotice
        icon={CircleCheck}
        title="Dziękujemy za wypełnienie ankiety"
        description="Twoje odpowiedzi zostały zapisane."
      />
    );
  }

  if (surveyQuery.isPending) {
    return (
      <div className="px-4 py-10 md:px-10 md:py-16">
        <div className="mx-auto max-w-[1280px] space-y-6">
          <Skeleton className="h-10 w-2/3 max-w-xl rounded-lg" />
          <Skeleton className="h-64 w-full max-w-2xl rounded-lg" />
        </div>
      </div>
    );
  }

  const survey = surveyQuery.data?.status === 200 ? surveyQuery.data.data : undefined;
  const problem = submitProblem ?? (survey ? null : surveyProblem(surveyQuery.error));
  if (problem || !survey) {
    const copy =
      PROBLEM_COPY[
        problem === "session" || problem === "invalid" || !problem ? "unknown" : problem
      ];
    return (
      <SurveyNotice
        icon={problem === "submitted" ? CircleCheck : ClipboardX}
        title={copy.title}
        description={copy.description}
      />
    );
  }

  return (
    <SurveyFillForm
      survey={survey}
      onSubmitted={() => setSubmitted(true)}
      onProblem={setSubmitProblem}
    />
  );
}

type SurveyFillFormProps = {
  survey: SurveyResponse;
  onSubmitted: () => void;
  onProblem: (problem: SurveyProblem) => void;
};

function SurveyFillForm({ survey, onSubmitted, onProblem }: SurveyFillFormProps) {
  const queryClient = useQueryClient();
  const { loginWithKeycloak } = useAuth();
  const [answers, setAnswers] = useState<Record<string, string>>({});
  // Missing answers are pointed out only after the first attempt to send.
  const [showMissing, setShowMissing] = useState(false);
  const submitSurvey = useSubmitSurvey();

  const questions = survey.questions;
  const missing = questions.filter((question) => !isSurveyAnswerGiven(answers[question.id]));
  const answeredCount = questions.length - missing.length;
  const hasAnswers = answeredCount > 0;
  const progress = Math.round((answeredCount / questions.length) * 100);

  // A reload or a closed tab would throw the answers away without a word.
  useEffect(() => {
    if (!hasAnswers) return;
    const warn = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [hasAnswers]);

  const submit = () => {
    if (missing.length > 0) {
      setShowMissing(true);
      const first = document.getElementById(`survey-question-${missing[0].id}`);
      first?.scrollIntoView({ behavior: "smooth", block: "center" });
      first?.querySelector<HTMLElement>("button, textarea")?.focus({ preventScroll: true });
      return;
    }

    submitSurvey.mutate(
      {
        id: survey.id,
        data: {
          answers: questions.map((question) => ({
            questionId: question.id,
            value: answers[question.id].trim(),
          })),
        },
      },
      {
        onSuccess: () => {
          queryClient.invalidateQueries({ queryKey: getListActiveSurveysQueryKey() });
          onSubmitted();
        },
        onError: (error) => {
          const problem = surveyProblem(error);
          if (problem === "session") {
            toast.error("Sesja wygasła. Zaloguj się ponownie.", {
              action: { label: "Zaloguj się", onClick: loginWithKeycloak },
            });
          } else if (problem === "invalid") {
            toast.error(
              "Ankieta zmieniła się w międzyczasie. Odśwież stronę i wypełnij ją ponownie.",
            );
          } else if (problem === "unknown") {
            toast.error("Nie udało się wysłać odpowiedzi. Spróbuj ponownie.");
          } else {
            queryClient.invalidateQueries({ queryKey: getListActiveSurveysQueryKey() });
            onProblem(problem);
          }
        },
      },
    );
  };

  return (
    <div className="bg-background">
      <div className="px-4 pt-10 pb-10 md:px-10 md:pt-16">
        <div className="mx-auto max-w-[1280px]">
          <div className="mb-8">
            <BackToSurveys />
          </div>

          <div className="max-w-2xl space-y-10">
            <header className="space-y-4">
              <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight break-words md:text-[33px]">
                {survey.title}
              </h1>
              {survey.description ? (
                <p className="text-muted-foreground text-base leading-relaxed break-words whitespace-pre-line">
                  {survey.description}
                </p>
              ) : null}
              <p className="bg-muted text-muted-foreground rounded-lg px-4 py-3 text-sm leading-relaxed">
                {formatQuestionCount(questions.length)}, wszystkie obowiązkowe. Ankietę możesz
                wysłać raz, do {formatDateTime(survey.endsAt)}. Administratorzy widzą odpowiedzi
                zbiorczo, bez nazwisk.
              </p>
            </header>

            <div className="space-y-10">
              {questions.map((question, index) => (
                <SurveyQuestionField
                  key={question.id}
                  question={question}
                  number={index + 1}
                  value={answers[question.id] ?? ""}
                  disabled={submitSurvey.isPending}
                  error={
                    showMissing && !isSurveyAnswerGiven(answers[question.id])
                      ? "Odpowiedz na to pytanie."
                      : undefined
                  }
                  onChange={(value) => setAnswers((prev) => ({ ...prev, [question.id]: value }))}
                />
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="bg-background/95 border-border sticky bottom-0 border-t px-4 py-4 backdrop-blur md:px-10">
        <div className="mx-auto flex max-w-[1280px] flex-wrap items-center gap-x-6 gap-y-3">
          <div className="min-w-48 flex-1 space-y-2 sm:max-w-sm">
            <p className="text-foreground text-sm" aria-live="polite">
              {showMissing && missing.length > 0 ? (
                <span className="text-destructive font-medium">
                  Brakuje odpowiedzi: {missing.length} z {questions.length}.
                </span>
              ) : (
                <>
                  Odpowiedziano na <span className="font-semibold">{answeredCount}</span> z{" "}
                  {questions.length}
                </>
              )}
            </p>
            <div className="bg-chart-1/15 h-2 w-full overflow-hidden rounded-full" aria-hidden>
              <div
                className="bg-chart-1 h-full rounded-full transition-[width]"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
          <Button type="button" size="xl" disabled={submitSurvey.isPending} onClick={submit}>
            {submitSurvey.isPending ? "Wysyłanie…" : "Wyślij odpowiedzi"}
          </Button>
        </div>
      </div>
    </div>
  );
}
