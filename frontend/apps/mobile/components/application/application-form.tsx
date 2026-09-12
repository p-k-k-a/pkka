import { CheckboxCard } from "@/components/application/checkbox-card";
import { ConsentRow } from "@/components/application/consent-row";
import { FormField } from "@/components/application/form-field";
import { FormSection } from "@/components/application/form-section";
import { InterestRow } from "@/components/application/interest-row";
import { OptionChips } from "@/components/application/option-chips";
import { SelectField } from "@/components/application/select-field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import { Text } from "@/components/ui/text";
import { FACULTIES, MEETING_FORMATS, STUDY_TYPES } from "@/lib/application-constants";
import {
  graduationYearError,
  hasRequiredConsents,
  isGraduationYearValid,
  APPLICATION_CONFLICT_MESSAGE,
  APPLICATION_SUBMIT_ERROR_MESSAGE,
  INTEREST_AREAS,
  PRIVACY_URL,
  TERMS_URL,
} from "@pkka/domain";
import {
  ApiError,
  ConsentType,
  getGetMineQueryKey,
  useCreateApplication,
  type Faculty,
  type MeetingPreference,
  type StudyType,
} from "@pkka/api";
import { useTheme } from "@react-navigation/native";
import { useForm, type AnyFieldApi } from "@tanstack/react-form";
import { useQueryClient } from "@tanstack/react-query";
import { router } from "expo-router";
import * as WebBrowser from "expo-web-browser";
import * as React from "react";
import { useRef, useState } from "react";
import { View } from "react-native";
import { KeyboardAwareScrollView } from "react-native-keyboard-controller";
import PhoneInput from "react-native-phone-input";

function FieldError({ field }: { field: AnyFieldApi }) {
  if (!field.state.meta.isTouched || field.state.meta.errors.length === 0) return null;
  return (
    <Text className="text-destructive text-xs font-semibold">
      {field.state.meta.errors.join(". ")}
    </Text>
  );
}

