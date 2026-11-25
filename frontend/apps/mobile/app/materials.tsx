import { RequireSignedIn } from "@/components/auth/require-alumni";
import { MaterialsLibrary } from "@/components/materials/materials-library";
import { DetailHeader } from "@/components/ui/detail-header";
import { router, useLocalSearchParams } from "expo-router";
import { View } from "react-native";

export default function MaterialsScreen() {
  const { eventId, eventTitle } = useLocalSearchParams<{ eventId?: string; eventTitle?: string }>();

  return (
    <RequireSignedIn>
      <View className="bg-background flex-1">
        <DetailHeader title="Materiały" onBack={() => router.back()} />
        <MaterialsLibrary initialEvent={eventId ? { id: eventId, title: eventTitle } : undefined} />
      </View>
    </RequireSignedIn>
  );
}
