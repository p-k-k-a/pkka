"use client";

import { useState } from "react";
import {
  ApiError,
  useCreateApplication,
  useListCurrentLegalDocuments,
  type CreateApplicationRequest,
  type Faculty,
  type MeetingPreference,
  type StudyType,
} from "@pkka/api";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { ConsentCheckboxList } from "@/components/legal/consent-checkbox-list";
import { PrivacyNoticeSummary } from "@/components/legal/privacy-notice-summary";
import {
  graduationYearError,
  graduationYearMax,
  isGraduationYearValid,
  isOutdatedDocumentError,
  missingRequiredDocuments,
  sortLegalDocuments,
  toggleInSet,
  APPLICATION_CONFLICT_MESSAGE,
  APPLICATION_SUBMIT_ERROR_MESSAGE,
  OUTDATED_DOCUMENTS_MESSAGE,
  FACULTY_OPTIONS,
  GRADUATION_YEAR_MIN,
  MEETING_PREFERENCE_OPTIONS,
  STUDY_TYPE_OPTIONS,
} from "@pkka/domain";

function toggle<T>(list: T[], value: T): T[] {
  return list.includes(value) ? list.filter((item) => item !== value) : [...list, value];
}

export function VerificationForm({ onSubmitted }: { onSubmitted?: () => void | Promise<void> }) {
  const [faculty, setFaculty] = useState<Faculty | "">("");
  const [fieldOfStudy, setFieldOfStudy] = useState("");
  const [studyType, setStudyType] = useState<StudyType | "">("");
  const [graduationYear, setGraduationYear] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [interests, setInterests] = useState("");
  const [meetingPreferences, setMeetingPreferences] = useState<MeetingPreference[]>([]);
  const [coCreationInterest, setCoCreationInterest] = useState(false);
  const [acceptedIds, setAcceptedIds] = useState<ReadonlySet<string>>(new Set());
  const [formError, setFormError] = useState<string | null>(null);

  const legalDocuments = useListCurrentLegalDocuments();
  const documents = sortLegalDocuments(legalDocuments.data?.data ?? []);

  const { mutate, isPending } = useCreateApplication<ApiError>({
    mutation: {
      onSuccess: () => {
        void onSubmitted?.();
      },
      onError: (error) => {
        if (isOutdatedDocumentError(error)) {
          // The ticks were for texts no longer in force, so they must be given again.
          setAcceptedIds(new Set());
          void legalDocuments.refetch();
          setFormError(OUTDATED_DOCUMENTS_MESSAGE);
        } else if (error instanceof ApiError && error.status === 409) {
          setFormError(APPLICATION_CONFLICT_MESSAGE);
        } else {
          setFormError(APPLICATION_SUBMIT_ERROR_MESSAGE);
        }
      },
    },
  });

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);

    if (!faculty || !studyType) {
      setFormError("Wybierz wydział i rodzaj studiów.");
      return;
    }

    // Ids of a version replaced since they were ticked drop out here and count as missing.
    if (missingRequiredDocuments(documents, acceptedIds).length > 0) {
      setFormError("Aby złożyć wniosek, musisz zaakceptować wymagane dokumenty.");
      return;
    }

    const parsedGraduationYear = Number.parseInt(graduationYear, 10);
    if (!isGraduationYearValid(parsedGraduationYear)) {
      setFormError(graduationYearError());
      return;
    }

    const payload: CreateApplicationRequest = {
      faculty,
      fieldOfStudy: fieldOfStudy.trim(),
      studyType,
      graduationYear: parsedGraduationYear,
      phoneNumber: phoneNumber.trim(),
      interests: interests
        .split(",")
        .map((interest) => interest.trim())
        .filter(Boolean),
      meetingPreferences,
      coCreationInterest,
      acceptedDocumentIds: documents
        .filter((document) => acceptedIds.has(document.id))
        .map((document) => document.id),
    };

    mutate({ data: payload });
  }

  return (
    <Card className="gap-0 p-0">
      <form onSubmit={handleSubmit} className="space-y-8 p-6 md:p-8">
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="faculty">
              Wydział <span className="text-destructive">*</span>
            </Label>
            <Select
              id="faculty"
              required
              value={faculty}
              onChange={(event) => setFaculty(event.target.value as Faculty)}
            >
              <option value="" disabled>
                Wybierz wydział
              </option>
              {FACULTY_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="studyType">
              Rodzaj studiów <span className="text-destructive">*</span>
            </Label>
            <Select
              id="studyType"
              required
              value={studyType}
              onChange={(event) => setStudyType(event.target.value as StudyType)}
            >
              <option value="" disabled>
                Wybierz rodzaj studiów
              </option>
              {STUDY_TYPE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="fieldOfStudy">
              Kierunek studiów <span className="text-destructive">*</span>
            </Label>
            <Input
              id="fieldOfStudy"
              required
              maxLength={200}
              placeholder="np. Informatyka"
              value={fieldOfStudy}
              onChange={(event) => setFieldOfStudy(event.target.value)}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="graduationYear">
              Rok ukończenia (lub planowany) <span className="text-destructive">*</span>
            </Label>
            <Input
              id="graduationYear"
              type="number"
              required
              min={GRADUATION_YEAR_MIN}
              max={graduationYearMax()}
              step={1}
              placeholder="np. 2022"
              value={graduationYear}
              onChange={(event) => setGraduationYear(event.target.value)}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="phoneNumber">
              Numer telefonu <span className="text-destructive">*</span>
            </Label>
            <Input
              id="phoneNumber"
              type="tel"
              required
              inputMode="tel"
              pattern="\+?[0-9 \-]{7,32}"
              placeholder="np. +48 600 700 800"
              value={phoneNumber}
              onChange={(event) => setPhoneNumber(event.target.value)}
            />
          </div>

          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="interests">Zainteresowania</Label>
            <Textarea
              id="interests"
              placeholder="Oddziel przecinkami, np. sztuczna inteligencja, mentoring, startupy"
              value={interests}
              onChange={(event) => setInterests(event.target.value)}
            />
          </div>
        </div>

        <fieldset className="space-y-3">
          <legend className="text-foreground text-sm font-medium">Preferowane formy spotkań</legend>
          <div className="flex flex-col gap-2">
            {MEETING_PREFERENCE_OPTIONS.map((option) => (
              <Label key={option.value} htmlFor={`meeting-${option.value}`} className="font-normal">
                <Checkbox
                  id={`meeting-${option.value}`}
                  checked={meetingPreferences.includes(option.value)}
                  onCheckedChange={() =>
                    setMeetingPreferences((prev) => toggle(prev, option.value))
                  }
                />
                {option.label}
              </Label>
            ))}
          </div>
        </fieldset>

        <div className="flex flex-col gap-3">
          <Label htmlFor="coCreationInterest" className="font-normal">
            <Checkbox
              id="coCreationInterest"
              checked={coCreationInterest}
              onCheckedChange={(checked) => setCoCreationInterest(checked === true)}
            />
            Chcę aktywnie współtworzyć działania klubu
          </Label>
        </div>

        <fieldset className="space-y-4">
          <legend className="text-foreground mb-3 text-sm font-medium">
            Zgody i oświadczenia <span className="text-destructive">*</span>
          </legend>
          {legalDocuments.isPending ? (
            <div className="space-y-3">
              <Skeleton className="h-5 w-full rounded-lg" />
              <Skeleton className="h-5 w-full rounded-lg" />
              <Skeleton className="h-10 w-full rounded-lg" />
            </div>
          ) : legalDocuments.isError ? (
            <div className="space-y-2">
              <p className="text-destructive text-sm font-medium">
                Nie udało się wczytać dokumentów do akceptacji.
              </p>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={legalDocuments.isFetching}
                onClick={() => void legalDocuments.refetch()}
              >
                Spróbuj ponownie
              </Button>
            </div>
          ) : (
            <ConsentCheckboxList
              documents={documents}
              acceptedIds={acceptedIds}
              onToggle={(documentId) => setAcceptedIds((prev) => toggleInSet(prev, documentId))}
            />
          )}
          <PrivacyNoticeSummary />
        </fieldset>

        {formError ? <p className="text-destructive text-sm font-medium">{formError}</p> : null}

        <Button
          type="submit"
          size="xl"
          className="w-full rounded-xl font-semibold"
          disabled={isPending || documents.length === 0}
        >
          {isPending ? "Wysyłanie…" : "Wyślij wniosek"}
        </Button>
      </form>
    </Card>
  );
}
