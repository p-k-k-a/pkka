import { CheckboxCard } from "@/components/application/checkbox-card";
import { FormField } from "@/components/application/form-field";
import { FormSection } from "@/components/application/form-section";
import { TagPicker } from "@/components/profile/tag-picker";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Text } from "@/components/ui/text";
import {
  canonicalizeProfileUrl,
  githubUrlError,
  linkedinUrlError,
  phoneNumberError,
} from "@pkka/domain";
import { useThemeColors } from "@/lib/theme";
import { cn } from "@/lib/utils";
import {
  ApiError,
  getGetMyProfileQueryKey,
  getGetMyTagsQueryKey,
  getListAlumniTagsQueryKey,
  useGetMyProfile,
  useGetMyTags,
  useListAlumniTags,
  useUpdateMyProfile,
  useUpdateMyTags,
  type ProfileResponse,
} from "@pkka/api";
import { useForm, type AnyFieldApi } from "@tanstack/react-form";
import { useQueryClient } from "@tanstack/react-query";
import { router } from "expo-router";
import { Camera, Eye, EyeOff, UserRound } from "lucide-react-native";
import { useEffect, useRef, useState } from "react";
import { ActivityIndicator, Pressable, View } from "react-native";
import { KeyboardAwareScrollView } from "react-native-keyboard-controller";

const FIELD_LABELS: Record<string, string> = {
  bio: "O mnie",
  currentPosition: "Stanowisko",
  company: "Firma",
  linkedinUrl: "LinkedIn",
  githubUrl: "GitHub",
  phoneNumber: "Telefon",
};

// The client validates the same rules, so a 400 means the two drifted apart — name the
// offending fields instead of leaving the user to guess.
function validationMessage(error: unknown): string | null {
  if (!(error instanceof ApiError) || error.status !== 400) return null;
  const errors = (error.body as { errors?: Record<string, string> } | null)?.errors;
  const fields = Object.keys(errors ?? {})
    .map((field) => FIELD_LABELS[field] ?? field)
    .join(", ");
  return fields ? `Popraw pola: ${fields}.` : null;
}

function FieldError({ field }: { field: AnyFieldApi }) {
  if (!field.state.meta.isTouched || field.state.meta.errors.length === 0) return null;
  return (
    <Text className="text-destructive text-xs font-semibold">
      {field.state.meta.errors.join(". ")}
    </Text>
  );
}

// Read-only account-derived field (Keycloak identity / Discord federated link) with a
// show/hide toggle
function IdentityRow({
  label,
  value,
  field,
  children,
}: {
  label: string;
  value?: string;
  field: AnyFieldApi;
  children?: React.ReactNode;
}) {
  const theme = useThemeColors();
  const visible = field.state.value as boolean;
  return (
    <View className="gap-2">
      <View className="flex-row items-center justify-between">
        <Label
          className={cn(
            "text-foreground text-xs font-bold uppercase tracking-wider",
            !visible && "opacity-50",
          )}
        >
          {label}
        </Label>
        <Pressable
          onPress={() => field.handleChange(!visible)}
          role="switch"
          accessibilityLabel={visible ? `Ukryj: ${label}` : `Pokaż: ${label}`}
          className="border-border active:bg-muted flex-row items-center gap-1.5 rounded-full border px-3 py-1.5"
        >
          {visible ? (
            <Eye size={14} color={theme.mutedForeground} />
          ) : (
            <EyeOff size={14} color={theme.mutedForeground} />
          )}
          <Text className="text-muted-foreground text-[10px] font-semibold uppercase tracking-wider">
            {visible ? "Widoczne" : "Ukryte"}
          </Text>
        </Pressable>
      </View>
      {value ? (
        <View
          className={cn(
            "border-muted-foreground/25 dark:border-input bg-muted/40 h-12 justify-center rounded-md border px-3",
            !visible && "opacity-50",
          )}
        >
          <Text numberOfLines={1} className="text-muted-foreground">
            {value}
          </Text>
        </View>
      ) : null}
      {children}
    </View>
  );
}

