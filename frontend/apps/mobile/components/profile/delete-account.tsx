import { BottomSheet } from "@/components/ui/bottom-sheet";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { useThemeColors } from "@/lib/theme";
import { ApiError, useDeleteMyAccount } from "@pkka/api";
import { router } from "expo-router";
import { Trash2 } from "lucide-react-native";
import { useState } from "react";
import { Keyboard, Pressable, View } from "react-native";

export function DeleteAccount() {
  const colors = useThemeColors();
  const { logout } = useAuth();
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState("");
  const deleteAccount = useDeleteMyAccount({
    mutation: {
      onSuccess: () => {
        router.back();
        void logout();
      },
    },
  });
  const busy = deleteAccount.isPending || deleteAccount.isSuccess;

  const openSheet = () => {
    setEmail("");
    deleteAccount.reset();
    setOpen(true);
  };

  const close = () => {
    Keyboard.dismiss();
    setOpen(false);
  };

  return (
    <>
      <Button size="lg" variant="outline" className="w-full" onPress={openSheet}>
        <Trash2 size={18} color={colors.destructive} />
        <Text className="font-bold">Usuń konto</Text>
      </Button>

      <BottomSheet visible={open} onClose={close}>
        <Text variant="h3" className="text-2xl font-bold">
          Usunąć konto?
        </Text>
        <Text className="text-muted-foreground mt-2 text-sm leading-6">
          Trwale usuniemy Twoje konto i wszystkie dane: profil, wnioski, zgłoszenia, zapisy na
          wydarzenia i odpowiedzi w ankietach. Tej operacji nie można cofnąć.
        </Text>

        <View className="mt-6 gap-2">
          <Text className="text-foreground text-sm font-semibold">
            Wpisz swój adres e-mail, aby potwierdzić
          </Text>
          <Input
            value={email}
            onChangeText={(text) => {
              setEmail(text);
              deleteAccount.reset();
            }}
            placeholder="adres@email.com"
            autoCapitalize="none"
            autoCorrect={false}
            keyboardType="email-address"
            accessibilityLabel="Adres e-mail"
            editable={!busy}
          />
          {deleteAccount.isError ? (
            <Text className="text-destructive text-sm font-medium">
              {deleteAccount.error instanceof ApiError && deleteAccount.error.status === 400
                ? "Ten adres e-mail nie pasuje do Twojego konta."
                : "Nie udało się usunąć konta. Spróbuj ponownie."}
            </Text>
          ) : null}
        </View>

        <View className="mt-6 gap-3">
          <Button
            size="lg"
            variant="destructive"
            disabled={!email.trim() || busy}
            onPress={() => deleteAccount.mutate({ data: { email: email.trim() } })}
          >
            <Text className="font-bold">{busy ? "Usuwanie…" : "Usuń konto na zawsze"}</Text>
          </Button>
          <Pressable onPress={close} className="items-center py-1">
            <Text className="text-muted-foreground text-sm font-semibold">Anuluj</Text>
          </Pressable>
        </View>
      </BottomSheet>
    </>
  );
}
