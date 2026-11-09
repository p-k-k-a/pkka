import { FormField } from "@/components/application/form-field";
import { FormSection } from "@/components/application/form-section";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Text } from "@/components/ui/text";
import { useAppColorScheme, useThemeColors } from "@/lib/theme";
import {
  ApiError,
  getListMyTopicGroupRequestsQueryKey,
  useSubmitTopicGroupRequest,
} from "@pkka/api";
import {
  TOPIC_GROUP_DESCRIPTION_MAX_LENGTH,
  TOPIC_GROUP_NAME_MAX_LENGTH,
  topicGroupRequestSubmitErrorMessage,
} from "@pkka/domain";
import { useForm, type AnyFieldApi } from "@tanstack/react-form";
import { useQueryClient } from "@tanstack/react-query";
import { router } from "expo-router";
import { useState } from "react";
import { View } from "react-native";
import { KeyboardAwareScrollView } from "react-native-keyboard-controller";

function FieldError({ field }: { field: AnyFieldApi }) {
  if (!field.state.meta.isTouched || field.state.meta.errors.length === 0) return null;
  return (
    <Text className="text-destructive text-xs font-semibold">
      {field.state.meta.errors.join(". ")}
    </Text>
  );
}

export function TopicGroupRequestForm() {
  const theme = useThemeColors();
  const scheme = useAppColorScheme();
  const queryClient = useQueryClient();
  const { mutateAsync: submitRequest } = useSubmitTopicGroupRequest();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const form = useForm({
    defaultValues: { name: "", description: "" },
    onSubmit: async ({ value }) => {
      setSubmitError(null);
      try {
        await submitRequest({
          data: { name: value.name.trim(), description: value.description.trim() },
        });
      } catch (error) {
        const apiError = error instanceof ApiError ? error : null;
        setSubmitError(topicGroupRequestSubmitErrorMessage(apiError?.status, apiError?.body));
        if (apiError?.status === 409) {
          void queryClient.invalidateQueries({ queryKey: getListMyTopicGroupRequestsQueryKey() });
        }
        return;
      }
      await queryClient.invalidateQueries({ queryKey: getListMyTopicGroupRequestsQueryKey() });
      router.dismissTo("/(tabs)/alumni");
    },
  });

  return (
    <KeyboardAwareScrollView
      showsVerticalScrollIndicator={false}
      style={{ flex: 1, backgroundColor: scheme === "dark" ? theme.background : theme.muted }}
      contentContainerStyle={{ paddingHorizontal: 20, paddingVertical: 32, gap: 24 }}
      keyboardShouldPersistTaps="handled"
      bottomOffset={24}
    >
      <Text className="text-muted-foreground text-xs font-bold uppercase tracking-[3px]">
        Wniosek o grupę
      </Text>

      <FormSection
        title="Zaproponuj grupę"
        description="Opis jest zarazem uzasadnieniem. Na jego podstawie administracja oceni propozycję."
      >
        <form.Field
          name="name"
          validators={{
            onChange: ({ value }) => (value.trim() ? undefined : "Podaj nazwę grupy"),
          }}
        >
          {(field) => (
            <FormField label="Nazwa grupy" required>
              <Input
                value={field.state.value}
                onChangeText={field.handleChange}
                onBlur={field.handleBlur}
                placeholder="np. Grupa AI"
                autoCapitalize="sentences"
                maxLength={TOPIC_GROUP_NAME_MAX_LENGTH}
              />
              <FieldError field={field} />
            </FormField>
          )}
        </form.Field>

        <form.Field
          name="description"
          validators={{
            onChange: ({ value }) => (value.trim() ? undefined : "Opisz, czym grupa się zajmie"),
          }}
        >
          {(field) => (
            <FormField label="Opis" required>
              <Input
                value={field.state.value}
                onChangeText={field.handleChange}
                onBlur={field.handleBlur}
                placeholder="Czym grupa będzie się zajmować i dla kogo jest?"
                multiline
                numberOfLines={6}
                maxLength={TOPIC_GROUP_DESCRIPTION_MAX_LENGTH}
                className="h-40 py-3"
                style={{ textAlignVertical: "top" }}
              />
              <View className="flex-row items-start justify-between gap-3">
                <View className="flex-1">
                  <FieldError field={field} />
                </View>
                <Text className="text-muted-foreground text-xs font-medium">
                  {field.state.value.length} / {TOPIC_GROUP_DESCRIPTION_MAX_LENGTH}
                </Text>
              </View>
            </FormField>
          )}
        </form.Field>
      </FormSection>

      {submitError ? (
        <Text className="text-destructive text-center text-sm font-semibold">{submitError}</Text>
      ) : null}

      <form.Subscribe
        selector={(state) =>
          [
            state.values.name.trim().length > 0 && state.values.description.trim().length > 0,
            state.isSubmitting,
          ] as const
        }
      >
        {([filled, isSubmitting]) => (
          <Button
            size="lg"
            className="w-full"
            disabled={!filled || isSubmitting}
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
