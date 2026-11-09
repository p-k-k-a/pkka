import { Card } from "@/components/ui/card";
import { Text } from "@/components/ui/text";
import { View } from "react-native";

type ProfileSectionCardProps = {
  title: string;
  children: React.ReactNode;
};

// Card used for bio as well as skills
export function ProfileSectionCard({ title, children }: ProfileSectionCardProps) {
  return (
    <Card className="gap-3 rounded-2xl p-6">
      <Text className="font-heading text-foreground text-lg font-semibold">{title}</Text>
      <View>{children}</View>
    </Card>
  );
}
