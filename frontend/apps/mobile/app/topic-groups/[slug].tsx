import { RequireAlumni } from "@/components/auth/require-alumni";
import { TopicGroupDetails } from "@/components/topic-groups/group-details";
import { DetailHeader } from "@/components/ui/detail-header";
import { router, useLocalSearchParams } from "expo-router";
import { View } from "react-native";

export default function TopicGroupScreen() {
  const { slug } = useLocalSearchParams<{ slug: string }>();

  return (
    <RequireAlumni>
      <View className="bg-background flex-1">
        <DetailHeader title="Grupa" onBack={() => router.back()} />
        <TopicGroupDetails slug={slug} />
      </View>
    </RequireAlumni>
  );
}
