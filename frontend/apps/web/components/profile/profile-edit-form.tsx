"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import {
  AlertTriangle,
  ArrowLeft,
  Camera,
  GraduationCap,
  IdCard,
  Link2,
  Sparkles,
  UserRound,
} from "lucide-react";
import { toast } from "sonner";
import {
  ApiError,
  getGetMyProfileQueryKey,
  useGetMyProfile,
  useListUserTags,
  useUpdateMyProfile,
  useUpdateMyTags,
  type ProfileResponse,
  type UpdateProfileRequest,
  type UserTagResponse,
} from "@pkka/api";
import { AvatarPicker } from "@/components/profile/avatar-picker";
import { DetailList, DetailRow } from "@/components/profile/detail-list";
import { ProfileSectionCard } from "@/components/profile/profile-section-card";
import { SwitchRow } from "@/components/profile/switch-row";
import { TagPicker } from "@/components/profile/tag-picker";
import { VisibilityField } from "@/components/profile/visibility-field";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/lib/auth-context";
import { AvatarError, readProfileAvatar, saveProfileAvatar } from "@/lib/profile-avatar";
import { isHttpsUrl } from "@/lib/utils";

const PROFILE_HREF = "/dashboard/profile";
const BIO_MAX_LENGTH = 2000;

type UrlField = "linkedinUrl" | "githubUrl";

function Field({
  label,
  htmlFor,
  error,
  hint,
  children,
}: {
  label: string;
  htmlFor: string;
  error?: string;
  hint?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={htmlFor} className="text-[11px] font-bold tracking-widest uppercase">
        {label}
      </Label>
      {children}
      {error ? (
        <p id={`${htmlFor}-error`} className="text-destructive text-[13px] font-medium">
          {error}
        </p>
      ) : (
        hint
      )}
    </div>
  );
}

type EditFormFieldsProps = {
  profile: ProfileResponse;
  availableTags: UserTagResponse[];
  tagsError: boolean;
};

