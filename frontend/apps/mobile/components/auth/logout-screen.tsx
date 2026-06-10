import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { View } from "react-native";

export function LogoutScreen() {
  const { logout } = useAuth();

  return (
    <View className="flex-1 items-center justify-center bg-muted px-4">
      <View className="w-full rounded-xl bg-card p-7">
        <Text className="mb-2 text-4xl font-extrabold tracking-tight text-foreground">
          Wyloguj się
        </Text>

        <Text variant="muted" className="leading-relaxed">
          Wyloguj się, aby zakończyć sesję w społeczności klubu alumnów wydziału informatyki AGH.
        </Text>

        <View className="h-8" />

        <Button size="lg" className="w-full" onPress={logout}>
          <Text>Wyloguj się</Text>
        </Button>
      </View>
    </View>
  );
}
