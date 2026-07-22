"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Camera, Eye, EyeOff, UserRound } from "lucide-react";
import {
  ApiError,
  getGetMyProfileQueryKey,
  useGetMyProfile,
  useListTags,
  useUpdateMyProfile,
  useUpdateMyTags,
  type MeResponse,
  type ProfileResponse,
  type TagResponse,
  type UpdateProfileRequest,
} from "@pkka/api";
import { TagPicker } from "@/components/profile/tag-picker";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/lib/auth-context";
import {
  isHttpsUrl,
  loadProfileMock,
  saveProfileMock,
  type ProfileMockFields,
} from "@/lib/profile-mock";
import { cn } from "@/lib/utils";

function optionalOrNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function IdentityRow({
  label,
  value,
  visible,
  onToggle,
}: {
  label: string;
  value?: string;
  visible: boolean;
  onToggle: () => void;
}) {
  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center justify-between gap-3">
        <Label className={cn("text-xs font-bold tracking-wider uppercase", !visible && "opacity-50")}>
          {label}
        </Label>
        <button
          type="button"
          role="switch"
          aria-checked={visible}
          aria-label={visible ? `Ukryj: ${label}` : `Pokaż: ${label}`}
          onClick={onToggle}
          className="border-border hover:bg-muted inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 transition-colors"
        >
          {visible ? (
            <Eye className="text-muted-foreground size-3.5" aria-hidden="true" />
          ) : (
            <EyeOff className="text-muted-foreground size-3.5" aria-hidden="true" />
          )}
          <span className="text-muted-foreground text-[10px] font-semibold tracking-wider uppercase">
            {visible ? "Widoczne" : "Ukryte"}
          </span>
        </button>
      </div>
      {value !== undefined ? (
        <div
          className={cn(
            "border-input bg-muted/40 text-muted-foreground flex h-11 items-center rounded-lg border px-3 text-sm",
            !visible && "opacity-50",
          )}
        >
          {value}
        </div>
      ) : null}
    </div>
  );
}

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
    <Card className="gap-5 p-6">
      <div className="space-y-1">
        <h2 className="font-heading text-foreground text-base font-semibold tracking-wide uppercase">
          {title}
        </h2>
        {description ? <p className="text-muted-foreground text-sm">{description}</p> : null}
      </div>
      <div className="flex flex-col gap-5">{children}</div>
    </Card>
  );
}

function Field({
  label,
  htmlFor,
  children,
}: {
  label: string;
  htmlFor: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={htmlFor} className="text-xs font-bold tracking-wider uppercase">
        {label}
      </Label>
      {children}
    </div>
  );
}

type EditFormFieldsProps = {
  user: MeResponse;
  profile: ProfileResponse;
  mock: ProfileMockFields;
  availableTags: TagResponse[];
  tagsError: boolean;
};