function EditFormFields({ profile, availableTags, tagsError }: EditFormFieldsProps) {
  const router = useRouter();
  const queryClient = useQueryClient();

  // Not part of the API yet, so it is read from and written to local storage.
  const [initialAvatar] = useState(readProfileAvatar);
  const [avatar, setAvatar] = useState(initialAvatar);
  const [currentPosition, setCurrentPosition] = useState(profile.currentPosition ?? "");
  const [company, setCompany] = useState(profile.company ?? "");
  const [bio, setBio] = useState(profile.bio ?? "");
  const [linkedinUrl, setLinkedinUrl] = useState(profile.linkedinUrl ?? "");
  const [githubUrl, setGithubUrl] = useState(profile.githubUrl ?? "");
  const [selectedTagIds, setSelectedTagIds] = useState(profile.tags.map((tag) => tag.id));
  const [willingToMentor, setWillingToMentor] = useState(profile.willingToMentor);
  const [showName, setShowName] = useState(profile.visibility.name);
  const [showEmail, setShowEmail] = useState(profile.visibility.email);
  const [showDiscord, setShowDiscord] = useState(profile.visibility.discord);
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<UrlField, string>>>({});
  const [formError, setFormError] = useState<string | null>(null);

  const updateProfile = useUpdateMyProfile<ApiError>();
  const updateTags = useUpdateMyTags<ApiError>();

  const fullName = [profile.firstName, profile.lastName].filter(Boolean).join(" ").trim();
  const isSaving = updateProfile.isPending || updateTags.isPending;
  const alumnSinceYear = profile.alumnSince ? profile.alumnSince.slice(0, 4) : null;
  const hasEducation = Boolean(profile.fieldOfStudy || profile.graduationYear || alumnSinceYear);

  const assignedTagIds = profile.tags.map((tag) => tag.id);
  const tagsChanged =
    selectedTagIds.length !== assignedTagIds.length ||
    selectedTagIds.some((id) => !assignedTagIds.includes(id));
  const isDirty =
    tagsChanged ||
    avatar !== initialAvatar ||
    currentPosition.trim() !== (profile.currentPosition ?? "") ||
    company.trim() !== (profile.company ?? "") ||
    bio.trim() !== (profile.bio ?? "") ||
    linkedinUrl.trim() !== (profile.linkedinUrl ?? "") ||
    githubUrl.trim() !== (profile.githubUrl ?? "") ||
    willingToMentor !== profile.willingToMentor ||
    showName !== profile.visibility.name ||
    showEmail !== profile.visibility.email ||
    showDiscord !== profile.visibility.discord;

  function setUrl(field: UrlField, value: string) {
    if (field === "linkedinUrl") setLinkedinUrl(value);
    else setGithubUrl(value);
    setFieldErrors((errors) => ({ ...errors, [field]: undefined }));
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);

    const linkedin = linkedinUrl.trim();
    const github = githubUrl.trim();
    const errors: Partial<Record<UrlField, string>> = {};
    if (linkedin && !isHttpsUrl(linkedin)) {
      errors.linkedinUrl = "Podaj pełny adres HTTPS, np. https://www.linkedin.com/in/…";
    }
    if (github && !isHttpsUrl(github)) {
      errors.githubUrl = "Podaj pełny adres HTTPS, np. https://github.com/…";
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    // Saved first so a rejected picture stops the submit before anything else
    // is written, the way a separate upload call would.
    if (avatar !== initialAvatar) {
      try {
        saveProfileAvatar(avatar);
      } catch (error) {
        setFormError(
          error instanceof AvatarError
            ? error.message
            : "Nie udało się zapisać zdjęcia profilowego. Spróbuj ponownie.",
        );
        return;
      }
    }

    const payload: UpdateProfileRequest = {
      bio: bio.trim(),
      currentPosition: currentPosition.trim(),
      company: company.trim(),
      linkedinUrl: linkedin,
      githubUrl: github,
      willingToMentor,
      visibility: { name: showName, email: showEmail, discord: showDiscord },
    };

    try {
      await updateProfile.mutateAsync({ data: payload });
    } catch {
      if (avatar !== initialAvatar) {
        try {
          saveProfileAvatar(initialAvatar);
        } catch {
          // Best-effort rollback - the profile itself did not save.
        }
      }
      setFormError("Nie udało się zapisać zmian. Spróbuj ponownie.");
      return;
    }

    if (tagsChanged) {
      try {
        await updateTags.mutateAsync({ data: { tagIds: selectedTagIds } });
      } catch (error) {
        // The profile itself is already saved, so the cache has to be refreshed
        // even though the tags did not go through.
        await queryClient.invalidateQueries({ queryKey: getGetMyProfileQueryKey() });
        setFormError(
          error instanceof ApiError && error.status === 400
            ? "Profil zapisany, ale nie udało się zapisać umiejętności - sprawdź wybrane tagi."
            : "Profil zapisany, ale nie udało się zapisać umiejętności. Spróbuj ponownie.",
        );
        return;
      }
    }

    await queryClient.invalidateQueries({ queryKey: getGetMyProfileQueryKey() });
    toast.success("Zmiany zostały zapisane.");
    router.push(PROFILE_HREF);
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col">
      <div className="bg-muted border-border sticky top-0 z-10 border-b">
        <div className="mx-auto flex w-full max-w-[960px] flex-col gap-4 px-4 py-6 md:flex-row md:items-end md:justify-between md:px-10">
          <div className="flex flex-col gap-1">
            <Link
              href={PROFILE_HREF}
              className="text-muted-foreground hover:text-accent inline-flex items-center gap-1.5 text-[13px] font-semibold tracking-wider uppercase transition-colors"
            >
              <ArrowLeft className="size-3.5" aria-hidden="true" />
              Wróć do profilu
            </Link>
            <h1 className="font-heading text-foreground text-[28px] leading-tight font-semibold">
              Edytuj profil
            </h1>
          </div>

          <div className="flex items-center gap-3">
            <Button
              type="button"
              variant="ghost"
              size="xl"
              disabled={isSaving}
              onClick={() => router.push(PROFILE_HREF)}
            >
              Anuluj
            </Button>
            <Button type="submit" size="xl" className="font-bold" disabled={isSaving || !isDirty}>
              {isSaving ? "Zapisywanie…" : "Zapisz zmiany"}
            </Button>
          </div>
        </div>
      </div>

      <div className="bg-background">
        <div className="mx-auto flex w-full max-w-[960px] flex-col gap-6 px-4 py-10 md:px-10 md:py-12">
          {formError ? (
            <Alert variant="destructive">
              <AlertTriangle />
              <AlertTitle>Zapis nie powiódł się</AlertTitle>
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          ) : null}

          <ProfileSectionCard
            title="Zdjęcie profilowe"
            icon={Camera}
            description="Widoczne przy Twoim profilu i w katalogu alumnów."
          >
            <AvatarPicker
              value={avatar}
              fallback={fullName}
              onChange={setAvatar}
              disabled={isSaving}
            />
          </ProfileSectionCard>

          <ProfileSectionCard
            title="O Tobie"
            icon={UserRound}
            description="Te informacje widzą pozostali alumni w katalogu."
          >
            <div className="flex flex-col gap-4">
              <div className="grid gap-4 sm:grid-cols-2">
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
              </div>

              <Field
                label="O mnie"
                htmlFor="bio"
                hint={
                  <p className="text-muted-foreground text-right text-[13px]">
                    {bio.length}/{BIO_MAX_LENGTH}
                  </p>
                }
              >
                <Textarea
                  id="bio"
                  value={bio}
                  onChange={(event) => setBio(event.target.value)}
                  placeholder="Napisz kilka słów o sobie - czym się zajmujesz, w czym możesz pomóc innym alumnom…"
                  maxLength={BIO_MAX_LENGTH}
                  className="min-h-32 resize-y"
                />
              </Field>

              <SwitchRow
                id="willingToMentor"
                label="Jestem otwarty na mentoring"
                description="Inni alumni zobaczą, że chętnie pomagasz, i będą mogli filtrować katalog po mentorach."
                checked={willingToMentor}
                onCheckedChange={setWillingToMentor}
                disabled={isSaving}
              />
            </div>
          </ProfileSectionCard>

          <ProfileSectionCard
            title="Dane z konta"
            icon={IdCard}
            description="Pochodzą z Twojego konta - możesz zdecydować, czy są widoczne, ale nie zmienić ich tutaj."
          >
            <div className="flex flex-col">
              <VisibilityField
                id="visibility-name"
                label="Imię i nazwisko"
                value={fullName}
                checked={showName}
                onCheckedChange={setShowName}
                disabled={isSaving}
              />
              <VisibilityField
                id="visibility-email"
                label="E-mail"
                value={profile.email}
                checked={showEmail}
                onCheckedChange={setShowEmail}
                disabled={isSaving}
              />
              <VisibilityField
                id="visibility-discord"
                label="Discord"
                value={profile.discordId}
                missingLabel="Brak połączonego konta Discord"
                checked={showDiscord}
                onCheckedChange={setShowDiscord}
                disabled={isSaving}
              />
            </div>
          </ProfileSectionCard>

          <ProfileSectionCard title="Linki" icon={Link2}>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="LinkedIn" htmlFor="linkedinUrl" error={fieldErrors.linkedinUrl}>
                <Input
                  id="linkedinUrl"
                  type="url"
                  inputMode="url"
                  value={linkedinUrl}
                  onChange={(event) => setUrl("linkedinUrl", event.target.value)}
                  placeholder="https://www.linkedin.com/in/…"
                  maxLength={500}
                  aria-invalid={Boolean(fieldErrors.linkedinUrl)}
                  aria-describedby={fieldErrors.linkedinUrl ? "linkedinUrl-error" : undefined}
                />
              </Field>
              <Field label="GitHub" htmlFor="githubUrl" error={fieldErrors.githubUrl}>
                <Input
                  id="githubUrl"
                  type="url"
                  inputMode="url"
                  value={githubUrl}
                  onChange={(event) => setUrl("githubUrl", event.target.value)}
                  placeholder="https://github.com/…"
                  maxLength={500}
                  aria-invalid={Boolean(fieldErrors.githubUrl)}
                  aria-describedby={fieldErrors.githubUrl ? "githubUrl-error" : undefined}
                />
              </Field>
            </div>
          </ProfileSectionCard>

          <ProfileSectionCard
            title="Umiejętności"
            icon={Sparkles}
            description="Wpisz frazę, aby wyszukać tagi. Maksymalnie 20 umiejętności."
          >
            {tagsError ? (
              <Alert variant="destructive">
                <AlertTriangle />
                <AlertTitle>Nie udało się wczytać listy tagów</AlertTitle>
                <AlertDescription>
                  Możesz zapisać pozostałe pola i wrócić do umiejętności później.
                </AlertDescription>
              </Alert>
            ) : (
              <TagPicker
                availableTags={availableTags}
                selectedIds={selectedTagIds}
                onChange={setSelectedTagIds}
                disabled={isSaving}
              />
            )}
          </ProfileSectionCard>

          {hasEducation ? (
            <ProfileSectionCard
              title="Wykształcenie"
              icon={GraduationCap}
              description="Dane z zatwierdzonego wniosku - tylko do odczytu."
            >
              <DetailList>
                <DetailRow label="Kierunek" value={profile.fieldOfStudy} />
                <DetailRow label="Rok ukończenia" value={profile.graduationYear} />
                <DetailRow label="Alumn od" value={alumnSinceYear} />
              </DetailList>
            </ProfileSectionCard>
          ) : null}
        </div>
      </div>
    </form>
  );
}

