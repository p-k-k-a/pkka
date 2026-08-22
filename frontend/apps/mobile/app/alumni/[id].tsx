import { AlumniProfileView } from "@/components/alumni/alumni-profile-view";
import { RequireAlumni, useIsAlumni } from "@/components/auth/require-alumni";
import { DetailHeader } from "@/components/ui/detail-header";
import { Text } from "@/components/ui/text";
import { ApiError, useGetAlumniProfile } from "@pkka/api";
import { router, useLocalSearchParams } from "expo-router";
import { ActivityIndicator, ScrollView, View } from "react-native";

export default function AlumnDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const isAlumni = useIsAlumni();
  // The guard below only redirects once rendered, so skip the request a
  // non-alumni would just get a 403 for.
  const { data, isPending, error } = useGetAlumniProfile(id ?? "", {
    query: { enabled: !!id && isAlumni },
  });
  const profile = data?.status === 200 ? data.data : undefined;
  // Every non-2xx throws out of the mutator, so a missing alumn arrives as an
  // ApiError rather than a ProblemDetail on `data`; anything else is a real failure.
  const notFound = error instanceof ApiError && error.status === 404;

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
              {notFound
                ? "Nie znaleziono profilu alumna."
                : "Nie udało się załadować profilu alumna."}
            </Text>
          </View>
        )}
      </View>
    </RequireAlumni>
  );
}
