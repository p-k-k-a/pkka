import { ProfileEditForm } from "@/components/profile/profile-edit-form";
import { DetailHeader } from "@/components/ui/detail-header";
import { router } from "expo-router";
import { View } from "react-native";

export default function ProfileEditScreen() {
  return (
    <View className="flex-1 bg-background">
      <DetailHeader title="Edytuj profil" onBack={() => router.back()} />
      <ProfileEditForm />
    </View>
  );
}
