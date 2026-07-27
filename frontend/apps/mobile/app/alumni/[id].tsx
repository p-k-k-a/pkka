import { AlumniProfileView } from "@/components/alumni/alumni-profile-view";
import { RequireAlumni } from "@/components/auth/require-alumni";
import { DetailHeader } from "@/components/ui/detail-header";
import { Text } from "@/components/ui/text";
import { getAlumnById } from "@/lib/alumni-directory";
import { router, useLocalSearchParams } from "expo-router";
import { ScrollView, View } from "react-native";

export default function AlumnDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const alumn = getAlumnById(id);

  // Directory profiles are verified-alumni only — the guard keeps a deep link
  // from leaking an alumn's contact details to a non-alumni.
  return (
    <RequireAlumni>
      <View className="bg-background flex-1">
        <DetailHeader title="Profil alumna" onBack={() => router.back()} />
        {alumn ? (
          <ScrollView contentContainerClassName="px-4 py-8">
            <AlumniProfileView profile={alumn} />
          </ScrollView>
        ) : (
          <View className="flex-1 items-center justify-center px-8">
            <Text className="text-muted-foreground text-center">
              Nie znaleziono profilu alumna.
            </Text>
          </View>
        )}
      </View>
    </RequireAlumni>
  );
}
