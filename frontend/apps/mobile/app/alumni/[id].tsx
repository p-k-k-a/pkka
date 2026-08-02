import { AlumniProfileView } from "@/components/alumni/alumni-profile-view";
import { RequireAlumni } from "@/components/auth/require-alumni";
import { DetailHeader } from "@/components/ui/detail-header";
import { Text } from "@/components/ui/text";
import { useGetAlumniProfile } from "@pkka/api";
import { router, useLocalSearchParams } from "expo-router";
import { ActivityIndicator, ScrollView, View } from "react-native";

export default function AlumnDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data, isPending, isError } = useGetAlumniProfile(id ?? "", {
    query: { enabled: !!id },
  });
  // 404 (not an alumnus / no approved application) comes back as a ProblemDetail
  // on the same field, so narrow on the status before handing it to the view.
  const profile = data?.status === 200 ? data.data : undefined;

  // Directory profiles are verified-alumni only — the guard keeps a deep link
  // from leaking an alumn's contact details to a non-alumni.
  return (
    <RequireAlumni>
      <View className="bg-background flex-1">
        <DetailHeader title="Profil alumna" onBack={() => router.back()} />
        {isPending ? (
          <View className="flex-1 items-center justify-center">
            <ActivityIndicator />
          </View>
        ) : profile ? (
          <ScrollView contentContainerClassName="px-4 py-8">
            <AlumniProfileView profile={profile} />
          </ScrollView>
        ) : (
          <View className="flex-1 items-center justify-center px-8">
            <Text className="text-muted-foreground text-center">
              {isError
                ? "Nie znaleziono profilu alumna."
                : "Nie udało się załadować profilu alumna."}
            </Text>
          </View>
        )}
      </View>
    </RequireAlumni>
  );
}