function EditFormSkeleton() {
  return (
    <div className="flex flex-col">
      <div className="bg-muted border-border border-b">
        <div className="mx-auto flex w-full max-w-[960px] items-end justify-between gap-4 px-4 py-6 md:px-10">
          <div className="flex flex-col gap-2">
            <Skeleton className="h-3 w-32" />
            <Skeleton className="h-8 w-48" />
          </div>
          <Skeleton className="h-[46px] w-36 rounded-lg" />
        </div>
      </div>
      <div className="mx-auto flex w-full max-w-[960px] flex-col gap-6 px-4 py-10 md:px-10 md:py-12">
        <Skeleton className="h-64 rounded-xl" />
        <Skeleton className="h-56 rounded-xl" />
        <Skeleton className="h-40 rounded-xl" />
      </div>
    </div>
  );
}

export function ProfileEditForm() {
  const { user } = useAuth();
  const profileQuery = useGetMyProfile();
  const tagsQuery = useListUserTags();

  if (!user) return null;

  if (profileQuery.isPending || tagsQuery.isPending) return <EditFormSkeleton />;

  const profile = profileQuery.data?.data;
  if (profileQuery.isError || !profile) {
    return (
      <div className="mx-auto w-full max-w-[720px] px-4 py-12 md:px-10">
        <Alert variant="destructive">
          <AlertTriangle />
          <AlertTitle>Nie udało się wczytać profilu do edycji</AlertTitle>
          <AlertDescription>Wróć do profilu i spróbuj ponownie.</AlertDescription>
        </Alert>
        <Button asChild variant="outline" className="mt-4">
          <Link href={PROFILE_HREF}>Wróć do profilu</Link>
        </Button>
      </div>
    );
  }

  // Tags already assigned stay selectable even if the tag catalogue call failed.
  const availableTags = [
    ...profile.tags,
    ...(tagsQuery.data?.data ?? []).filter(
      (tag) => !profile.tags.some((assigned) => assigned.id === tag.id),
    ),
  ];

  return (
    <EditFormFields profile={profile} availableTags={availableTags} tagsError={tagsQuery.isError} />
  );
}
