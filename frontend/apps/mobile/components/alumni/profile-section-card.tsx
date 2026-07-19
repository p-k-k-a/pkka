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
    <Card className="gap-3 p-6">
      <Text className="text-[10px] font-semibold uppercase tracking-widest text-muted-foreground">
        {title}
      </Text>
      <View>{children}</View>
    </Card>
  );
}