function ApplicationForm() {
  const { colors } = useTheme();
  const queryClient = useQueryClient();
  const { mutateAsync: submitApplication } = useCreateApplication();
  const phoneInputRef = useRef<PhoneInput>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const form = useForm({
    defaultValues: {
      phoneNumber: "",
      faculty: null as Faculty | null,
      fieldOfStudy: "",
      studyType: null as StudyType | null,
      graduationYear: "",
      interests: [] as string[],
      meetingPreferences: [] as MeetingPreference[],
      coCreationInterest: false,
      newsletterSubscription: false,
      acceptedTerms: false,
      acceptedRodo: false,
    },
    onSubmit: async ({ value }) => {
      setSubmitError(null);

      const consents: ConsentType[] = [];
      if (value.acceptedTerms) consents.push(ConsentType.REGULATIONS_PRIVACY);
      if (value.acceptedRodo) consents.push(ConsentType.GDPR_DATA_PROCESSING);

      if (!value.faculty || !value.studyType || !hasRequiredConsents(consents)) {
        setSubmitError("Uzupełnij wymagane pola i zaakceptuj wymagane zgody.");
        return;
      }

      try {
        await submitApplication({
          data: {
            phoneNumber: value.phoneNumber.trim(),
            faculty: value.faculty,
            fieldOfStudy: value.fieldOfStudy.trim(),
            studyType: value.studyType,
            graduationYear: Number(value.graduationYear),
            interests: value.interests,
            meetingPreferences:
              value.meetingPreferences.length > 0 ? value.meetingPreferences : undefined,
            coCreationInterest: value.coCreationInterest,
            newsletterSubscription: value.newsletterSubscription,
            consents,
          },
        });
      } catch (error) {
        setSubmitError(
          error instanceof ApiError && error.status === 409
            ? APPLICATION_CONFLICT_MESSAGE
            : APPLICATION_SUBMIT_ERROR_MESSAGE,
        );
        return;
      }

      await queryClient.invalidateQueries({ queryKey: getGetMineQueryKey() });
      router.replace("/(tabs)/login");
    },
  });

  return (
    <KeyboardAwareScrollView
      style={{ flex: 1, backgroundColor: colors.background }}
      contentContainerStyle={{ paddingHorizontal: 20, paddingVertical: 32, gap: 24 }}
      keyboardShouldPersistTaps="handled"
      bottomOffset={24}
    >
      <View className="gap-3">
        <Text className="text-muted-foreground text-xs font-bold uppercase tracking-[3px]">
          Formularz zgłoszeniowy
        </Text>
      </View>

      <FormSection title="Dane kontaktowe">
        <form.Field
          name="phoneNumber"
          validators={{
            onBlur: ({ value }) => {
              if (!value.trim()) return "Podaj numer telefonu";
              if (!phoneInputRef.current?.isValidNumber()) return "Podaj poprawny numer telefonu";
              return undefined;
            },
          }}
        >
          {(field) => (
            <FormField label="Telefon kontaktowy" required>
              <PhoneInput
                ref={phoneInputRef}
                initialCountry="pl"
                autoFormat
                onChangePhoneNumber={(value) => field.handleChange(value)}
                textProps={{
                  onBlur: field.handleBlur,
                  placeholder: "000 000 000",
                  keyboardType: "phone-pad",
                  autoComplete: "tel",
                }}
                style={{
                  height: 48,
                  borderWidth: 1,
                  borderColor: colors.border,
                  borderRadius: 8,
                  backgroundColor: colors.background,
                  paddingHorizontal: 12,
                  flexDirection: "row",
                  alignItems: "center",
                }}
                textStyle={{
                  color: colors.text,
                  fontSize: 16,
                  flex: 1,
                }}
                flagStyle={{ width: 24, height: 16, borderWidth: 0 }}
                offset={8}
              />
              <FieldError field={field} />
            </FormField>
          )}
        </form.Field>
      </FormSection>

      <FormSection title="Informacje o studiach">
        <form.Field
          name="faculty"
          validators={{
            onChange: ({ value }) => (value ? undefined : "Wybierz wydział"),
          }}
        >
          {(field) => (
            <FormField label="Wydział" required>
              <SelectField
                value={field.state.value}
                options={FACULTIES}
                placeholder="Wybierz wydział"
                onChange={(value) => field.handleChange(value as Faculty)}
              />
              <FieldError field={field} />
            </FormField>
          )}
        </form.Field>

        <form.Field
          name="fieldOfStudy"
          validators={{
            onBlur: ({ value }) => (value.trim() ? undefined : "Podaj kierunek studiów"),
          }}
        >
          {(field) => (
            <FormField label="Kierunek studiów" required>
              <Input
                value={field.state.value}
                onChangeText={field.handleChange}
                onBlur={field.handleBlur}
                placeholder="np. Informatyka"
                autoCapitalize="sentences"
                maxLength={200}
              />
              <FieldError field={field} />
            </FormField>
          )}
        </form.Field>

        <form.Field
          name="studyType"
          validators={{
            onChange: ({ value }) => (value ? undefined : "Wybierz rodzaj studiów"),
          }}
        >
          {(field) => (
            <FormField label="Rodzaj studiów" required>
              <SelectField
                value={field.state.value}
                options={STUDY_TYPES}
                placeholder="Wybierz stopień"
                onChange={(value) => field.handleChange(value as StudyType)}
              />
              <FieldError field={field} />
            </FormField>
          )}
        </form.Field>

        <form.Field
          name="graduationYear"
          validators={{
            onBlur: ({ value }) => {
              if (!/^\d{4}$/.test(value)) return "Podaj rok w formacie YYYY";
              if (!isGraduationYearValid(Number(value))) return graduationYearError();
              return undefined;
            },
          }}
        >
          {(field) => (
            <FormField label="Rok ukończenia (lub planowany)" required>
              <Input
                value={field.state.value}
                onChangeText={(text) => field.handleChange(text.replace(/[^0-9]/g, ""))}
                onBlur={field.handleBlur}
                placeholder="YYYY"
                keyboardType="number-pad"
                maxLength={4}
              />
              <FieldError field={field} />
            </FormField>
          )}
        </form.Field>
      </FormSection>

      <FormSection
        title={"Oczekiwania i\nzaangażowanie"}
        description="Sekcja dobrowolna. Pomoże nam dostosować ofertę klubu do Twoich potrzeb."
      >
        <form.Field name="interests">
          {(field) => (
            <FormField label="Jakimi obszarami jesteś najbardziej zainteresowany/a?">
              <View className="mt-1 gap-4">
                {INTEREST_AREAS.map((area) => (
                  <InterestRow
                    key={area}
                    label={area}
                    checked={field.state.value.includes(area)}
                    onCheckedChange={(checked) =>
                      field.handleChange(
                        checked
                          ? [...field.state.value, area]
                          : field.state.value.filter((item) => item !== area),
                      )
                    }
                  />
                ))}
              </View>
            </FormField>
          )}
        </form.Field>

        <form.Field name="meetingPreferences">
          {(field) => (
            <FormField label="Preferowana forma spotkań">
              <OptionChips
                options={MEETING_FORMATS}
                value={field.state.value}
                onChange={(next) => field.handleChange(next as MeetingPreference[])}
              />
            </FormField>
          )}
        </form.Field>

        <form.Field name="coCreationInterest">
          {(field) => (
            <CheckboxCard
              title="Chcę aktywnie współtworzyć klub"
              description="Jestem chętny/a do bycia prelegentem, mentorem lub organizatorem wydarzeń."
              checked={field.state.value}
              onCheckedChange={field.handleChange}
            />
          )}
        </form.Field>

        <form.Field name="newsletterSubscription">
          {(field) => (
            <CheckboxCard
              title="Subskrypcja newslettera"
              description="Chcę otrzymywać informacje o nadchodzących wydarzeniach i aktualnościach ze świata IT."
              checked={field.state.value}
              onCheckedChange={field.handleChange}
            />
          )}
        </form.Field>
      </FormSection>

      <Separator />
      <View className="gap-5">
        <form.Field
          name="acceptedTerms"
          validators={{
            onChange: ({ value }) => (value ? undefined : "Ta zgoda jest wymagana"),
          }}
        >
          {(field) => (
            <View className="gap-2">
              <ConsentRow checked={field.state.value} onCheckedChange={field.handleChange}>
                Zapoznałem/am się z{" "}
                <Text
                  className="text-sm font-bold underline"
                  onPress={() => WebBrowser.openBrowserAsync(TERMS_URL)}
                >
                  Regulaminem Klubu Alumnów WI AGH
                </Text>{" "}
                oraz{" "}
                <Text
                  className="text-sm font-bold underline"
                  onPress={() => WebBrowser.openBrowserAsync(PRIVACY_URL)}
                >
                  Polityką Prywatności
                </Text>{" "}
                i akceptuję ich postanowienia.*
              </ConsentRow>
              <FieldError field={field} />
            </View>
          )}
        </form.Field>

        <form.Field
          name="acceptedRodo"
          validators={{
            onChange: ({ value }) => (value ? undefined : "Ta zgoda jest wymagana"),
          }}
        >
          {(field) => (
            <View className="gap-2">
              <ConsentRow checked={field.state.value} onCheckedChange={field.handleChange}>
                Wyrażam zgodę na przetwarzanie moich danych osobowych w celu realizacji procesu
                rejestracji i członkostwa w klubie, zgodnie z przepisami RODO.*
              </ConsentRow>
              <FieldError field={field} />
            </View>
          )}
        </form.Field>
      </View>

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
              {isSubmitting ? "Wysyłanie..." : "Wyślij wniosek"}
            </Text>
          </Button>
        )}
      </form.Subscribe>
    </KeyboardAwareScrollView>
  );
}

export { ApplicationForm };
