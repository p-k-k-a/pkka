"use client";

import { useState } from "react";
import { CalendarClock, CircleStop, Play, Send } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import {
  SurveyStatus,
  getGetResultsQueryKey,
  getGetSurveyQueryKey,
  getListSurveysQueryKey,
  useUpdateSurveyAvailability,
  type AdminSurveyResponse,
} from "@pkka/api";
import {
  fromDatetimeLocalValue,
  surveyPhase,
  toDatetimeLocalValue,
  type SurveyPhase,
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
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

const REOPEN_DAYS = 7;

const SCHEDULE_COPY: Record<
  SurveyPhase,
  { title: string; description: string; action: string; done: string }
> = {
  draft: {
    title: "Opublikować ankietę?",
    description: "Ankieta od razu pojawi się u zweryfikowanych alumnów.",
    action: "Opublikuj ankietę",
    done: "Ankieta opublikowana",
  },
  active: {
    title: "Zmienić termin zakończenia?",
    description: "Alumni będą mogli wypełniać ankietę do nowego terminu.",
    action: "Zapisz termin",
    done: "Zmieniono termin",
  },
  ended: {
    title: "Wznowić ankietę?",
    description:
      "Ankieta wróci na listę alumnów do podanego terminu. Zebrane odpowiedzi zostają, a osoby, które już ją wypełniły, nie wypełnią jej ponownie.",
    action: "Wznów ankietę",
    done: "Ankieta wznowiona",
  },
};

// A deadline that has passed can't reopen anything, so the dialog suggests a week from now.
function suggestedDeadline(endsAt: string) {
  if (Date.parse(endsAt) > Date.now()) return toDatetimeLocalValue(endsAt);
  const date = new Date();
  date.setDate(date.getDate() + REOPEN_DAYS);
  date.setHours(23, 59, 0, 0);
  return toDatetimeLocalValue(date.toISOString());
}

/** Close, reopen, publish or reschedule: the changes a survey allows even with answers in. */
export function SurveyAvailabilityActions({ survey }: { survey: AdminSurveyResponse }) {
  const queryClient = useQueryClient();
  const [dialog, setDialog] = useState<"schedule" | "close" | null>(null);
  const [endsAt, setEndsAt] = useState("");
  const [deadlineIssue, setDeadlineIssue] = useState<string | null>(null);
  const phase = surveyPhase(survey.status, survey.endsAt);
  const copy = SCHEDULE_COPY[phase];

  const updateAvailability = useUpdateSurveyAvailability();

  const submit = (status: SurveyStatus, nextEndsAt: string, done: string) =>
    updateAvailability.mutate(
      { id: survey.id, data: { status, endsAt: nextEndsAt } },
      {
        onSuccess: () => {
          queryClient.invalidateQueries({ queryKey: getListSurveysQueryKey() });
          queryClient.invalidateQueries({ queryKey: getGetSurveyQueryKey(survey.id) });
          queryClient.invalidateQueries({ queryKey: getGetResultsQueryKey(survey.id) });
          setDialog(null);
          toast.success(done);
        },
      },
    );

  const openSchedule = () => {
    setEndsAt(suggestedDeadline(survey.endsAt));
    setDeadlineIssue(null);
    updateAvailability.reset();
    setDialog("schedule");
  };

  const saveSchedule = () => {
    const iso = fromDatetimeLocalValue(endsAt);
    if (!iso || Date.parse(iso) <= Date.now()) {
      setDeadlineIssue("Podaj termin w przyszłości.");
      return;
    }
    submit(SurveyStatus.ACTIVE, iso, copy.done);
  };

  const isPending = updateAvailability.isPending;

  return (
    <>
      {phase === "draft" ? (
        <Button type="button" onClick={openSchedule}>
          <Send data-icon="inline-start" />
          Opublikuj
        </Button>
      ) : null}
      {phase === "active" ? (
        <>
          <Button type="button" variant="outline" onClick={openSchedule}>
            <CalendarClock data-icon="inline-start" />
            Zmień termin
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => {
              updateAvailability.reset();
              setDialog("close");
            }}
          >
            <CircleStop data-icon="inline-start" />
            Zakończ teraz
          </Button>
        </>
      ) : null}
      {phase === "ended" ? (
        <Button type="button" variant="outline" onClick={openSchedule}>
          <Play data-icon="inline-start" />
          Wznów ankietę
        </Button>
      ) : null}

      <AlertDialog
        open={dialog === "schedule"}
        onOpenChange={(open) => !open && !isPending && setDialog(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{copy.title}</AlertDialogTitle>
            <AlertDialogDescription>{copy.description}</AlertDialogDescription>
          </AlertDialogHeader>
          <div className="space-y-2">
            <FieldLabel htmlFor="survey-availability-ends-at">Termin zakończenia</FieldLabel>
            <Input
              id="survey-availability-ends-at"
              type="datetime-local"
              value={endsAt}
              aria-invalid={deadlineIssue !== null}
              aria-describedby={deadlineIssue ? "survey-availability-issue" : undefined}
              onChange={(changeEvent) => {
                setEndsAt(changeEvent.target.value);
                setDeadlineIssue(null);
              }}
            />
            {deadlineIssue ? (
              <p id="survey-availability-issue" className="text-destructive text-xs font-medium">
                {deadlineIssue}
              </p>
            ) : null}
          </div>
          {updateAvailability.isError ? (
            <p className="text-destructive text-sm font-medium">
              Nie udało się zapisać zmiany. Spróbuj ponownie.
            </p>
          ) : null}
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isPending}>Anuluj</AlertDialogCancel>
            <AlertDialogAction
              disabled={isPending}
              onClick={(clickEvent) => {
                // Keeps the dialog open until the request settles, so a failure is not lost.
                clickEvent.preventDefault();
                saveSchedule();
              }}
            >
              {isPending ? "Zapisywanie…" : copy.action}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog
        open={dialog === "close"}
        onOpenChange={(open) => !open && !isPending && setDialog(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Zakończyć ankietę teraz?</AlertDialogTitle>
            <AlertDialogDescription>
              Ankieta zniknie z listy alumnów i nikt więcej jej nie wypełni. Zebrane odpowiedzi
              zostają, a ankietę możesz później wznowić.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {updateAvailability.isError ? (
            <p className="text-destructive text-sm font-medium">
              Nie udało się zakończyć ankiety. Spróbuj ponownie.
            </p>
          ) : null}
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isPending}>Anuluj</AlertDialogCancel>
            <AlertDialogAction
              disabled={isPending}
              onClick={(clickEvent) => {
                clickEvent.preventDefault();
                submit(SurveyStatus.CLOSED, survey.endsAt, "Ankieta zakończona");
              }}
            >
              {isPending ? "Kończenie…" : "Zakończ ankietę"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