function ProfileForm({ profile }: { profile: ProfileResponse }) {
  const theme = useThemeColors();
  const queryClient = useQueryClient();

  const availableTagsQuery = useListAlumniTags();
  const myTagsQuery = useGetMyTags();
  const updateMyTags = useUpdateMyTags();
  const updateMyProfile = useUpdateMyProfile();

  const availableTags = availableTagsQuery.data?.data ?? [];
  const tagsLoading = availableTagsQuery.isPending || myTagsQuery.isPending;
  const tagsError = availableTagsQuery.isError || myTagsQuery.isError;

  const [submitError, setSubmitError] = useState<string | null>(null);
  // Read at submit time via a ref so the (possibly memoized) onSubmit closure
  // never saves an empty set before the current tags have loaded.
  const seededTagsRef = useRef(false);

  const form = useForm({
    defaultValues: {
      currentPosition: profile.currentPosition ?? "",
      company: profile.company ?? "",
      bio: profile.bio ?? "",
      linkedinUrl: profile.linkedinUrl ?? "",
      githubUrl: profile.githubUrl ?? "",
      phoneNumber: profile.phoneNumber ?? "",
      willingToMentor: profile.willingToMentor,
      showName: profile.visibility.name,
      showEmail: profile.visibility.email,
      showDiscord: profile.visibility.discord,
      showPhone: profile.visibility.phone,
      tagNames: [] as string[],
    },
    onSubmit: async ({ value }) => {
      setSubmitError(null);
      if (seededTagsRef.current) {
        try {
          await updateMyTags.mutateAsync({ data: { names: value.tagNames } });
          queryClient.invalidateQueries({ queryKey: getGetMyTagsQueryKey() });
          queryClient.invalidateQueries({ queryKey: getListAlumniTagsQueryKey() });
        } catch (saveException) {
          setSubmitError(
            saveException instanceof ApiError && saveException.status === 400
              ? "Któraś z umiejętności ma niedozwoloną nazwę. Popraw ją i spróbuj ponownie."
              : "Nie udało się zapisać umiejętności. Spróbuj ponownie.",
          );
          return;
        }
      }

      const trimmed = (v: string) => v.trim();
      try {
        await updateMyProfile.mutateAsync({
          data: {
            currentPosition: trimmed(value.currentPosition),
            company: trimmed(value.company),
            bio: trimmed(value.bio),
            linkedinUrl: canonicalizeProfileUrl(value.linkedinUrl),
            githubUrl: canonicalizeProfileUrl(value.githubUrl),
            phoneNumber: trimmed(value.phoneNumber),
            willingToMentor: value.willingToMentor,
            visibility: {
              name: value.showName,
              email: value.showEmail,
              discord: value.showDiscord,
              phone: value.showPhone,
            },
          },
        });
        queryClient.invalidateQueries({ queryKey: getGetMyProfileQueryKey() });
      } catch (saveException) {
        setSubmitError(
          validationMessage(saveException) ?? "Nie udało się zapisać profilu. Spróbuj ponownie.",
        );
        return;
      }

      router.back();
    },
  });

  // Seed the picker with the user's saved tags once they load
  useEffect(() => {
    if (seededTagsRef.current || !myTagsQuery.isSuccess) return;
    form.setFieldValue("tagNames", myTagsQuery.data?.data?.map((tag) => tag.name) ?? []);
    seededTagsRef.current = true;
  }, [myTagsQuery.isSuccess, myTagsQuery.data, form]);

  return (
    <KeyboardAwareScrollView
      showsVerticalScrollIndicator={false}
      style={{ flex: 1, backgroundColor: theme.background }}
      contentContainerStyle={{ paddingHorizontal: 20, paddingVertical: 32, gap: 24 }}
      keyboardShouldPersistTaps="handled"
      bottomOffset={24}
    >
      <View className="items-center gap-3">
        <View className="border-border bg-muted size-36 items-center justify-center overflow-hidden rounded-full border">
          <UserRound size={64} color={theme.mutedForeground} strokeWidth={1.5} />
        </View>
        {/* Mocked — photo upload isn't implemented; the button intentionally does nothing. */}
        <Button variant="outline" size="sm" onPress={() => {}} className="active:bg-muted">
          <Camera size={16} color={theme.foreground} />
          <Text className="group-active:text-foreground">Zmień zdjęcie</Text>
        </Button>
      </View>

      <FormSection title="Dane podstawowe">
        <form.Field name="showName">
          {(field) => (
            <IdentityRow
              label="Imię i nazwisko"
              value={[profile.firstName, profile.lastName].filter(Boolean).join(" ")}
              field={field}
            />
          )}
        </form.Field>
        <form.Field name="showEmail">
          {(field) => <IdentityRow label="E-mail" value={profile.email} field={field} />}
        </form.Field>
        {profile.discordId ? (
          <form.Field name="showDiscord">
            {(field) => <IdentityRow label="Discord" field={field} />}
          </form.Field>
        ) : null}
        <form.Field name="showPhone">
          {(visibility) => (
            <IdentityRow label="Telefon" field={visibility}>
              <form.Field
                name="phoneNumber"
                validators={{ onBlur: ({ value }) => phoneNumberError(value) }}
              >
                {(field) => (
                  <>
                    <Input
                      value={field.state.value}
                      onChangeText={field.handleChange}
                      onBlur={field.handleBlur}
                      placeholder="+48 600 700 800"
                      keyboardType="phone-pad"
                      autoComplete="tel"
                      maxLength={32}
                    />
                    <FieldError field={field} />
                  </>
                )}
              </form.Field>
            </IdentityRow>
          )}
        </form.Field>
        <form.Field name="willingToMentor">
          {(field) => (
            <CheckboxCard
              title="Jestem otwarty na mentoring"
              description="Inni alumni będą to widzieć."
              checked={field.state.value}
              onCheckedChange={field.handleChange}
            />
          )}
        </form.Field>
      </FormSection>

      <FormSection title="O Tobie" description="Puste pola nie pojawią się na Twoim profilu.">
        <form.Field name="currentPosition">
          {(field) => (
            <FormField label="Stanowisko">
              <Input
                value={field.state.value}
                onChangeText={field.handleChange}
                onBlur={field.handleBlur}
                placeholder="np. Java Developer"
                autoCapitalize="sentences"
              />
            </FormField>
          )}
        </form.Field>

        <form.Field name="company">
          {(field) => (
            <FormField label="Firma">
              <Input
                value={field.state.value}
                onChangeText={field.handleChange}
                onBlur={field.handleBlur}
                placeholder="np. Google"
                autoCapitalize="sentences"
              />
            </FormField>
          )}
        </form.Field>

        <form.Field name="bio">
          {(field) => (
            <FormField label="O mnie">
              <Input
                value={field.state.value}
                onChangeText={field.handleChange}
                onBlur={field.handleBlur}
                placeholder="Napisz kilka słów o sobie..."
                multiline
                numberOfLines={5}
                maxLength={2000}
                className="h-32 py-3"
                style={{ textAlignVertical: "top" }}
              />
            </FormField>
          )}
        </form.Field>
      </FormSection>

      <FormSection
        title="Umiejętności"
        description="Wybierz technologie i obszary, w których się specjalizujesz. Jeśli czegoś brakuje na liście, dodaj własną."
      >
        <form.Field name="tagNames">
          {(field) => (
            <TagPicker
              catalog={availableTags}
              value={field.state.value}
              onChange={field.handleChange}
              loading={tagsLoading}
              error={tagsError}
            />
          )}
        </form.Field>
      </FormSection>

      <FormSection title="Linki" description="Puste pola nie pojawią się na Twoim profilu.">
        <form.Field
          name="linkedinUrl"
          validators={{ onBlur: ({ value }) => linkedinUrlError(value) }}
        >
          {(field) => (
            <FormField label="LinkedIn">
              <Input
                value={field.state.value}
                onChangeText={field.handleChange}
                onBlur={field.handleBlur}
                placeholder="linkedin.com/in/..."
                autoCapitalize="none"
                keyboardType="url"
              />
              <FieldError field={field} />
            </FormField>
          )}
        </form.Field>

        <form.Field name="githubUrl" validators={{ onBlur: ({ value }) => githubUrlError(value) }}>
          {(field) => (
            <FormField label="GitHub">
              <Input
                value={field.state.value}
                onChangeText={field.handleChange}
                onBlur={field.handleBlur}
                placeholder="github.com/..."
                autoCapitalize="none"
                keyboardType="url"
              />
              <FieldError field={field} />
            </FormField>
          )}
        </form.Field>
      </FormSection>

      {submitError ? (
        <Text className="text-destructive text-center text-sm font-semibold">{submitError}</Text>
      ) : null}

      <form.Subscribe selector={(state) => [state.canSubmit, state.isSubmitting] as const}>
        {([canSubmit, isSubmitting]) => (
          <Button
            size="lg"
            className="mt-2 w-full"
            disabled={!canSubmit || isSubmitting}
            onPress={() => form.handleSubmit()}
          >
            <Text className="text-sm font-bold uppercase tracking-widest">
              {isSubmitting ? "Zapisywanie..." : "Zapisz zmiany"}
            </Text>
          </Button>
        )}
      </form.Subscribe>
    </KeyboardAwareScrollView>
  );
}

// The form seeds default values from the profile, so it can only mount once loaded.
export function ProfileEditForm() {
  const { data, isPending, isError } = useGetMyProfile();
  const profile = data?.data;

  if (isPending) {
    return (
      <View className="flex-1 items-center justify-center bg-background">
        <ActivityIndicator />
      </View>
    );
  }

  if (isError || !profile) {
    return (
      <View className="flex-1 items-center justify-center bg-background px-5">
        <Text className="text-muted-foreground text-center text-sm leading-6">
          Nie udało się wczytać profilu. Wróć i spróbuj ponownie.
        </Text>
      </View>
    );
  }

  return <ProfileForm profile={profile} />;
}
