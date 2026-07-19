"use client";

import { useState } from "react";
import {
  ApiError,
  ApplicationResponseDtoConsentsItem,
  useCreate,
  type CreateApplicationRequestDto,
  type CreateApplicationRequestDtoConsentsItem,
  type CreateApplicationRequestDtoFaculty,
  type CreateApplicationRequestDtoMeetingPreferencesItem,
  type CreateApplicationRequestDtoStudyType,
} from "@pkka/api";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import {
  CONSENT_OPTIONS,
  FACULTY_OPTIONS,
  MEETING_PREFERENCE_OPTIONS,
  STUDY_TYPE_OPTIONS,
} from "@/lib/application-labels";

const REQUIRED_CONSENTS: string[] = [
  ApplicationResponseDtoConsentsItem.REGULATIONS_PRIVACY,
  ApplicationResponseDtoConsentsItem.GDPR_DATA_PROCESSING,
];

function toggle<T>(list: T[], value: T): T[] {
  return list.includes(value) ? list.filter((item) => item !== value) : [...list, value];
}

export function VerificationForm({
  onSubmitted,
}: {
  onSubmitted?: () => void | Promise<void>;
}) {
  const [faculty, setFaculty] = useState("");
  const [fieldOfStudy, setFieldOfStudy] = useState("");
  const [studyType, setStudyType] = useState("");
  const [graduationYear, setGraduationYear] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [interests, setInterests] = useState("");
  const [meetingPreferences, setMeetingPreferences] = useState<string[]>([]);
  const [coCreationInterest, setCoCreationInterest] = useState(false);
  const [newsletterSubscription, setNewsletterSubscription] = useState(false);
  const [consents, setConsents] = useState<string[]>([]);
  const [formError, setFormError] = useState<string | null>(null);

  const { mutate, isPending } = useCreate<ApiError>({
    mutation: {
      onSuccess: () => {
        void onSubmitted?.();
      },
      onError: (error) => {
        if (error instanceof ApiError && error.status === 409) {
          setFormError(
            "Masz już aktywny wniosek (w trakcie weryfikacji lub zaakceptowany). Nie możesz złożyć kolejnego.",
          );
        } else {
          setFormError("Nie udało się wysłać wniosku. Sprawdź dane i spróbuj ponownie.");
        }
      },
    },
  });

  const currentYear = new Date().getFullYear();

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);

    if (!REQUIRED_CONSENTS.every((consent) => consents.includes(consent))) {
      setFormError("Aby złożyć wniosek, musisz zaakceptować wymagane zgody.");
      return;
    }

    const parsedGraduationYear = Number.parseInt(graduationYear, 10);
    if (
      !Number.isInteger(parsedGraduationYear) ||
      parsedGraduationYear < 1919 ||
      parsedGraduationYear > currentYear
    ) {
      setFormError(`Podaj prawidłowy rok ukończenia (1919–${currentYear}).`);
      return;
    }

    const payload: CreateApplicationRequestDto = {
      faculty: faculty as CreateApplicationRequestDtoFaculty,
      fieldOfStudy: fieldOfStudy.trim(),
      studyType: studyType as CreateApplicationRequestDtoStudyType,
      graduationYear: parsedGraduationYear,
      phoneNumber: phoneNumber.trim(),
      interests: interests
        .split(",")
        .map((interest) => interest.trim())
        .filter(Boolean),
      meetingPreferences: meetingPreferences as CreateApplicationRequestDtoMeetingPreferencesItem[],
      coCreationInterest,
      newsletterSubscription,
      consents: consents as CreateApplicationRequestDtoConsentsItem[],
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
              onChange={(event) => setFaculty(event.target.value)}
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
              onChange={(event) => setStudyType(event.target.value)}
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
              Rok ukończenia <span className="text-destructive">*</span>
            </Label>
            <Input
              id="graduationYear"
              type="number"
              required
              min={1919}
              max={currentYear}
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
                  onChange={() => setMeetingPreferences((prev) => toggle(prev, option.value))}
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
              onChange={(event) => setCoCreationInterest(event.target.checked)}
            />
            Chcę aktywnie współtworzyć działania klubu
          </Label>
          <Label htmlFor="newsletterSubscription" className="font-normal">
            <Checkbox
              id="newsletterSubscription"
              checked={newsletterSubscription}
              onChange={(event) => setNewsletterSubscription(event.target.checked)}
            />
            Chcę zapisać się do newslettera
          </Label>
        </div>

        <fieldset className="space-y-3">
          <legend className="text-foreground text-sm font-medium">
            Zgody <span className="text-destructive">*</span>
          </legend>
          <div className="flex flex-col gap-2">
            {CONSENT_OPTIONS.map((option) => (
              <Label
                key={option.value}
                htmlFor={`consent-${option.value}`}
                className="items-start font-normal"
              >
                <Checkbox
                  id={`consent-${option.value}`}
                  className="mt-0.5"
                  checked={consents.includes(option.value)}
                  onChange={() => setConsents((prev) => toggle(prev, option.value))}
                />
                {option.label}
              </Label>
            ))}
          </div>
        </fieldset>

        {formError ? <p className="text-destructive text-sm font-medium">{formError}</p> : null}

        <Button
          type="submit"
          size="xl"
          className="w-full rounded-xl font-semibold"
          disabled={isPending}
        >
          {isPending ? "Wysyłanie…" : "Wyślij wniosek"}
        </Button>
      </form>
    </Card>
  );
}
