"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, ArrowLeft, ArrowRight } from "lucide-react";
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
import { SectionHeading } from "@/components/profile/section-heading";
import { TagPicker } from "@/components/profile/tag-picker";
import { VisibilityField } from "@/components/profile/visibility-field";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/lib/auth-context";
import { canonicalizeProfileUrl, githubUrlError, linkedinUrlError } from "@pkka/domain";

const PROFILE_HREF = "/dashboard/profile";
const BIO_MAX_LENGTH = 2000;
const MAX_TAGS = 20;

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
    <div className="flex flex-col gap-2">
      <Label
        htmlFor={htmlFor}
        className="text-accent text-xs font-semibold tracking-widest uppercase"
      >
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

  // Compared and submitted in canonical form so an untouched field is never
  // reported dirty just because the stored value carries a trailing slash.
  const linkedin = canonicalizeProfileUrl(linkedinUrl);
  const github = canonicalizeProfileUrl(githubUrl);

  const assignedTagIds = profile.tags.map((tag) => tag.id);
  const tagsChanged =
    selectedTagIds.length !== assignedTagIds.length ||
    selectedTagIds.some((id) => !assignedTagIds.includes(id));
  const isDirty =
    tagsChanged ||
    currentPosition.trim() !== (profile.currentPosition ?? "") ||
    company.trim() !== (profile.company ?? "") ||
    bio.trim() !== (profile.bio ?? "") ||
    linkedin !== canonicalizeProfileUrl(profile.linkedinUrl ?? "") ||
    github !== canonicalizeProfileUrl(profile.githubUrl ?? "") ||
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

    const errors: Partial<Record<UrlField, string>> = {};
    const linkedinError = linkedinUrlError(linkedinUrl);
    const githubError = githubUrlError(githubUrl);
    if (linkedinError) errors.linkedinUrl = linkedinError;
    if (githubError) errors.githubUrl = githubError;
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

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
    <form onSubmit={handleSubmit} className="bg-background px-4 py-10 md:px-10 md:py-16">
      <div className="mx-auto max-w-[1280px]">
        <Button asChild variant="ghost" size="sm" className="mb-8 -ml-2">
          <Link href={PROFILE_HREF}>
            <ArrowLeft data-icon="inline-start" />
            Wróć do profilu
          </Link>
        </Button>

        <div className="mb-10 flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-3">
            <p className="bg-accent/10 text-accent inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
              Edycja profilu
            </p>
            <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
              Edytuj profil
            </h1>
          </div>
          <Button type="submit" size="xl" className="gap-2" disabled={isSaving || !isDirty}>
            {isSaving ? "Zapisywanie…" : "Zapisz zmiany"}
            {isSaving ? null : <ArrowRight data-icon="inline-end" />}
          </Button>
        </div>

        {formError ? (
          <Alert variant="destructive" className="mb-6">
            <AlertTriangle />
            <AlertTitle>Zapis nie powiódł się</AlertTitle>
            <AlertDescription>{formError}</AlertDescription>
          </Alert>
        ) : null}

        <div className="grid grid-cols-1 items-start gap-16 lg:grid-cols-[minmax(0,1fr)_320px]">
          <div className="space-y-12">
            <section className="space-y-5">
              <SectionHeading>Zdjęcie profilowe</SectionHeading>
              <AvatarPicker fallback={fullName} />
            </section>

            <section className="space-y-5">
              <SectionHeading>O Tobie</SectionHeading>
              <p className="text-muted-foreground text-sm">
                Te informacje widzą pozostali alumni w katalogu.
              </p>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
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
                  <p className="text-muted-foreground text-right text-xs">
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
            </section>

            <section className="space-y-5">
              <SectionHeading>Linki</SectionHeading>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
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
            </section>

            <section className="space-y-5">
              <SectionHeading>Umiejętności</SectionHeading>
              <p className="text-muted-foreground text-sm">
                Wpisz frazę, aby wyszukać tagi. Maksymalnie {MAX_TAGS} umiejętności.
              </p>
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
                  max={MAX_TAGS}
                  disabled={isSaving}
                />
              )}
            </section>
          </div>

          <div className="space-y-10">
            <Card className="gap-5 rounded-lg p-5 shadow-none">
              <SectionHeading>Mentoring</SectionHeading>
              <div className="flex items-start justify-between gap-4">
                <div className="flex flex-col gap-1">
                  <Label
                    htmlFor="willingToMentor"
                    className="text-foreground text-sm font-semibold"
                  >
                    Jestem otwarty na mentoring
                  </Label>
                  <p className="text-muted-foreground text-xs leading-relaxed">
                    Inni alumni zobaczą, że chętnie pomagasz, i będą mogli filtrować katalog po
                    mentorach.
                  </p>
                </div>
                <Switch
                  id="willingToMentor"
                  checked={willingToMentor}
                  onCheckedChange={setWillingToMentor}
                  disabled={isSaving}
                  className="mt-1"
                />
              </div>
            </Card>

            <Card className="gap-5 rounded-lg p-5 shadow-none">
              <SectionHeading>Widoczność</SectionHeading>
              <p className="text-muted-foreground text-xs leading-relaxed">
                Dane pochodzą z Twojego konta - możesz zdecydować, czy są widoczne, ale nie zmienić
                ich tutaj.
              </p>
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
                  missingLabel="Brak konta"
                  checked={showDiscord}
                  onCheckedChange={setShowDiscord}
                  disabled={isSaving}
                />
              </div>
            </Card>

            {hasEducation ? (
              <Card className="gap-5 rounded-lg p-5 shadow-none">
                <SectionHeading>Wykształcenie</SectionHeading>
                <p className="text-muted-foreground text-xs leading-relaxed">
                  Dane z zatwierdzonego wniosku - tylko do odczytu.
                </p>
                <DetailList>
                  <DetailRow label="Kierunek" value={profile.fieldOfStudy} />
                  <DetailRow label="Rok ukończenia" value={profile.graduationYear} />
                  <DetailRow label="Alumn od" value={alumnSinceYear} />
                </DetailList>
              </Card>
            ) : null}
          </div>
        </div>
      </div>
    </form>
  );
}

function EditFormSkeleton() {
  return (
    <div className="bg-background px-4 py-10 md:px-10 md:py-16">
      <div className="mx-auto max-w-[1280px]">
        <Skeleton className="mb-8 h-8 w-36" />
        <div className="mb-10 flex items-start justify-between gap-4">
          <div className="flex flex-col gap-3">
            <Skeleton className="h-6 w-32" />
            <Skeleton className="h-9 w-56" />
          </div>
          <Skeleton className="h-[46px] w-40 rounded-lg" />
        </div>
        <div className="grid grid-cols-1 gap-16 lg:grid-cols-[minmax(0,1fr)_320px]">
          <div className="space-y-12">
            <Skeleton className="h-32" />
            <Skeleton className="h-64" />
            <Skeleton className="h-28" />
          </div>
          <div className="space-y-10">
            <Skeleton className="h-36 rounded-lg" />
            <Skeleton className="h-56 rounded-lg" />
          </div>
        </div>
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
