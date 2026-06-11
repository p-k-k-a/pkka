import { ApplicationForm } from "@/components/application/application-form";
import { DetailHeader } from "@/components/ui/detail-header";
import { router } from "expo-router";
import { View } from "react-native";

export default function ApplicationScreen() {
  return (
    <View className="flex-1 bg-background">
      <DetailHeader title="Klub Alumnów WI AGH" onBack={() => router.back()} />
      <ApplicationForm />
    </View>
  );
}
