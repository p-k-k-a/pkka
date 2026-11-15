import { RequireAlumni } from "@/components/auth/require-alumni";
import { TopicGroupRequestForm } from "@/components/topic-groups/request-form";
import { DetailHeader } from "@/components/ui/detail-header";
import { router } from "expo-router";
import { View } from "react-native";

export default function TopicGroupRequestScreen() {
  return (
    <RequireAlumni>
      <View className="bg-background flex-1">
        <DetailHeader title="Nowa grupa" onBack={() => router.back()} />
        <TopicGroupRequestForm />
      </View>
    </RequireAlumni>
  );
}
