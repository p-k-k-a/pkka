"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Plus } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import {
  ApiError,
  SurveyAudience,
  SurveyStatus,
  getGetAdminSurveyQueryKey,
  getListAdminSurveysQueryKey,
  useCreateAdminSurvey,
  useUpdateAdminSurvey,
  type AdminSurveyResponse,
} from "@pkka/api";
import {
  SURVEY_AUDIENCE_OPTIONS,
  SURVEY_MAX_QUESTIONS,
  SURVEY_TITLE_MAX,
  formatQuestionCount,
  fromDatetimeLocalValue,
  isSurveyAudience,
  surveyAudienceLabel,
  surveyQuestionIssue,
  toDatetimeLocalValue,
  toSurveyQuestionRequest,
  type SurveyQuestionDraft,
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
import { FieldLabel } from "@/components/admin/field-label";
import { SurveyPreview } from "@/components/admin/survey-preview";
import {
  SurveyQuestionEditor,
  newDraftKey,
  newSurveyOption,
  newSurveyQuestion,
} from "@/components/admin/survey-question-editor";
import { SectionTitle } from "@/components/content/section-title";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { ADMIN_SURVEYS_PATH } from "@/lib/survey-paths";

const DEFAULT_OPEN_DAYS = 14;

// Two weeks from now, at the end of that day, so the deadline doesn't land mid-afternoon.
function defaultDeadline() {
  const date = new Date();
  date.setDate(date.getDate() + DEFAULT_OPEN_DAYS);
  date.setHours(23, 59, 0, 0);
  return toDatetimeLocalValue(date.toISOString());
}

function toDrafts(survey: AdminSurveyResponse): SurveyQuestionDraft[] {
  return survey.questions.map((question) => ({
    key: question.id,
    content: question.content,
    type: question.type,
    // A text question comes back without options and anything but a grid without rows;
    // two blanks wait in case the type changes.
    options: question.options?.length
      ? question.options.map((option) => ({ key: option.id, label: option.label }))
      : [newSurveyOption(), newSurveyOption()],
    rows: question.rows?.length
      ? question.rows.map((row) => ({ key: row.id, label: row.label }))
      : [newSurveyOption(), newSurveyOption()],
  }));
}

function focusField(id: string) {
  const element = document.getElementById(id);
  element?.scrollIntoView({ behavior: "smooth", block: "center" });
  element?.focus({ preventScroll: true });
}

type SurveyFormProps = {
  survey?: AdminSurveyResponse;
};

export function SurveyForm({ survey }: SurveyFormProps) {
  const router = useRouter();
  const queryClient = useQueryClient();

  const [title, setTitle] = useState(survey?.title ?? "");
  const [description, setDescription] = useState(survey?.description ?? "");
  const [audience, setAudience] = useState<SurveyAudience>(
    survey?.audience ?? SurveyAudience.VERIFIED_ALUMNI,
  );
  const [endsAt, setEndsAt] = useState(() =>
    survey ? toDatetimeLocalValue(survey.endsAt) : defaultDeadline(),
  );
  const [questions, setQuestions] = useState<SurveyQuestionDraft[]>(() =>
    survey ? toDrafts(survey) : [newSurveyQuestion()],
  );
  // A draft saved from this screen is updated by the next save instead of created again.
  const [surveyId, setSurveyId] = useState(survey?.id);
  const [savedStatus, setSavedStatus] = useState(survey?.status);
  const [view, setView] = useState<"edit" | "preview">("edit");
  const [showIssues, setShowIssues] = useState(false);
  // Whether the deadline is too early depends on the status being saved, so it is judged
  // on a save attempt and forgotten once the date changes.
  const [shownDeadlineIssue, setShownDeadlineIssue] = useState<string | null>(null);
  const [confirmingPublish, setConfirmingPublish] = useState(false);
  const [pendingStatus, setPendingStatus] = useState<SurveyStatus | null>(null);

  const createSurvey = useCreateAdminSurvey();
  const updateSurvey = useUpdateAdminSurvey();

  const isEditing = surveyId !== undefined;
  const isPublished = savedStatus === SurveyStatus.ACTIVE;
  const isPending = pendingStatus !== null;
  const endsAtIso = fromDatetimeLocalValue(endsAt);
  const titleIssue = title.trim() === "" ? "Wpisz tytuł ankiety." : null;
  const questionIssues = questions.map(surveyQuestionIssue);

  const deadlineIssue = (status: SurveyStatus) => {
    if (!endsAtIso) return "Ustaw termin zakończenia.";
    if (status === SurveyStatus.ACTIVE && Date.parse(endsAtIso) <= Date.now()) {
      return "Opublikowana ankieta musi mieć termin w przyszłości.";
    }
    return null;
  };
  /** Points at the first thing to fix; true when there is nothing left. */
  const validate = (status: SurveyStatus) => {
    const deadline = deadlineIssue(status);
    setShownDeadlineIssue(deadline);
    const firstQuestionWithIssue = questionIssues.findIndex((issue) => issue !== null);
    const fieldId = titleIssue
      ? "survey-title"
      : firstQuestionWithIssue !== -1
        ? `survey-question-${questions[firstQuestionWithIssue].key}-content`
        : deadline
          ? "survey-ends-at"
          : null;
    if (fieldId === null) return true;

    setShowIssues(true);
    setView("edit");
    toast.error(fieldId === "survey-ends-at" ? deadline : "Uzupełnij zaznaczone pola ankiety.");
    // The editor may have just been swapped back in for the preview.
    requestAnimationFrame(() => focusField(fieldId));
    return false;
  };

  const save = (status: SurveyStatus) => {
    if (!endsAtIso) return;
    const data = {
      title: title.trim(),
      description: description.trim() || undefined,
      endsAt: endsAtIso,
      status,
      audience,
      questions: questions.map(toSurveyQuestionRequest),
    };
    setPendingStatus(status);

    const onSuccess = (savedId: string) => {
      queryClient.invalidateQueries({ queryKey: getListAdminSurveysQueryKey() });
      queryClient.invalidateQueries({ queryKey: getGetAdminSurveyQueryKey(savedId) });
      setPendingStatus(null);
      setConfirmingPublish(false);
      if (status === SurveyStatus.ACTIVE) {
        toast.success(isPublished ? "Zapisano zmiany" : "Ankieta opublikowana");
        router.push(ADMIN_SURVEYS_PATH);
      } else {
        setSurveyId(savedId);
        setSavedStatus(status);
        toast.success("Zapisano szkic");
      }
    };
    const onError = (error: unknown) => {
      setPendingStatus(null);
      setConfirmingPublish(false);
      toast.error(
        error instanceof ApiError && error.status === 409
          ? "Ktoś już wypełnił tę ankietę, więc nie można jej zmienić."
          : "Nie udało się zapisać ankiety. Spróbuj ponownie.",
      );
    };

    if (surveyId !== undefined) {
      updateSurvey.mutate(
        { id: surveyId, data },
        { onSuccess: () => onSuccess(surveyId), onError },
      );
    } else {
      createSurvey.mutate(
        { data },
        { onSuccess: (response) => onSuccess(response.data.id), onError },
      );
    }
  };

  const updateQuestion = (question: SurveyQuestionDraft) =>
    setQuestions((prev) => prev.map((item) => (item.key === question.key ? question : item)));

  const moveQuestion = (index: number, direction: -1 | 1) =>
    setQuestions((prev) => {
      const next = [...prev];
      [next[index], next[index + direction]] = [next[index + direction], next[index]];
      return next;
    });

  const duplicateQuestion = (index: number) =>
    setQuestions((prev) => {
      const source = prev[index];
      const copy = {
        ...source,
        key: newDraftKey(),
        options: source.options.map((option) => newSurveyOption(option.label)),
        rows: source.rows.map((row) => newSurveyOption(row.label)),
      };
      return [...prev.slice(0, index + 1), copy, ...prev.slice(index + 1)];
    });

  const removeQuestion = (index: number) => {
    const removed = questions[index];
    setQuestions((prev) => prev.filter((item) => item.key !== removed.key));
    toast("Usunięto pytanie", {
      action: {
        label: "Cofnij",
        onClick: () =>
          setQuestions((prev) => [...prev.slice(0, index), removed, ...prev.slice(index)]),
      },
    });
  };

  const addQuestion = () => {
    const question = newSurveyQuestion();
    setQuestions((prev) => [...prev, question]);
    requestAnimationFrame(() => focusField(`survey-question-${question.key}-content`));
  };

  const canAddQuestion = questions.length < SURVEY_MAX_QUESTIONS;

  return (
    <div className="bg-background px-4 py-10 md:px-10 md:py-16">
      <div className="mx-auto max-w-[1280px]">
        <Button asChild variant="ghost" size="sm" className="mb-8 -ml-2">
          <Link href={ADMIN_SURVEYS_PATH}>
            <ArrowLeft data-icon="inline-start" />
            Wróć do listy ankiet
          </Link>
        </Button>

        <div className="mb-10 flex flex-wrap items-end justify-between gap-6">
          <div className="space-y-3">
            <p className="bg-accent/10 text-accent inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
              {isEditing ? "Edycja" : "Nowa ankieta"}
            </p>
            <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
              {isEditing ? "Edytuj ankietę" : "Utwórz ankietę"}
            </h1>
          </div>
          <Tabs value={view} onValueChange={(value) => setView(value as "edit" | "preview")}>
            <TabsList>
              <TabsTrigger value="edit">Edycja</TabsTrigger>
              <TabsTrigger value="preview">Podgląd</TabsTrigger>
            </TabsList>
          </Tabs>
        </div>

        <div className="grid grid-cols-1 items-start gap-16 lg:grid-cols-[minmax(0,1fr)_320px]">
          {view === "preview" ? (
            <SurveyPreview title={title} description={description} questions={questions} />
          ) : (
            <div className="space-y-12">
              <section className="space-y-5">
                <FieldLabel htmlFor="survey-title">Tytuł</FieldLabel>
                <Input
                  id="survey-title"
                  value={title}
                  maxLength={SURVEY_TITLE_MAX}
                  placeholder="np. Jakich wydarzeń chcesz w przyszłym roku?"
                  className="font-heading h-auto min-h-14 py-3 text-lg font-semibold md:text-[23px]"
                  aria-invalid={showIssues && titleIssue !== null}
                  aria-describedby={showIssues && titleIssue ? "survey-title-issue" : undefined}
                  onChange={(changeEvent) => setTitle(changeEvent.target.value)}
                />
                {showIssues && titleIssue ? (
                  <p id="survey-title-issue" className="text-destructive text-xs font-medium">
                    {titleIssue}
                  </p>
                ) : null}
              </section>

              <section className="space-y-5">
                <SectionTitle>Opis</SectionTitle>
                <Textarea
                  id="survey-description"
                  aria-label="Opis ankiety"
                  value={description}
                  rows={3}
                  placeholder="Krótko: po co jest ankieta i co zrobicie z odpowiedziami. Opcjonalnie."
                  onChange={(changeEvent) => setDescription(changeEvent.target.value)}
                />
              </section>

              <section className="space-y-5">
                <SectionTitle>Pytania</SectionTitle>
                <p className="text-muted-foreground text-sm">
                  Alumn odpowiada na wszystkie pytania, więc pytaj tylko o to, co naprawdę
                  potrzebne.
                </p>
                <div className="space-y-5">
                  {questions.map((question, index) => (
                    <SurveyQuestionEditor
                      key={question.key}
                      question={question}
                      number={index + 1}
                      isFirst={index === 0}
                      isLast={index === questions.length - 1}
                      canRemove={questions.length > 1}
                      canDuplicate={canAddQuestion}
                      issue={showIssues ? questionIssues[index] : null}
                      onChange={updateQuestion}
                      onMove={(direction) => moveQuestion(index, direction)}
                      onDuplicate={() => duplicateQuestion(index)}
                      onRemove={() => removeQuestion(index)}
                    />
                  ))}
                </div>
                <Button
                  type="button"
                  variant="outline"
                  className="w-full border-dashed"
                  disabled={!canAddQuestion}
                  onClick={addQuestion}
                >
                  <Plus data-icon="inline-start" />
                  Dodaj pytanie
                </Button>
              </section>
            </div>
          )}

          <div className="space-y-10 lg:sticky lg:top-6">
            <Card className="gap-5 rounded-lg p-5 shadow-none">
              <SectionTitle>Publikacja</SectionTitle>
              <div className="space-y-2">
                <FieldLabel htmlFor="survey-audience">Kto może wypełnić</FieldLabel>
                <Select
                  id="survey-audience"
                  value={audience}
                  aria-describedby="survey-audience-hint"
                  onChange={(changeEvent) => {
                    if (isSurveyAudience(changeEvent.target.value)) {
                      setAudience(changeEvent.target.value);
                    }
                  }}
                >
                  {SURVEY_AUDIENCE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </Select>
                <p id="survey-audience-hint" className="text-muted-foreground text-xs">
                  {audience === SurveyAudience.ALL_USERS
                    ? "Także osoby, których wniosek nie został jeszcze zatwierdzony."
                    : "Tylko osoby z zatwierdzonym wnioskiem."}
                </p>
              </div>
              <div className="space-y-2">
                <FieldLabel htmlFor="survey-ends-at">Termin zakończenia</FieldLabel>
                <Input
                  id="survey-ends-at"
                  type="datetime-local"
                  value={endsAt}
                  aria-invalid={shownDeadlineIssue !== null}
                  aria-describedby="survey-ends-at-hint"
                  onChange={(changeEvent) => {
                    setEndsAt(changeEvent.target.value);
                    setShownDeadlineIssue(null);
                  }}
                />
                {shownDeadlineIssue ? (
                  <p id="survey-ends-at-hint" className="text-destructive text-xs font-medium">
                    {shownDeadlineIssue}
                  </p>
                ) : (
                  <p id="survey-ends-at-hint" className="text-muted-foreground text-xs">
                    Po tym terminie ankieta zniknie z listy ankiet.
                  </p>
                )}
              </div>
              <p className="text-muted-foreground text-sm">
                {isPublished ? "Ankieta jest opublikowana." : "Szkic widzą tylko administratorzy."}{" "}
                W ankiecie: {formatQuestionCount(questions.length)}.
              </p>
              <div className="flex flex-col gap-2">
                {isPublished ? (
                  <>
                    <Button
                      type="button"
                      disabled={isPending}
                      onClick={() => validate(SurveyStatus.ACTIVE) && save(SurveyStatus.ACTIVE)}
                    >
                      {pendingStatus === SurveyStatus.ACTIVE ? "Zapisywanie…" : "Zapisz zmiany"}
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      disabled={isPending}
                      onClick={() => validate(SurveyStatus.DRAFT) && save(SurveyStatus.DRAFT)}
                    >
                      {pendingStatus === SurveyStatus.DRAFT ? "Zapisywanie…" : "Cofnij do szkicu"}
                    </Button>
                  </>
                ) : (
                  <>
                    <Button
                      type="button"
                      variant="outline"
                      disabled={isPending}
                      onClick={() => validate(SurveyStatus.DRAFT) && save(SurveyStatus.DRAFT)}
                    >
                      {pendingStatus === SurveyStatus.DRAFT ? "Zapisywanie…" : "Zapisz szkic"}
                    </Button>
                    <Button
                      type="button"
                      disabled={isPending}
                      onClick={() => validate(SurveyStatus.ACTIVE) && setConfirmingPublish(true)}
                    >
                      Opublikuj
                    </Button>
                  </>
                )}
              </div>
              <p className="text-muted-foreground text-xs">
                Pytania możesz zmieniać do pierwszego wypełnienia. Później zmienisz już tylko
                termin.
              </p>
            </Card>
          </div>
        </div>
      </div>

      <AlertDialog
        open={confirmingPublish}
        onOpenChange={(open) => !open && !isPending && setConfirmingPublish(false)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Opublikować ankietę?</AlertDialogTitle>
            <AlertDialogDescription>
              {`„${title.trim()}” od razu zobaczą: ${surveyAudienceLabel(audience).toLowerCase()}. Gdy ktoś ją wypełni, pytań nie da się już zmienić.`}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isPending}>Anuluj</AlertDialogCancel>
            <AlertDialogAction
              disabled={isPending}
              onClick={(clickEvent) => {
                // Keeps the dialog open until the request settles.
                clickEvent.preventDefault();
                save(SurveyStatus.ACTIVE);
              }}
            >
              {isPending ? "Publikowanie…" : "Opublikuj ankietę"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