function EditFormFields({ user, profile, mock, availableTags, tagsError }: EditFormFieldsProps) {
  const router = useRouter();
  const queryClient = useQueryClient();

  const [currentPosition, setCurrentPosition] = useState(profile.currentPosition ?? "");
  const [company, setCompany] = useState(profile.company ?? "");
  const [bio, setBio] = useState(mock.bio ?? "");
  const [linkedinUrl, setLinkedinUrl] = useState(profile.linkedinUrl ?? "");
  const [githubUrl, setGithubUrl] = useState(profile.githubUrl ?? "");
  const [fieldOfStudy, setFieldOfStudy] = useState(mock.fieldOfStudy ?? "");
  const [graduationYear, setGraduationYear] = useState(mock.graduationYear?.toString() ?? "");
  const [alumnSince, setAlumnSince] = useState(mock.alumnSince?.toString() ?? "");
  const [discordId, setDiscordId] = useState(mock.discordId ?? "");
  const [selectedTagIds, setSelectedTagIds] = useState(profile.tags.map((tag) => tag.id));
  const [showName, setShowName] = useState(mock.visibility.name);
  const [showEmail, setShowEmail] = useState(mock.visibility.email);
  const [showDiscord, setShowDiscord] = useState(mock.visibility.discord);
  const [formError, setFormError] = useState<string | null>(null);

  const updateProfile = useUpdateMyProfile<ApiError>();
  const updateTags = useUpdateMyTags<ApiError>();

  const fullName = [user.firstName, user.lastName].filter(Boolean).join(" ").trim();
  const isSaving = updateProfile.isPending || updateTags.isPending;

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);

    const linkedin = linkedinUrl.trim();
    const github = githubUrl.trim();
    if (linkedin && !isHttpsUrl(linkedin)) {
      setFormError("LinkedIn musi być adresem HTTPS (https://…).");
      return;
    }
    if (github && !isHttpsUrl(github)) {
      setFormError("GitHub musi być adresem HTTPS (https://…).");
      return;
    }

    const year = graduationYear.trim();
    let parsedGraduationYear: number | undefined;
    if (year) {
      parsedGraduationYear = Number.parseInt(year, 10);
      const currentYear = new Date().getFullYear();
      if (
        !Number.isInteger(parsedGraduationYear) ||
        parsedGraduationYear < 1919 ||
        parsedGraduationYear > currentYear
      ) {
        setFormError(`Podaj prawidłowy rok ukończenia (1919–${currentYear}).`);
        return;
      }
    }

    const since = alumnSince.trim();
    let parsedAlumnSince: number | undefined;
    if (since) {
      parsedAlumnSince = Number.parseInt(since, 10);
      const currentYear = new Date().getFullYear();
      if (
        !Number.isInteger(parsedAlumnSince) ||
        parsedAlumnSince < 1919 ||
        parsedAlumnSince > currentYear
      ) {
        setFormError(`Podaj prawidłowy rok „Alumn od” (1919–${currentYear}).`);
        return;
      }
    }

    // Backend clears omitted/null fields — always send the full set.
    const payload = {
      currentPosition: optionalOrNull(currentPosition),
      company: optionalOrNull(company),
      linkedinUrl: optionalOrNull(linkedin),
      githubUrl: optionalOrNull(github),
    } as UpdateProfileRequest;

    const mockFields: ProfileMockFields = {
      bio: optionalOrNull(bio) ?? undefined,
      discordId: optionalOrNull(discordId) ?? undefined,
      fieldOfStudy: optionalOrNull(fieldOfStudy) ?? undefined,
      graduationYear: parsedGraduationYear,
      alumnSince: parsedAlumnSince,
      visibility: {
        name: showName,
        email: showEmail,
        discord: showDiscord,
      },
    };

    try {
      await updateProfile.mutateAsync({ data: payload });
      await updateTags.mutateAsync({ data: { tagIds: selectedTagIds } });
      saveProfileMock(user.sub, mockFields);
      await queryClient.invalidateQueries({ queryKey: getGetMyProfileQueryKey() });
      router.push("/dashboard/profile");
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        setFormError("Nie udało się zapisać tagów — sprawdź wybrane umiejętności.");
      } else {
        setFormError("Nie udało się zapisać zmian. Spróbuj ponownie.");
      }
    }
  }

  return (
    <form onSubmit={handleSubmit} className="mx-auto flex max-w-2xl flex-col gap-6">
      <div className="flex items-center gap-3">
        <Button asChild variant="ghost" size="icon-lg" aria-label="Wróć do profilu">
          <Link href="/dashboard/profile">
            <ArrowLeft />
          </Link>
        </Button>
        <h1 className="font-heading text-foreground text-lg font-semibold tracking-widest uppercase md:text-xl">
          Edytuj profil
        </h1>
      </div>

      <div className="flex flex-col items-center gap-3">
        <div className="border-border bg-muted flex size-36 items-center justify-center overflow-hidden rounded-full border">
          <UserRound
            className="text-muted-foreground size-16"
            strokeWidth={1.5}
            aria-hidden="true"
          />
        </div>
        {/* Mocked — photo upload is not wired to the backend yet. */}
        <Button type="button" variant="outline" size="sm" disabled>
          <Camera data-icon="inline-start" />
          Zmień zdjęcie
        </Button>
      </div>

      <FormSection
        title="Dane podstawowe"
        description="Pochodzą z Twojego konta — możesz je ukryć na profilu, ale nie edytować."
      >
        <IdentityRow
          label="Imię i nazwisko"
          value={fullName || undefined}
          visible={showName}
          onToggle={() => setShowName((value) => !value)}
        />
        <IdentityRow
          label="E-mail"
          value={user.email}
          visible={showEmail}
          onToggle={() => setShowEmail((value) => !value)}
        />
        <IdentityRow
          label="Discord"
          value={discordId.trim() || "— (mock, brak ID z konta)"}
          visible={showDiscord}
          onToggle={() => setShowDiscord((value) => !value)}
        />
        <Field label="Discord ID (mock)" htmlFor="discordId">
          <Input
            id="discordId"
            value={discordId}
            onChange={(event) => setDiscordId(event.target.value)}
            placeholder="np. 123456789012345678"
            inputMode="numeric"
            autoComplete="off"
          />
        </Field>
      </FormSection>

      <FormSection title="O Tobie" description="Puste pola nie pojawią się na Twoim profilu.">
        <Field label="Stanowisko" htmlFor="currentPosition">
          <Input
            id="currentPosition"
            value={currentPosition}
            onChange={(event) => setCurrentPosition(event.target.value)}
            placeholder="np. Senior Java Developer"
            maxLength={255}
          />
        </Field>
        <Field label="Firma" htmlFor="company">
          <Input
            id="company"
            value={company}
            onChange={(event) => setCompany(event.target.value)}
            placeholder="np. Google"
            maxLength={255}
          />
        </Field>
        <Field label="O mnie (mock)" htmlFor="bio">
          <Textarea
            id="bio"
            value={bio}
            onChange={(event) => setBio(event.target.value)}
            placeholder="Napisz kilka słów o sobie…"
            maxLength={2000}
            className="min-h-32"
          />
        </Field>
        <Field label="Kierunek (mock)" htmlFor="fieldOfStudy">
          <Input
            id="fieldOfStudy"
            value={fieldOfStudy}
            onChange={(event) => setFieldOfStudy(event.target.value)}
            placeholder="np. Informatyka"
          />
        </Field>
        <div className="grid gap-5 sm:grid-cols-2">
          <Field label="Rok ukończenia (mock)" htmlFor="graduationYear">
            <Input
              id="graduationYear"
              value={graduationYear}
              onChange={(event) => setGraduationYear(event.target.value)}
              placeholder="np. 2014"
              inputMode="numeric"
            />
          </Field>
          <Field label="Alumn od (mock)" htmlFor="alumnSince">
            <Input
              id="alumnSince"
              value={alumnSince}
              onChange={(event) => setAlumnSince(event.target.value)}
              placeholder="np. 2024"
              inputMode="numeric"
            />
          </Field>
        </div>
      </FormSection>

      <FormSection title="Linki" description="Puste pola nie pojawią się na Twoim profilu.">
        <Field label="LinkedIn" htmlFor="linkedinUrl">
          <Input
            id="linkedinUrl"
            type="url"
            value={linkedinUrl}
            onChange={(event) => setLinkedinUrl(event.target.value)}
            placeholder="https://www.linkedin.com/in/…"
            maxLength={500}
          />
        </Field>
        <Field label="GitHub" htmlFor="githubUrl">
          <Input
            id="githubUrl"
            type="url"
            value={githubUrl}
            onChange={(event) => setGithubUrl(event.target.value)}
            placeholder="https://github.com/…"
            maxLength={500}
          />
        </Field>
      </FormSection>

      <FormSection
        title="Umiejętności"
        description="Wpisz frazę, aby wyszukać tagi. Maksymalnie 20 umiejętności."
      >
        {tagsError ? (
          <p className="text-destructive text-sm">
            Nie udało się wczytać listy tagów. Możesz zapisać pozostałe pola i wrócić później.
          </p>
        ) : (
          <TagPicker
            availableTags={availableTags}
            selectedIds={selectedTagIds}
            onChange={setSelectedTagIds}
            disabled={isSaving}
          />
        )}
      </FormSection>

      {formError ? <p className="text-destructive text-sm font-medium">{formError}</p> : null}

      <Button
        type="submit"
        size="xl"
        className="w-full font-bold tracking-widest uppercase"
        disabled={isSaving}
      >
        {isSaving ? "Zapisywanie…" : "Zapisz zmiany"}
      </Button>
    </form>
  );
}

