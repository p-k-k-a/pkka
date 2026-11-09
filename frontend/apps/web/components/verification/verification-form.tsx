"use client";

import { useState } from "react";
import PhoneInput, { isValidPhoneNumber } from "react-phone-number-input";
import flags from "react-phone-number-input/flags";
import pl from "react-phone-number-input/locale/pl";
import "react-phone-number-input/style.css";
import {
  ApiError,
  ConsentType,
  useCreateApplication,
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
import { Separator } from "@/components/ui/separator";
import { Toggle } from "@/components/ui/toggle";
import { cn } from "@/lib/utils";
import {
  graduationYearError,
  graduationYearMax,
  hasRequiredConsents,
  isGraduationYearValid,
  AGH_PRIVACY_NOTICE_URL,
  APPLICATION_CONFLICT_MESSAGE,
  APPLICATION_SUBMIT_ERROR_MESSAGE,
  FACULTY_OPTIONS,
  GRADUATION_YEAR_MIN,
  INTEREST_AREAS,
  MEETING_PREFERENCE_OPTIONS,
  STUDY_TYPE_OPTIONS,
} from "@pkka/domain";

function toggle<T>(list: T[], value: T): T[] {
  return list.includes(value) ? list.filter((item) => item !== value) : [...list, value];
}

const selectedClass = "bg-primary/15 border-primary";
const unselectedClass = "bg-background border-border";

function FormSection({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: React.ReactNode;
}) {
  return (
    <Card className="gap-6 rounded-2xl p-6 md:p-8">
      <div className="space-y-1">
        <h2 className="font-heading text-foreground text-[23px] leading-tight font-semibold tracking-tight">
          {title}
        </h2>
        {description ? <p className="text-muted-foreground text-sm">{description}</p> : null}
      </div>
      {children}
    </Card>
  );
}

function FieldLabel({
  htmlFor,
  required,
  children,
}: {
  htmlFor?: string;
  required?: boolean;
  children: React.ReactNode;
}) {
  return (
    <Label htmlFor={htmlFor} className="text-xs font-bold tracking-wider uppercase">
      {children}
      {required ? <span className="text-destructive">*</span> : null}
    </Label>
  );
}

function CheckboxCard({
  id,
  title,
  description,
  checked,
  onCheckedChange,
}: {
  id: string;
  title: string;
  description: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
}) {
  return (
    <Label
      htmlFor={id}
      className={cn(
        "cursor-pointer items-start gap-4 rounded-lg border p-4 transition-colors",
        checked ? selectedClass : unselectedClass,
      )}
    >
      <Checkbox
        id={id}
        className="mt-0.5"
        checked={checked}
        onCheckedChange={(value) => onCheckedChange(value === true)}
      />
      <span className="flex flex-col gap-1">
        <span className="text-sm leading-5 font-extrabold tracking-wide uppercase">{title}</span>
        <span className="text-muted-foreground text-sm leading-5 font-normal">{description}</span>
      </span>
    </Label>
  );
}

function ExternalLink({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className="font-bold underline underline-offset-2"
    >
      {children}
    </a>
  );
}

export function VerificationForm({ onSubmitted }: { onSubmitted?: () => void | Promise<void> }) {
  const [faculty, setFaculty] = useState<Faculty | "">("");
  const [fieldOfStudy, setFieldOfStudy] = useState("");
  const [studyType, setStudyType] = useState<StudyType | "">("");
  const [graduationYear, setGraduationYear] = useState("");
  const [phoneNumber, setPhoneNumber] = useState<string | undefined>();
  const [interests, setInterests] = useState<string[]>([]);
  const [meetingPreferences, setMeetingPreferences] = useState<MeetingPreference[]>([]);
  const [coCreationInterest, setCoCreationInterest] = useState(false);
  const [newsletterSubscription, setNewsletterSubscription] = useState(false);
  const [consents, setConsents] = useState<ConsentType[]>([]);
  const [formError, setFormError] = useState<string | null>(null);

  const { mutate, isPending } = useCreateApplication<ApiError>({
    mutation: {
      onSuccess: () => {
        void onSubmitted?.();
      },
      onError: (error) => {
        if (error instanceof ApiError && error.status === 409) {
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

    if (!phoneNumber || !isValidPhoneNumber(phoneNumber)) {
      setFormError("Podaj poprawny numer telefonu.");
      return;
    }

    if (!faculty || !studyType) {
      setFormError("Wybierz wydział i rodzaj studiów.");
      return;
    }

    if (!hasRequiredConsents(consents)) {
      setFormError("Aby złożyć wniosek, musisz zaakceptować wymagane zgody.");
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
      phoneNumber,
      interests,
      meetingPreferences,
      coCreationInterest,
      newsletterSubscription,
      consents,
    };

    mutate({ data: payload });
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <FormSection title="Dane kontaktowe">
        <div className="space-y-2 md:max-w-sm">
          <FieldLabel htmlFor="phoneNumber" required>
            Telefon kontaktowy
          </FieldLabel>
          <PhoneInput
            id="phoneNumber"
            required
            international
            countryCallingCodeEditable={false}
            defaultCountry="PL"
            flags={flags}
            labels={pl}
            autoComplete="tel"
            value={phoneNumber}
            onChange={setPhoneNumber}
            className="border-border bg-background focus-within:border-ring focus-within:ring-ring/50 flex h-11 w-full gap-1 rounded-lg border px-3 text-sm transition-all [--PhoneInput-color--focus:var(--ring)] [--PhoneInputCountryFlag-height:1.1em] focus-within:ring-3"
            numberInputProps={{
              className:
                "text-foreground placeholder:text-muted-foreground h-full bg-transparent outline-none",
            }}
          />
        </div>
      </FormSection>

      <FormSection title="Informacje o studiach">
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
          <div className="space-y-2">
            <FieldLabel htmlFor="faculty" required>
              Wydział
            </FieldLabel>
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
            <FieldLabel htmlFor="fieldOfStudy" required>
              Kierunek studiów
            </FieldLabel>
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
            <FieldLabel htmlFor="studyType" required>
              Rodzaj studiów
            </FieldLabel>
            <Select
              id="studyType"
              required
              value={studyType}
              onChange={(event) => setStudyType(event.target.value as StudyType)}
            >
              <option value="" disabled>
                Wybierz stopień
              </option>
              {STUDY_TYPE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </Select>
          </div>

          <div className="space-y-2">
            <FieldLabel htmlFor="graduationYear" required>
              Rok ukończenia (lub planowany)
            </FieldLabel>
            <Input
              id="graduationYear"
              type="number"
              required
              min={GRADUATION_YEAR_MIN}
              max={graduationYearMax()}
              step={1}
              placeholder="YYYY"
              value={graduationYear}
              onChange={(event) => setGraduationYear(event.target.value)}
            />
          </div>
        </div>
      </FormSection>

      <FormSection
        title="Oczekiwania i zaangażowanie"
        description="Sekcja dobrowolna. Pomoże nam dostosować ofertę klubu do Twoich potrzeb."
      >
        <fieldset className="space-y-3">
          <legend className="mb-3">
            <FieldLabel>Jakimi obszarami jesteś najbardziej zainteresowany/a?</FieldLabel>
          </legend>
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            {INTEREST_AREAS.map((area, index) => {
              const checked = interests.includes(area);
              return (
                <Label
                  key={area}
                  htmlFor={`interest-${index}`}
                  className={cn(
                    "cursor-pointer gap-3 rounded-md border px-4 py-3 leading-5 font-bold transition-colors",
                    checked ? selectedClass : unselectedClass,
                  )}
                >
                  <Checkbox
                    id={`interest-${index}`}
                    checked={checked}
                    onCheckedChange={() => setInterests((prev) => toggle(prev, area))}
                  />
                  {area}
                </Label>
              );
            })}
          </div>
        </fieldset>

        <fieldset>
          <legend className="mb-3">
            <FieldLabel>Preferowana forma spotkań</FieldLabel>
          </legend>
          <div className="grid grid-cols-3 gap-2">
            {MEETING_PREFERENCE_OPTIONS.map((option) => {
              const pressed = meetingPreferences.includes(option.value);
              return (
                <Toggle
                  key={option.value}
                  variant="outline"
                  pressed={pressed}
                  onPressedChange={() =>
                    setMeetingPreferences((prev) => toggle(prev, option.value))
                  }
                  className={cn(
                    "h-auto min-h-10 px-2 py-2 text-[13px] leading-4 font-bold whitespace-normal",
                    pressed ? `${selectedClass} data-[state=on]:bg-primary/15` : unselectedClass,
                  )}
                >
                  {option.label}
                </Toggle>
              );
            })}
          </div>
        </fieldset>

        <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
          <CheckboxCard
            id="coCreationInterest"
            title="Chcę aktywnie współtworzyć klub"
            description="Jestem chętny/a do bycia prelegentem, mentorem lub organizatorem wydarzeń."
            checked={coCreationInterest}
            onCheckedChange={setCoCreationInterest}
          />
          <CheckboxCard
            id="newsletterSubscription"
            title="Subskrypcja newslettera"
            description="Chcę otrzymywać informacje o nadchodzących wydarzeniach i aktualnościach ze świata IT."
            checked={newsletterSubscription}
            onCheckedChange={setNewsletterSubscription}
          />
        </div>
      </FormSection>

      <Separator />

      <div className="space-y-5">
        <Label
          htmlFor="consent-terms"
          className="items-start gap-4 text-sm leading-6 font-semibold"
        >
          <Checkbox
            id="consent-terms"
            className="mt-1"
            checked={consents.includes(ConsentType.REGULATIONS_PRIVACY)}
            onCheckedChange={() =>
              setConsents((prev) => toggle(prev, ConsentType.REGULATIONS_PRIVACY))
            }
          />
          <span>
            Zapoznałem/am się z{" "}
            <ExternalLink href={AGH_PRIVACY_NOTICE_URL}>
              informacją o przetwarzaniu danych osobowych przez AGH
            </ExternalLink>{" "}
            dla celów działalności Klubu Absolwentów AGH.<span className="text-destructive">*</span>
          </span>
        </Label>
        <Label htmlFor="consent-gdpr" className="items-start gap-4 text-sm leading-6 font-semibold">
          <Checkbox
            id="consent-gdpr"
            className="mt-1"
            checked={consents.includes(ConsentType.GDPR_DATA_PROCESSING)}
            onCheckedChange={() =>
              setConsents((prev) => toggle(prev, ConsentType.GDPR_DATA_PROCESSING))
            }
          />
          <span>
            Wyrażam zgodę na przetwarzanie moich danych osobowych przez AGH dla celów działalności
            Klubu Absolwentów AGH.
            <span className="text-destructive">*</span>
          </span>
        </Label>
      </div>

      {formError ? (
        <p className="text-destructive text-center text-sm font-semibold">{formError}</p>
      ) : null}

      <Button
        type="submit"
        size="xl"
        className="w-full rounded-xl font-bold tracking-widest uppercase"
        disabled={isPending}
      >
        {isPending ? "Wysyłanie…" : "Wyślij wniosek"}
      </Button>
    </form>
  );
}
