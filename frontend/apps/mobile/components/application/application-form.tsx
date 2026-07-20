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
import {
  FACULTIES,
  INTEREST_AREAS,
  MEETING_FORMATS,
  PRIVACY_URL,
  STUDY_TYPES,
  TERMS_URL,
} from "@/lib/application-constants";
import {
  ApiError,
  type CreateApplicationRequestDtoFaculty,
  type CreateApplicationRequestDtoMeetingPreferencesItem,
  type CreateApplicationRequestDtoStudyType,
  useCreate,
} from "@pkka/api";
import { useTheme } from "@react-navigation/native";
import { useForm, type AnyFieldApi } from "@tanstack/react-form";
import { router } from "expo-router";
import * as WebBrowser from "expo-web-browser";
import * as React from "react";
import { KeyboardAvoidingView, Platform, ScrollView, View } from "react-native";
import PhoneInput from "react-native-phone-input";

const CURRENT_YEAR = new Date().getFullYear();
const MAX_YEAR = CURRENT_YEAR + 7;

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
  const { mutateAsync: submitApplication } = useCreate();
  const phoneInputRef = React.useRef<PhoneInput>(null);
  const [submitError, setSubmitError] = React.useState<string | null>(null);

  const form = useForm({
    defaultValues: {
      phoneNumber: "",
      faculty: null as CreateApplicationRequestDtoFaculty | null,
      fieldOfStudy: "",
      studyType: null as CreateApplicationRequestDtoStudyType | null,
      graduationYear: "",
      interests: [] as string[],
      meetingPreferences: [] as CreateApplicationRequestDtoMeetingPreferencesItem[],
      coCreationInterest: false,
      newsletterSubscription: true,
      acceptedTerms: false,
      acceptedRodo: false,
    },
    onSubmit: async ({ value }) => {
      setSubmitError(null);
      try {
        await submitApplication({
          data: {
            phoneNumber: value.phoneNumber.trim(),
            faculty: value.faculty!,
            fieldOfStudy: value.fieldOfStudy.trim(),
            studyType: value.studyType!,
            graduationYear: Number(value.graduationYear),
            interests: value.interests,
            meetingPreferences:
              value.meetingPreferences.length > 0 ? value.meetingPreferences : undefined,
            coCreationInterest: value.coCreationInterest,
            newsletterSubscription: value.newsletterSubscription,
            consents: ["REGULATIONS_PRIVACY", "GDPR_DATA_PROCESSING"],
          },
        });
      } catch (error) {
        setSubmitError(
          error instanceof ApiError && error.status === 409
            ? "Masz już aktywny wniosek (w trakcie weryfikacji lub zaakceptowany). Nie możesz złożyć kolejnego."
            : "Nie udało się wysłać wniosku. Sprawdź dane i spróbuj ponownie.",
        );
        return;
      }
      router.replace("/(tabs)/login");
    },
  });

  return (
    <KeyboardAvoidingView
      className="flex-1"
      behavior={Platform.OS === "ios" ? "padding" : undefined}
    >
      <ScrollView
        className="bg-background flex-1"
        contentContainerClassName="px-5 py-8 gap-6"
        keyboardShouldPersistTaps="handled"
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
                  onChange={(value) =>
                    field.handleChange(value as CreateApplicationRequestDtoFaculty)
                  }
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
                  onChange={(value) =>
                    field.handleChange(value as CreateApplicationRequestDtoStudyType)
                  }
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
                const year = Number(value);
                if (year < 1919 || year > MAX_YEAR) return `Podaj rok między 1919 a ${MAX_YEAR}`;
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
                  onChange={(next) =>
                    field.handleChange(next as CreateApplicationRequestDtoMeetingPreferencesItem[])
                  }
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
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

export { ApplicationForm };