export function ProfileEditForm() {
  const { user } = useAuth();
  const profileQuery = useGetMyProfile();
  const tagsQuery = useListTags();

  if (!user) return null;

  if (profileQuery.isPending || tagsQuery.isPending) {
    return (
      <div className="mx-auto max-w-2xl space-y-6">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="mx-auto size-36 rounded-full" />
        <Skeleton className="h-64 w-full rounded-xl" />
        <Skeleton className="h-48 w-full rounded-xl" />
      </div>
    );
  }

  if (profileQuery.isError || !profileQuery.data?.data) {
    return (
      <div className="mx-auto max-w-2xl space-y-4">
        <p className="text-destructive font-medium">Nie udało się wczytać profilu do edycji.</p>
        <Button asChild variant="outline">
          <Link href="/dashboard/profile">Wróć do profilu</Link>
        </Button>
      </div>
    );
  }

  return (
    <EditFormFields
      user={user}
      profile={profileQuery.data.data}
      mock={loadProfileMock(user.sub)}
      availableTags={[
        ...profileQuery.data.data.tags,
        ...(tagsQuery.data?.data ?? []).filter(
          (tag) => !profileQuery.data.data.tags.some((assigned) => assigned.id === tag.id),
        ),
      ]}
      tagsError={tagsQuery.isError}
    />
  );
}
